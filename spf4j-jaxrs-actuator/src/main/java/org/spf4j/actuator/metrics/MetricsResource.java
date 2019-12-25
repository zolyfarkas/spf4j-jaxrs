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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.avro.Schema;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.RecorderFactory;

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
    return RecorderFactory.MEASUREMENT_STORE.getMeasurementData(metricName, from, to);
  }


  @GET
  @Path("{metric}/schema")
  @Produces("application/json")
  public Schema getMetricSchema(@PathParam("metric") final String metricName) throws IOException {
    return RecorderFactory.MEASUREMENT_STORE.getMeasurementSchema(metricName);
  }

}
