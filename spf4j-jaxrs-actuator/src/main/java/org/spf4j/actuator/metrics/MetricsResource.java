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
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import org.apache.avro.Schema;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.RecorderFactory;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.avro.MeasurementType;

/**
 * @author Zoltan Farkas
 */
@Path("metrics/local")
@RolesAllowed("operator")
@Singleton
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class MetricsResource {

  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream", "text/csv"})
  public Set<String> getMetrics() throws IOException {
    RecorderFactory.MEASUREMENT_STORE.flush();
    return RecorderFactory.MEASUREMENT_STORE.getMeasurements();
  }

  @GET
  @Path("{metric}/data")
  @ProjectionSupport
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream", "text/csv"})
  public AvroCloseableIterable<TimeSeriesRecord> getMetrics(@PathParam("metric") final String metricName,
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto) throws IOException {
    Instant from = pfrom == null ? Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()) : pfrom;
    Instant to = pto == null ? Instant.now() : pto;
    RecorderFactory.MEASUREMENT_STORE.flush();
    AvroCloseableIterable<TimeSeriesRecord> measurementData
            = RecorderFactory.MEASUREMENT_STORE.getMeasurementData(metricName, from, to);
    if (measurementData == null) {
       throw new NotFoundException("Metric not found " + metricName);
    }
    return measurementData;
  }


  @GET
  @Path("{metric}/data")
  @ProjectionSupport
  @Produces(value = {TextFormat.CONTENT_TYPE_004})
  public StreamingOutput getMetricsTextPrometheus(@PathParam("metric") final String metricName,
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto) throws IOException {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream out) throws IOException, WebApplicationException {
        try (AvroCloseableIterable<TimeSeriesRecord> metrics = getMetrics(metricName, pfrom, pto);
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



  @GET
  @Path("{metric}/schema")
  @Produces("application/json")
  public Schema getMetricSchema(@PathParam("metric") final String metricName) throws IOException {
    RecorderFactory.MEASUREMENT_STORE.flush();
    Schema measurementSchema = RecorderFactory.MEASUREMENT_STORE.getMeasurementSchema(metricName);
    if (measurementSchema == null) {
      throw new NotFoundException("Metric not found " + metricName);
    }
    return measurementSchema;
  }

}
