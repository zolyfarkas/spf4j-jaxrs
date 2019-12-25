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
package org.spf4j.actuator.cluster.metrics;

import gnu.trove.set.hash.THashSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.spf4j.actuator.metrics.MetricsResource;
import org.spf4j.base.ArrayWriter;
import org.spf4j.base.avro.AvroCloseableIterable;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.jaxrs.StreamingArrayContent;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.perf.TimeSeriesRecord;

/**
 * @author Zoltan Farkas
 */
@Path("metrics/cluster")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Singleton
public class MetricsClusterResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final MetricsResource localResource;

  @Inject
  public MetricsClusterResource(
          final Cluster cluster, final Spf4JClient httpClient, final MetricsResource localResource) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.localResource = localResource;
  }

  @Operation(
          description = "Get cluster metrics",
          responses = {
            @ApiResponse(description = "a list of the cluster metrics",
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = String.class)
                            )
                    )
            )
          }
  )
  @Produces({"application/json", "application/avro", "text/csv"})
  @GET
  public void getClusterMetrics(@Suspended final AsyncResponse ar)
          throws URISyntaxException {
    CompletableFuture<Set<String>> cf
            = ContextPropagatingCompletableFuture.supplyAsync(() -> {
              try {
                return new THashSet<>(localResource.getMetrics());
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            }, DefaultExecutor.INSTANCE);
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    NetworkService service = clusterInfo.getHttpService();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(service.getName(), null,
              addr.getHostAddress(), service.getPort(), "/metrics/local", null, null);
      cf = cf.thenCombine(httpClient.target(uri).request("application/avro")
              .rx().get(new GenericType<List<String>>() {
              }),
              (Set<String> result, List<String> resp) -> {
                result.addAll(resp);
                return result;
              });
    }
    cf.whenComplete((labels, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(new GenericEntity<Collection<String>>(labels) {
        });
      }
    });
  }

  @GET
  @Path("{metric}/schema")
  @Produces("application/json")
  public org.apache.avro.Schema getMetricSchema(@PathParam("metric") final String metricName)
          throws IOException, URISyntaxException {
    try {
      return localResource.getMetricSchema(metricName);
    } catch (WebApplicationException ex) {
      if (ex.getResponse().getStatus() != 404) {
        throw ex;
      }
    }
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    NetworkService service = clusterInfo.getHttpService();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(service.getName(), null,
              addr.getHostAddress(), service.getPort(), "/metrics/local/" + metricName + "/schema", null, null);
      try {
        return httpClient.target(uri).request(MediaType.APPLICATION_JSON).get(org.apache.avro.Schema.class);
      } catch (WebApplicationException ex) {
        if (ex.getResponse().getStatus() != 404) {
          throw ex;
        }
      }
    }
    throw new NotFoundException("Metric not found " + metricName);
  }

  @Produces({"application/json", "application/avro", "text/csv"})
  @GET
  @Path("{metric}/data")
  @ProjectionSupport
  public StreamingArrayContent<GenericRecord> getClusterMetricsData(@PathParam("metric") final String metricName,
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto) throws URISyntaxException, IOException {
    org.apache.avro.Schema metricSchema = getMetricSchema(metricName);
    org.apache.avro.Schema nSchema = addNodeToSchema(metricSchema);
    Instant from = pfrom == null ? Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()) : pfrom;
    Instant to = pto == null ? Instant.now() : pto;
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    InetAddress localAddress = clusterInfo.getLocalAddress();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    NetworkService service = clusterInfo.getHttpService();
    return new StreamingArrayContent<GenericRecord>() {
      @Override
      public void write(final ArrayWriter<GenericRecord> output) throws IOException {
        try (AvroCloseableIterable<TimeSeriesRecord> metrics = localResource.getMetrics(metricName, from, to)) {
          for (TimeSeriesRecord rec : metrics) {
            output.write(addNodeToRecord(nSchema, rec, localAddress.getHostAddress()));
          }
        }
        for (InetAddress addr : peerAddresses) {
          URI uri;
          try {
            uri = new URI(service.getName(), null,
                    addr.getHostAddress(), service.getPort(), "/metrics/local/" + metricName + "/data", null, null);
          } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
          }
          try (AvroCloseableIterable<GenericRecord> metrics = httpClient.target(uri)
                  .queryParam("from", from)
                  .queryParam("to", to)
                  .request("application/avro").get(new GenericType<AvroCloseableIterable<GenericRecord>>() {
          })) {
            for (GenericRecord rec : metrics) {
              output.write(addNodeToRecord(nSchema, rec, addr.getHostAddress()));
            }
          } catch (WebApplicationException ex) {
            if (ex.getResponse().getStatus() != 404) {
              throw ex;
            }
          }
        }

      }

      @Override
      public org.apache.avro.Schema getElementSchema() {
        return nSchema;
      }

    };
  }

  private static org.apache.avro.Schema addNodeToSchema(final org.apache.avro.Schema schema) {
    List<org.apache.avro.Schema.Field> ofields = schema.getFields();
    List<org.apache.avro.Schema.Field> fields = new ArrayList<>(ofields.size() + 1);
    fields.add(new org.apache.avro.Schema.Field("node",
            org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING), "Node info"));
    for (org.apache.avro.Schema.Field f : ofields) {
      fields.add(new org.apache.avro.Schema.Field(f, f.schema()));
    }
    return org.apache.avro.Schema.createRecord("Nodes" + schema.getName(),
            schema.getDoc(), schema.getNamespace(), false, fields, false);
  }

  private static GenericRecord addNodeToRecord(final org.apache.avro.Schema nSchema,
          final GenericRecord record, final String node) {
    GenericRecord result = new GenericData.Record(nSchema);
    result.put(0, node);
    for (int i = 1, l = nSchema.getFields().size(); i < l; i++) {
      result.put(i, record.get(i - 1));
    }
    return result;
  }

}
