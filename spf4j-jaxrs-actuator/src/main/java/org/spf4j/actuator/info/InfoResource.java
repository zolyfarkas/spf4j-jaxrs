package org.spf4j.actuator.info;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.glassfish.hk2.api.Immediate;
import org.slf4j.LoggerFactory;
import org.spf4j.base.PackageInfo;
import org.spf4j.base.Reflections;
import org.spf4j.base.avro.ApplicationInfo;
import org.spf4j.base.avro.ProcessInfo;
import org.spf4j.cluster.Service;
import org.spf4j.cluster.ServiceInfo;
import org.spf4j.jaxrs.ConfigProperty;
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


  private final Service service;

  private final String hostName;

  @Inject
  public InfoResource(@ConfigProperty("hostName") @DefaultValue("127.0.0.1") final String hostName,
          final Service service) {
    this.service = service;
    this.hostName = hostName;
  }


  @GET
  public ApplicationInfo getApplicationInfo() {
    return org.spf4j.base.Runtime.getApplicationInfo();
  }

  @Path("local")
  @GET
  public ProcessInfo getProcessInfo() {
    return getProcessInfo(service.getServiceInfo());
  }

  ProcessInfo getProcessInfo(final ServiceInfo serviceInfo) {
    ProcessInfo.Builder builder = ProcessInfo.newBuilder()
            .setAppVersion(org.spf4j.base.Runtime.getAppVersionString())
            .setHostName(hostName)
            .setInstanceId(org.spf4j.base.Runtime.PROCESS_ID)
            .setName(org.spf4j.base.Runtime.PROCESS_NAME)
            .setJreVersion(org.spf4j.base.Runtime.JAVA_VERSION)
            .setNetworkServices(new ArrayList<>(serviceInfo.getServices()))
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


}
