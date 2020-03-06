package org.spf4j.jaxrs.server.providers;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
import org.spf4j.base.ContextValue;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Throwables;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.log.Level;
import org.spf4j.http.ContextTags;
import org.spf4j.jaxrs.server.DebugDetailEntitlement;
import org.spf4j.jaxrs.server.MediaTypes;
import org.spf4j.servlet.CountingHttpServletResponse;

/**
 * @author Zoltan Farkas
 */
@Provider
public final class LoggingExceptionMapper implements ExceptionMapper<Throwable>, ResponseErrorMapper {

  private static final Set<MediaType> SUPPORTED;

  static {
    Set<MediaType> linkedHs = new LinkedHashSet<>(7);
    linkedHs.add(MediaType.valueOf("application/json"));
    linkedHs.add(MediaType.valueOf("application/avro+json"));
    linkedHs.add(MediaType.valueOf("application/avro"));
    linkedHs.add(MediaType.valueOf("text/plain"));
    SUPPORTED = linkedHs;
  }

  private final String host;

  private final ContainerRequestContext reqCtx;

  private final DebugDetailEntitlement allowClientDebug;

  @Inject
  public LoggingExceptionMapper(
         @ConfigProperty(name = "hostName", defaultValue = "hostName") final String host,
         @Context final ContainerRequestContext reqCtx,
         final DebugDetailEntitlement allowClientDebug) {
    this.host = host;
    this.reqCtx = reqCtx;
    if (allowClientDebug == null) {
      Logger.getLogger(LoggingExceptionMapper.class.getName())
              .warning("LoggingExceptionMapper will send debug detail to all clients");
      this.allowClientDebug = ((x) -> true);
    } else {
      this.allowClientDebug = allowClientDebug;
    }
  }

  @Override
  public Response toResponse(final Throwable exception) {
    WebApplicationException wex = Throwables.first(exception, WebApplicationException.class);
    String message = exception.getMessage();
    if (message == null) {
      message = "";
    }
    int status;
    Object payload;
    if (wex != null) {
      Response response = wex.getResponse();
      status = response.getStatus();
      if (response.hasEntity()) {
        payload = response.getEntity();
        if (payload instanceof InputStream) {
          payload = null;
        }
      } else {
        payload = null;
      }
    } else {
      status = 500;
      payload = null;
    }
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx == null) { // Exception mapper can execute in a timeout thread, where context is not available,
      Logger.getLogger("handling.error")
              .log(java.util.logging.Level.WARNING, "No request context available", exception);
      ServiceError.Builder errBuilder = ServiceError.newBuilder()
              .setCode(status);
      if (allowClientDebug.test(reqCtx.getSecurityContext())) {
              errBuilder.setDetail(new DebugDetail(host,
                      Collections.EMPTY_LIST, Converters.convert(exception), Collections.EMPTY_LIST));
      }
      errBuilder.setType(exception.getClass().getName())
              .setMessage(message).setPayload(payload);
      return Response.serverError()
              .entity(errBuilder.build())
              .type(getMediaType())
              .build();
    }
    if (status >= 500) {
      ContextValue<CountingHttpServletResponse> contextAndValue = ctx.getContextAndValue(ContextTags.HTTP_RESP);
      contextAndValue.getContext().accumulate(ContextTags.LOG_LEVEL, Level.ERROR);
    }
    ctx.accumulateComponent(ContextTags.LOG_ATTRIBUTES, exception);
    String reqProfileOnError = reqCtx.getHeaderString("X-Req-Profile-On-Error");
    boolean isReqProfileOnError = "true".equalsIgnoreCase(reqProfileOnError);
    return Response.status(status)
            .entity(new ServiceError(status, exception.getClass().getName(),
                    message, payload,
                    allowClientDebug.test(reqCtx.getSecurityContext())
                            ? ctx.getDebugDetail(host, exception, isReqProfileOnError)
                            : null))
            .type(getMediaType())
            .build();
  }


  private MediaType getMediaType() {
    List<MediaType> acceptableMediaTypes = reqCtx.getAcceptableMediaTypes();
    return MediaTypes.getMatch(acceptableMediaTypes, SUPPORTED);
  }

  @Override
  public String toString() {
    return "LoggingExceptionMapper{" + "host=" + host + '}';
  }

}
