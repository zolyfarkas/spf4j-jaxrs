package org.spf4j.actuator.cluster.profiles;

import gnu.trove.set.hash.THashSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.actuator.cluster.logs.LogsClusterResource;
import org.spf4j.actuator.profiles.ProfilesResource;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.server.AsyncResponseWrapper;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;
import javax.ws.rs.core.GenericType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.actuator.profiles.FlameGraphParams;
import org.spf4j.jaxrs.client.Spf4jWebTarget;

/**
 *
 * @author Zoltan Farkas
 */
@Path("profiles")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
@Singleton
public class ProfilesClusterResource {

  private final LogsClusterResource logsResource;

  private final ProfilesResource profiles;

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final int port;

  private final String protocol;

  @Inject
  public ProfilesClusterResource(final LogsClusterResource logsResource, final ProfilesResource profiles,
           final Cluster cluster, final Spf4JClient httpClient,
          @ConfigProperty(name = "servlet.port") final int port,
          @ConfigProperty(name = "servlet.protocol") final String protocol)
          throws IOException {
    this.logsResource = logsResource;
    this.profiles = profiles;
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.port = port;
    this.protocol = protocol;
  }

  @Path("cluster/traces/{trId}")
  @GET
  @Produces(value = {"application/stack.samples+json", "application/stack.samples.d3+json"})
  public void getSamples(@PathParam("trId") final String traceId, @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    StringBuilder sb = new StringBuilder(traceId.length());
    AppendableUtils.escapeJsonString(traceId, sb);
    logsResource.getClusterLogs(10, "log.stackSamples.length != 0 and log.trId == \""
            + sb + "\"", Order.DESC, new AsyncResponseWrapper(ar) {
      @Override
      public boolean resume(final Object response) {
        List<LogRecord> logs = (List<LogRecord>) response;
        if (logs.isEmpty()) {
          return super.resume(null);
        }
        SampleNode result = Converter.convert(logs.get(0).getStackSamples().iterator());
        for (int i = 1, l = logs.size(); i < l; i++) {
          List<StackSampleElement> stackSamples = logs.get(i).getStackSamples();
          if (stackSamples.isEmpty()) {
            continue;
          }
          result = SampleNode.aggregate(result, Converter.convert(stackSamples.iterator()));
        }
        return super.resume(result);
      }

    });
  }

  @Operation(
          description = "Get cluster prrofile aggregation groups.",
          responses = {
            @ApiResponse(description = "a list of the aggregation groups",
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
  @Path("cluster/groups")
  @GET
  @Produces({"application/json", "application/avro"})
  public void getSampleLabels(@Suspended final AsyncResponse ar) throws IOException, URISyntaxException {
    CompletableFuture<Set<String>> cf
            = ContextPropagatingCompletableFuture.supplyAsync(() -> {
              try {
                return new THashSet<>(profiles.getSampleLabels());
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            }, DefaultExecutor.INSTANCE);
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(protocol, null,
                  addr.getHostAddress(), port, "/profiles/local/groups", null, null);
      cf = cf.thenCombine(httpClient.target(uri).request("application/avro")
              .rx().get(new GenericType<List<String>>() { }),
              (Set<String> result, List<String> resp) -> {
                result.addAll(resp);
                return result;
              });
    }
    cf.whenComplete((labels, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(new GenericEntity<Collection<String>>(labels) { });
      }
    });
  }

  @Path("cluster/groups/{label}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  public void getLabeledSamples(@PathParam("label") final String label,
          @Nullable @QueryParam("from") final Instant from,
          @Nullable @QueryParam("to") final Instant to,
          @Suspended final AsyncResponse ar) throws IOException, URISyntaxException {

    CompletableFuture<SampleNode> cf
            = ContextPropagatingCompletableFuture.supplyAsync(() -> {
              try {
                return profiles.getLabeledSamples(label, from, to);
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
            }, DefaultExecutor.INSTANCE);
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(protocol, null,
                  addr.getHostAddress(), port, "/profiles/local/groups", null, null);
      Spf4jWebTarget target = httpClient.target(uri).path(label);
      if (from != null) {
        target = target.queryParam("from", from.toString());
      }
      if (to != null) {
        target = target.queryParam("to", to.toString());
      }
      cf = cf.thenCombine(target.request("application/stack.samples+json")
              .rx().get(InputStream.class),
              (SampleNode resp, InputStream input) -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                  SampleNode.parseInto(br, resp);
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
                return resp;
              });
    }
    cf.whenComplete((samples, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(samples);
      }
    });


  }

  @Path("cluster/visualize/traces/{trId}")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public StreamingOutput visualize(@PathParam("trId") final String traceId) throws IOException {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream os) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
          profiles.getVisualizePage().apply(new FlameGraphParams("Request profile for: " + traceId,
                  "/profiles/cluster/traces/" + UriComponent.encode(traceId, UriComponent.Type.PATH_SEGMENT)
                  + "?_Accept=application/stack.samples.d3%2Bjson"), bw);
        }
      }
    };

  }

  @Path("cluster/visualize/groups/{label}")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public StreamingOutput visualizeGroups(@PathParam("label") final String label,
          @Nullable @QueryParam("from") final Instant from,
          @Nullable @QueryParam("to") final Instant to) throws IOException {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream os) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
          StringBuilder url = new StringBuilder(64);
          url.append("/profiles/cluster/groups/" + UriComponent.encode(label, UriComponent.Type.PATH_SEGMENT)
                  + "?_Accept=application/stack.samples.d3%2Bjson");
          if (from != null) {
            url.append("&from=");
            url.append(UriComponent.encode(from.toString(), UriComponent.Type.QUERY_PARAM));
          }
          if (to != null) {
            url.append("&to=");
            url.append(UriComponent.encode(to.toString(), UriComponent.Type.QUERY_PARAM));
          }
          profiles.getVisualizePage().apply(new FlameGraphParams("Cluster level profiles for: " +  label, url), bw);
        }
      }
    };

  }

}
