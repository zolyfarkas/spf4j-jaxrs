package org.spf4j.actuator.cluster.logs;

import org.spf4j.jaxrs.server.AsyncResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.StreamingOutput;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.Order;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.LogPrinter;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logs/cluster")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
public class LogsClusterResource {

  private static final Comparator<LogRecord> L_COMP = new Comparator<LogRecord>() {
    @Override
    public int compare(final LogRecord o1, final LogRecord o2) {
      return o2.getTs().compareTo(o1.getTs());
    }
  };

  private static final Comparator<LogRecord> N_COMP = new Comparator<LogRecord>() {
    @Override
    public int compare(final LogRecord o1, final LogRecord o2) {
      return o1.getTs().compareTo(o2.getTs());
    }
  };

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final LogsResource localLogs;

  @Inject
  public LogsClusterResource(final LogsResource localLogs,
          final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.localLogs = localLogs;
  }

  @GET
  @Produces(value = {"text/plain"})
  public void getClusterLogsText(@QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @QueryParam("order") @DefaultValue("DESC") final Order resOrder,
          @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    getClusterLogs(limit, filter, resOrder, new AsyncResponseWrapper(ar) {
      @Override
      public boolean resume(final Object response) {
        return super.resume(new StreamingOutput() {
          @Override
          public void write(final OutputStream output) throws IOException, WebApplicationException {
            LogPrinter printer = new LogPrinter(StandardCharsets.UTF_8);
            for (LogRecord record : (Iterable<LogRecord>) response) {
              printer.print(record, output);
            }
          }
        });
      }

    });
  }

  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  public void getClusterLogs(@QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @QueryParam("order") @DefaultValue("DESC") final Order resOrder,
          @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    getClusterLogs(limit, filter, resOrder, "default", ar);
  }

  @Path("{appenderName}")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  public void getClusterLogs(@QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter,
           @QueryParam("order") @DefaultValue("DESC") final Order resOrder,
          @PathParam("appenderName") final String appender, @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    CompletableFuture<PriorityQueue<LogRecord>> cf
            = ContextPropagatingCompletableFuture.supplyAsync(() -> {
              PriorityQueue<LogRecord> result = new PriorityQueue(limit, N_COMP);
              Collection<LogRecord> ll;
              try {
                ll = localLogs.getLocalLogs(0, limit, filter, resOrder, appender);
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
              addAll(limit, result, ll);
              return result;
            }, DefaultExecutor.INSTANCE);

    NetworkService service = clusterInfo.getHttpService();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(service.getName(), null,
                  addr.getHostAddress(), service.getPort(), "/logs/local", null, null);
      Spf4jWebTarget invTarget = httpClient.target(uri)
              .path(appender)
              .queryParam("limit", limit);
      if (filter != null) {
        invTarget = invTarget.queryParam("filter", filter);
      }
      cf = cf.thenCombine(
              invTarget.request("application/avro").rx().get(new GenericType<List<LogRecord>>() {
              }),
              (PriorityQueue<LogRecord> result, List<LogRecord> rl) -> {
                addAll(limit, result, rl);
                return result;
              }
      );
    }
    cf.whenComplete((records, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ArrayList<LogRecord> result = new ArrayList(limit);
        result.addAll(records);
        Collections.sort(result, (resOrder == Order.DESC) ? L_COMP : N_COMP);
        ar.resume(result);
      }
    });
  }

  private static void addAll(final int limit,
          final PriorityQueue<LogRecord> result,
          final Collection<LogRecord> records) {
    synchronized (result) {
      result.addAll(records);
      int removeCnt = result.size() - limit;
      for (int i = 0; i < removeCnt; i++) {
        result.remove();
      }
    }
  }

  @Override
  public String toString() {
    return "LogsClusterResource{" + "cluster=" + cluster + ", httpClient=" + httpClient + '}';
  }

}
