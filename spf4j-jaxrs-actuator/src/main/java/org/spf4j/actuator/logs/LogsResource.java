package org.spf4j.actuator.logs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.log.AvroDataFileAppender;
import org.spf4j.log.LogbackUtils;
import org.spf4j.os.OperatingSystem;
import org.spf4j.zel.vm.CompileException;
import org.spf4j.zel.vm.Program;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logs/local")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
@Singleton
public class LogsResource {

  private final String hostName;

  @Inject
  public LogsResource(@ConfigProperty(name = "hostName", defaultValue = "") final String hostName) {
    this.hostName = hostName.isEmpty() ? OperatingSystem.getHostName() : hostName;
  }

  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @ProjectionSupport
  public List<LogRecord> getLocalLogs(
          @QueryParam("tailOffset") @DefaultValue("0") final long tailOffset,
          @QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @QueryParam("order") @DefaultValue("DESC") final Order resOrder) throws IOException {
    return getLocalLogs(tailOffset, limit, filter, resOrder, "default");
  }

  @Path("{appenderName}")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @ProjectionSupport
  public  List<LogRecord> getLocalLogs(
          @QueryParam("tailOffset") @DefaultValue("0") final long tailOffset,
          @QueryParam("limit") @DefaultValue("100") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @QueryParam("order") @DefaultValue("DESC") final Order resOrder,
          @PathParam("appenderName") final String appenderName) throws IOException {
    if (limit == 0) {
      return Collections.emptyList();
    }
    if (limit < 0) {
      throw new ClientErrorException("limit parameter must be positive: " + limit, 400);
    }
    Map<String, AvroDataFileAppender> appenders = LogbackUtils.getConfiguredFileAppenders();
    AvroDataFileAppender fa = appenders.get(appenderName);
    if (fa == null) {
      throw new NotFoundException("Resource not available: " + appenderName);
    }
    List<LogRecord> result = new ArrayList<>(limit);
    if (filter != null) {
      try {
        fa.getFilteredLogs(hostName, tailOffset, limit, Program.compilePredicate(filter, "log"), result::add);
      } catch (CompileException ex) {
        throw new ClientErrorException("Invalid filter " + filter + ", " + ex.getMessage(), 400, ex);
      }
    } else {
       fa.getLogs(hostName, tailOffset, limit, result::add);
    }
    if (resOrder == Order.DESC) {
      Collections.reverse(result);
    }
    return result;
  }

  @Override
  public String toString() {
    return "LogsResource{" + "hostName=" + hostName + '}';
  }

}
