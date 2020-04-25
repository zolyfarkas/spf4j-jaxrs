/*
 * Copyright 2019 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.actuator.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.StreamingOutput;
import org.apache.avro.Schema;
import org.apache.avro.reflect.AvroSchema;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.RecorderFactory;

/**
 * @author Zoltan Farkas
 */
@Path("metrics")
@RolesAllowed("operator")
@Singleton
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class MetricsResource {

  private final Duration defaultFromDuration;

  @Inject
  public MetricsResource(@ConfigProperty(name = "metrics.fromDefaultDuration",
          defaultValue = "PT1M") final Duration defaultFromDuration) {
    this.defaultFromDuration = defaultFromDuration;
  }


  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream", "text/csv"})
  @Path("local")
  @AvroSchema(value = "{\"type\":\"array\",\"items\": {\"type\":\"string\", \"logicalType\":\"avsc\"}}")
  public Collection<Schema> getMetrics() throws IOException {
    RecorderFactory.MEASUREMENT_STORE.flush();
    return RecorderFactory.MEASUREMENT_STORE.query().getMeasurements((x) -> true);
  }

  /**
   * Prometheus metrics endpoint tailored for prometheus-to-sd.
   * Defaults are compatible with gcr.io/google-containers/prometheus-to-sd:v0.9.1
   * @param pfrom when null will default to: now - metrics.fromDefaultDuration
   * @param pto when null it will default to now.
   * @param pagg when null it will default to metrics.fromDefaultDuration,
   * for no aggregation zero duration should be used
   * @return prometheus metrics
   * @throws IOException
   */
  @GET
  @Produces(value = {TextFormat.CONTENT_TYPE_004})
  public StreamingOutput getMetricsTextPrometheus(
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto,
          @Nullable @QueryParam("aggDuration") final Duration pagg) throws IOException {
    Instant from = pfrom == null ? Instant.now().minus(defaultFromDuration) : pfrom;
    Instant to = pto == null ? Instant.now() : pto;
    Duration agg = pagg == null ? defaultFromDuration : pagg;
    long aggMillis = agg.toMillis();
    if (aggMillis > Integer.MAX_VALUE) {
      throw new ClientErrorException("Invalid aggregation durration: " + agg, 400);
    }
    RecorderFactory.MEASUREMENT_STORE.flush();
    MeasurementStoreQuery query = RecorderFactory.MEASUREMENT_STORE.query();
    return new PrometheusOutput(query, aggMillis, from, to);
  }

  @GET
  @Path("local/{metric}")
  @ProjectionSupport
  @Produces(value = {"application/avsc+json;qs=0.9"})
  public Schema getMetricSchema(@PathParam("metric") final String metricName)
          throws IOException {
    RecorderFactory.MEASUREMENT_STORE.flush();
    MeasurementStoreQuery query = RecorderFactory.MEASUREMENT_STORE.query();
    Collection<Schema> measurements = query.getMeasurements((x) -> x.equals(metricName));
    if (measurements.isEmpty()) {
      throw new NotFoundException("Metric nod found: " + metricName);
    } else if (measurements.size() > 1) {
      throw new IllegalStateException("Multiple metrics found, this is a bug " + measurements);
    }
    return measurements.iterator().next();
  }

  @GET
  @Path("local/{metric}")
  @ProjectionSupport
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream", "text/csv"})
  public AvroCloseableIterable<TimeSeriesRecord> getMetrics(@PathParam("metric") final String metricName,
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto,
          @Nullable @QueryParam("aggDuration") final Duration agg) throws IOException {
    Instant from = pfrom == null ? Instant.now().minus(defaultFromDuration) : pfrom;
    Instant to = pto == null ? Instant.now() : pto;
    long aggMillis = 0;
    if (agg != null) {
      aggMillis = agg.toMillis();
      if (aggMillis > Integer.MAX_VALUE) {
        throw new ClientErrorException("Durration to large " +  agg, 400);
      }
    }
    Schema measurement = getMetricSchema(metricName);
    MeasurementStoreQuery query = RecorderFactory.MEASUREMENT_STORE.query();
    return  aggMillis <= 0
            ? query.getMeasurementData(measurement, from, to)
            : query.getAggregatedMeasurementData(measurement, from, to, (int) aggMillis, TimeUnit.MILLISECONDS);
  }


  @GET
  @Path("local/{metric}")
  @Produces(value = {TextFormat.CONTENT_TYPE_004})
  public StreamingOutput getMetricsTextPrometheus(@PathParam("metric") final String metricName,
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto,
          @Nullable @QueryParam("aggDuration") final Duration agg) throws IOException {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream out) throws IOException {
        try (AvroCloseableIterable<TimeSeriesRecord> metrics = getMetrics(metricName, pfrom, pto, agg);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

          Iterator<TimeSeriesRecord> iterator = metrics.iterator();
          TextFormat.write004(bw, new Enumeration<Collector.MetricFamilySamples>() {
            @Override
            public boolean hasMoreElements() {
              return iterator.hasNext();
            }

            @Override
            public Collector.MetricFamilySamples nextElement() {
              TimeSeriesRecord next = iterator.next();
              return PrometheusUtils.convert(next.getSchema(), Collections.singletonList(next));
            }
          });
        }
      }
    };
  }

  @Override
  public String toString() {
    return "MetricsResource{" + "defaultFromDuration=" + defaultFromDuration + '}';
  }

  private static class PrometheusOutput implements StreamingOutput {

    private final MeasurementStoreQuery query;
    private final long aggMillis;
    private final Instant from;
    private final Instant to;

    PrometheusOutput(final MeasurementStoreQuery query, final long aggMillis,
            final Instant from, final Instant to) {
      this.query = query;
      this.aggMillis = aggMillis;
      this.from = from;
      this.to = to;
    }

    @Override
    public void write(final OutputStream out) throws IOException {
      try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
        Collection<Schema> metrics = query.getMeasurements((x) -> true);
        for (Schema metric : metrics) {
          try (AvroCloseableIterable<TimeSeriesRecord> values = aggMillis <= 0
                  ? query.getMeasurementData(metric, from, to)
                  : query.getAggregatedMeasurementData(metric, from, to, (int) aggMillis, TimeUnit.MILLISECONDS)) {
            Collector.MetricFamilySamples mfs = PrometheusUtils.convert(values);
            if (mfs != null) {
              TextFormat.write004(bw,
                      Collections.enumeration(Collections.singletonList(mfs)));
            }
          }
        }
        TextFormat.write004(bw, Collections.enumeration(Collections.singletonList(
                new Collector.MetricFamilySamples("process_start_time_seconds",
                        Collector.Type.COUNTER, "Seconds since unix EPOCH when the process started",
                        Collections.singletonList(new Collector.MetricFamilySamples.Sample("process_start_time_seconds",
                                Collections.emptyList(), Collections.emptyList(),
                                ((double) ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000))))));
      }
    }
  }

}
