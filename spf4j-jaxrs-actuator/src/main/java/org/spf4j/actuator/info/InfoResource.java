package org.spf4j.actuator.info;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import org.glassfish.hk2.api.Immediate;
import org.slf4j.LoggerFactory;
import org.spf4j.base.PackageInfo;
import org.spf4j.base.Reflections;
import org.spf4j.base.avro.ApplicationInfo;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.ProcessInfo;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.jaxrs.ConfigProperty;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.log.ExecContextLogger;

/**
 *
 * @author Zoltan Farkas
 */
@Path("info")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@Immediate
@RolesAllowed("operator")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class InfoResource {

  private static final org.slf4j.Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(InfoResource.class));


  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final String hostName;

  @Inject
  public InfoResource(@ConfigProperty("hostName") @DefaultValue("hostName") final String hostName,
          final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.hostName = "hostName".equals(hostName) ? cluster.getLocalHostName() : hostName;
  }



  @GET
  public ApplicationInfo getApplicationInfo() {
    return org.spf4j.base.Runtime.getApplicationInfo();
  }

  @Path("local")
  @GET
  public ProcessInfo getProcessInfo() {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    return getProcessInfo(clusterInfo);
  }

  private ProcessInfo getProcessInfo(final ClusterInfo clusterInfo) {
    ProcessInfo.Builder builder = ProcessInfo.newBuilder()
            .setAppVersion(org.spf4j.base.Runtime.getAppVersionString())
            .setHostName(hostName)
            .setInstanceId(org.spf4j.base.Runtime.PROCESS_ID)
            .setName(org.spf4j.base.Runtime.PROCESS_NAME)
            .setJreVersion(org.spf4j.base.Runtime.JAVA_VERSION)
            .setNetworkServices(new ArrayList<>(clusterInfo.getServices()))
            .setProcessId(org.spf4j.base.Runtime.PID);
    URL jarSourceUrl = PackageInfo.getJarSourceUrl(org.spf4j.base.Runtime.getMainClass());
    if (jarSourceUrl != null) {
      Manifest manifest;
      try {
        manifest = Reflections.getManifest(jarSourceUrl);
        Attributes mainAttributes = manifest.getMainAttributes();
        //        <Implementation-Build>${buildNumber}</Implementation-Build>
        //        <Build-Time>${maven.build.timestamp}</Build-Time>
        String buildId = mainAttributes.getValue("Implementation-Build");
        if (buildId != null) {
          builder.setBuildId(buildId);
        }
        String ts = mainAttributes.getValue("Build-Time");
        if (ts != null) {
          try {
            builder.setBuildTimeStamp(Instant.parse(ts));
          } catch (RuntimeException ex) {
           LOG.warn("cannot parse build time {}", ts, ex);
          }
        }
      } catch (IOException ex) {
        LOG.warn("cannot read manifest for {}", jarSourceUrl, ex);
      }
    }
    return builder.build();
  }

  @Path("cluster")
  @GET
  public void getClusterInfo(@Suspended final AsyncResponse ar) throws URISyntaxException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    List<ProcessInfo> result = Collections.synchronizedList(new ArrayList(peerAddresses.size() + 1));
    result.add(getProcessInfo(clusterInfo));
    CompletableFuture<List<ProcessInfo>> cf = ContextPropagatingCompletableFuture.completedFuture(result);
    NetworkService service = clusterInfo.getHttpService();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(service.getName(), null,
              addr.getHostAddress(), service.getPort(), "/info/local", null, null);
      cf = cf.thenCombine(httpClient.target(uri)
              .request("application/avro").rx().get(ProcessInfo.class),
              (res, info) -> {
                res.add(info);
                return res;
              });
    }
    cf.whenComplete((res, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(new org.spf4j.base.avro.ClusterInfo(getApplicationInfo(), res));
      }
    });
  }

}
