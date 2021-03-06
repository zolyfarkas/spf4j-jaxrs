package org.spf4j.actuator.cluster.logs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.spf4j.jaxrs.server.AsyncResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.actuator.logs.LogbackResource;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.actuator.logs.LogsResource.LogAccumulator;
import org.spf4j.base.CloseableIterable;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.LogPrinter;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logback/cluster/status")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed(JaxRsSecurityContext.OPERATOR_ROLE)
@Singleton
public class LogbackClusterResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final LogbackResource localLogback;

  private final int port;

  private final String protocol;

  private final int maxLogRetrieveLimit;

  @Inject
  public LogbackClusterResource(final LogbackResource localLogback,
          final Cluster cluster, final Spf4JClient httpClient,
          @ConfigProperty(name = "servlet.port") final int port,
          @ConfigProperty(name = "servlet.protocol") final String protocol,
          @ConfigProperty(name = "resources.logback.maxLogRetrieveLimit",
                  defaultValue = "200") final int maxLogRetrieveLimit) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.localLogback = localLogback;
    this.port = port;
    this.protocol = protocol;
    this.maxLogRetrieveLimit = maxLogRetrieveLimit;
  }

  @GET
  @Produces(value = {"text/plain"})
  public void getClusterLogbackStatusText(@QueryParam("limit") @DefaultValue("100") final int limit,
          @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    if (limit > maxLogRetrieveLimit || limit < 0) {
      throw new ClientErrorException("Invalid limit " + limit, Status.REQUESTED_RANGE_NOT_SATISFIABLE);
    }
    getClusterLogbackStatus(limit, new AsyncResponseWrapper(ar) {
      @Override
      public boolean resume(final Object response) {
        return super.resume(new StreamingOutput() {
          @Override
          public void write(final OutputStream output) throws IOException, WebApplicationException {
            LogPrinter printer = new LogPrinter(StandardCharsets.UTF_8);
            for (LogRecord record : ((LogAccumulator) response).get()) {
              printer.print(record, output);
            }
          }
        });
      }

    });
  }

  @Operation(
          description = "Get cluster wide logback status",
          responses = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LogRecord.class))))
          }
  )
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @ProjectionSupport
  public void getClusterLogbackStatus(@QueryParam("limit") @DefaultValue("100") final int limit,
          @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    if (limit > maxLogRetrieveLimit || limit < 0) {
      throw new ClientErrorException("Invalid limit " + limit, Status.REQUESTED_RANGE_NOT_SATISFIABLE);
    }
    CompletableFuture<LogsResource.LogAccumulator> cf
            = ContextPropagatingCompletableFuture.supplyAsync(() -> {
             LogsResource.LogAccumulator result = new LogsResource.TopAccumulator(limit, LogRecord::getTs, Order.DESC);
              Collection<LogRecord> ll = localLogback.status(limit);
              result.acceptAll(ll);
              return result;
            }, DefaultExecutor.INSTANCE);
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(protocol, null,
              addr.getHostAddress(), port, "/logback/local/status", null, null);
      Spf4jWebTarget invTarget = httpClient.target(uri)
              .queryParam("limit", limit);
      cf = cf.thenCombine(
              invTarget.request("application/avro").rx().get(new GenericType<CloseableIterable<LogRecord>>() {
              }),
              (LogsResource.LogAccumulator result, CloseableIterable<LogRecord> rl) -> {
                try (CloseableIterable<LogRecord> r = rl) {
                  result.acceptAll(r);
                }
                return result;
              }
      );
    }
    cf.whenComplete((records, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(records);
      }
    });
  }

  @DELETE
  public void clear() throws URISyntaxException {
    localLogback.clear();
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(protocol, null,
              addr.getHostAddress(), port, "/logback/local/status", null, null);
      Spf4jWebTarget invTarget = httpClient.target(uri);
      invTarget.request().delete(Void.class);
    }
  }

}
