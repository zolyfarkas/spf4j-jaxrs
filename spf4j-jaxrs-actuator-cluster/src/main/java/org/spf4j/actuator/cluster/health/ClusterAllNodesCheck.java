package org.spf4j.actuator.cluster.health;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.spf4j.actuator.health.HealthCheck;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 * @author Zoltan Farkas
 */
@Service
@Singleton
public final class ClusterAllNodesCheck implements HealthCheck {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final long timeoutNanos;

  @Inject
  public ClusterAllNodesCheck(final Cluster cluster,
          final Spf4JClient httpClient,
          @ConfigProperty(name = "spf4j.health.cluster.timeoutMillis", defaultValue = "10000")
          final long timeouMillis) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeouMillis);
  }

  @Override
  public void test(final Logger logger) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public HealthRecord getRecord(final String name, final String origin,
          final Logger logger, final boolean isDebug,
          final boolean isDebugOnError) {
    try (ExecutionContext ec = ExecutionContexts.start(name,
            timeout(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS)) {
      ClusterInfo clusterInfo = cluster.getClusterInfo();
      Set<InetAddress> addresses = clusterInfo.getAddresses();

      NetworkService service = clusterInfo.getHttpService();
      CompletableFuture<List<HealthRecord>> cf
              = ContextPropagatingCompletableFuture.completedFuture(Collections.synchronizedList(
                      new ArrayList<>(addresses.size())));

      for (InetAddress addr : addresses) {
        URI uri;
        try {
          uri = new URI(service.getName(), null,
                  addr.getHostAddress(), service.getPort(), "/health/check/local", null, null);
        } catch (URISyntaxException ex) {
           throw new RuntimeException(ex);
        }
        cf = cf.thenCombine(httpClient.target(uri)
                .request("application/avro").rx().get(HealthRecord.class),
                (res, info) -> {
                  res.add(info);
                  return res;
                });
      }
      List<HealthRecord> result;
      try {
        result = cf.get();
        return new HealthRecord(origin, name, HealthStatus.HEALTHY,
                isDebug ? ec.getDebugDetail(origin, null) : null, result);
      } catch (InterruptedException | ExecutionException | RuntimeException ex) {
        return new HealthRecord(origin, name, HealthStatus.HEALTHY,
                isDebugOnError ? ec.getDebugDetail(origin, ex) : null,
                Collections.EMPTY_LIST);
      }
    }
  }

  @Override
  public String info() {
    return "A health checks, that runs and aggregates the health checks of all cluster members";
  }

  @Override
  public Type getType() {
    return Type.cluster;
  }

  @Override
  public long timeout(final TimeUnit tu) {
    return tu.convert(timeoutNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public String toString() {
    return "ClusterAllNodesCheck{" + "cluster=" + cluster + ", httpClient=" + httpClient
            + ", timeoutNanos=" + timeoutNanos + '}';
  }



}
