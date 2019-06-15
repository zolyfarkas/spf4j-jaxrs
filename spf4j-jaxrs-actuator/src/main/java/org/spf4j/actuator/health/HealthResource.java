package org.spf4j.actuator.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.health.HealthCheck.Type;
import org.spf4j.base.avro.HealthCheckInfo;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;
import org.spf4j.jaxrs.server.DebugDetailEntitlement;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.jaxrs.ConfigProperty;

/**
 * @author Zoltan Farkas
 */
@Path("health")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class HealthResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(HealthResource.class));

  private final Supplier<HealthOrgNode> checkSupplier;

  private final String host;

  private final DebugDetailEntitlement ddEnt;

  @Inject
  public HealthResource(final Iterable<HealthCheck.Registration> healthChecks,
          final DebugDetailEntitlement ddEnt,
          @ConfigProperty("hostName") @DefaultValue("hostName") final String host) {
    this.ddEnt = ddEnt;
    this.host = host;
    checkSupplier = () -> {
      HealthOrgNode checks = HealthOrgNode.newHealthChecks();
      for (HealthCheck.Registration registration : healthChecks) {
        String[] path = registration.getPath();
        HealthCheck check = registration.getCheck();
        switch (check.getType()) {
          case local:
            path = org.spf4j.base.Arrays.preppend(path, Type.local.toString());
            break;
          case cluster:
            path = org.spf4j.base.Arrays.preppend(path, Type.cluster.toString());
            break;
          case custom:
            break;
          default:
            throw new UnsupportedOperationException("Unsupported health check type " + check.getType());
        }
        checks.addHealthCheck(check, path);
      }
      return checks;
    };
  }

  @GET
  @Path("ping")
  public String ping() {
    return "pong";
  }

  @GET
  @Path("info")
  public HealthCheckInfo list(@QueryParam("maxDepth") @DefaultValue("10") final int maxDepth) {
    return list(Collections.EMPTY_LIST, maxDepth);
  }

  @GET
  @Path("info/{path:.*}")
  public HealthCheckInfo list(@PathParam("path") final List<PathSegment> path,
          @QueryParam("maxDepth") @DefaultValue("10") final int maxDepth) {
    String[] pathArr = toStrArray(path);
    HealthOrgNode hNode = checkSupplier.get().getHealthNode(pathArr);
    if (hNode == null) {
      throw new NotFoundException("No health checks at " + path);
    }
    return hNode.getHealthCheckInfo("", maxDepth);
  }

  private static String[] toStrArray(final List<PathSegment> path) {
    String[] pathArr = new String[path.size()];
    int i = 0;
    for (PathSegment seg : path) {
      pathArr[i++] = seg.getPath();
    }
    return pathArr;
  }

  @GET
  @Path("check")
  @Operation(
         description = "Run all health checks",
         responses = {
           @ApiResponse(
                 description = "All health checks are successful",
                 responseCode = "200",
                 content = @Content(schema = @Schema(implementation = HealthRecord.class))),
            @ApiResponse(
                 description = "In  case a health check fails",
                 responseCode = "503",
                 content = @Content(schema = @Schema(implementation = HealthRecord.class)))
         }
  )
  public Response run(
          @QueryParam("debug") @DefaultValue("false") final boolean pisDebug,
          @QueryParam("debugOnError") @DefaultValue("true") final boolean pisDebugOnError,
          @Context final SecurityContext secCtx) {
    return run(Collections.EMPTY_LIST, pisDebug, pisDebugOnError, secCtx);
  }

  @GET
  @Path("check/{path:.*}")
  @Operation(
         description = "Run a health check",
         responses = {
           @ApiResponse(
                 description = "All health checks are successful",
                 responseCode = "200",
                 content = @Content(schema = @Schema(implementation = HealthRecord.class))),
            @ApiResponse(
                 description = "In  case a health check fails",
                 responseCode = "503",
                 content = @Content(schema = @Schema(implementation = HealthRecord.class)))
         }
  )
  public Response run(@PathParam("path") final List<PathSegment> path,
          @QueryParam("debug") @DefaultValue("false") final boolean pisDebug,
          @QueryParam("debugOnError") @DefaultValue("true") final boolean pisDebugOnError,
          @Context final SecurityContext secCtx) {
    boolean isDebug = pisDebug;
    boolean isDebugOnError = pisDebugOnError;
    if (!ddEnt.test(secCtx)) {
      isDebug = false;
      isDebugOnError = false;
    }
    String[] pathArr = toStrArray(path);
    HealthOrgNode hNode = checkSupplier.get().getHealthNode(pathArr);
    HealthRecord healthRecord = hNode.getHealthRecord("", host, LOG, isDebug, isDebugOnError);
    if (healthRecord.getStatus() == HealthStatus.HEALTHY) {
      return Response.ok(healthRecord).build();
    } else {
      return Response.status(503).entity(healthRecord).build();
    }
  }

  @Override
  public String toString() {
    return "HealthResource{" + "checks=" + checkSupplier + ", host=" + host + ", ddEnt=" + ddEnt + '}';
  }

}
