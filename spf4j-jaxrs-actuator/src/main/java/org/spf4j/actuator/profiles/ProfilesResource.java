package org.spf4j.actuator.profiles;

import java.io.IOException;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author Zoltan Farkas
 */
@Path("profiles/local")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
public class ProfilesResource {

  private final LogsResource logsResource;

  @Inject
  public ProfilesResource(final LogsResource logsResource) {
    this.logsResource = logsResource;
  }

  @Path("{trId}")
  @GET
  @Produces(value = {"application/stack.samples+json", "application/stack.samples.d3+json", "application/avro"})
  public SampleNode getSamples(final String traceId) throws IOException {
    StringBuilder sb = new StringBuilder(traceId.length());
    AppendableUtils.escapeJsonString(traceId, sb);
    List<LogRecord> logs = logsResource.getLocalLogs(0, 2, "log.trId == \""
            +  sb + "\"", Order.DESC);
    if (logs.isEmpty()) {
      return null;
    }
    return Converter.convert(logs.get(0).getStackSamples().iterator());
  }



}
