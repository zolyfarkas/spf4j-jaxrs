package org.spf4j.jaxrs.server;

import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.StackSamples;
import org.spf4j.base.Throwables;
import org.spf4j.base.Methods;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecord;
import org.spf4j.http.ContextTags;
import org.spf4j.ssdump2.Converter;
import org.spf4j.jaxrs.Config;

/**
 * @author Zoltan Farkas
 */
@Provider
public final class LoggingExceptionMapper implements ExceptionMapper<Throwable>, ResponseErrorMapper {

  private static final Set<MediaType> SUPPORTED =  ImmutableSet.of(MediaType.valueOf("application/json"),
            MediaType.valueOf("application/avro+json"),
            MediaType.valueOf("application/avro"),
            MediaType.valueOf("text/plain"));

  private final String host;

  private final ContainerRequestContext reqCtx;

  private final DebugDetailEntitlement allowClientDebug;

  @Inject
  public LoggingExceptionMapper(@Config("baseUri") final String uriStr,
         @Context final ContainerRequestContext reqCtx,
         final DebugDetailEntitlement allowClientDebug)
         throws URISyntaxException, UnknownHostException {
    URI uri = new URI(uriStr);
    this.host = InetAddress.getByName(uri.getHost()).getHostName();
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
      ctx.putToRootParent(ContextTags.LOG_LEVEL, Level.ERROR);
    }
    ctx.addToRootParent(ContextTags.LOG_ATTRIBUTES, exception);
    List<Slf4jLogRecord> ctxLogs = new ArrayList<>();
    ExecutionContext curr = ctx;
    while (curr != null) {
      curr.streamLogs((log) -> {
        ctxLogs.add(log);
      });
      curr = curr.getSource();
    }
    Collections.sort(ctxLogs, Slf4jLogRecord::compareByTimestamp);
    StackSamples ss = ctx.getAndClearStackSamples();
    List<StackSampleElement> sses;
    if (ss == null) {
      sses = Collections.EMPTY_LIST;
    } else {
      sses = new ArrayList<>(64);
      Converter.convert(Methods.ROOT, ss, -1, 0, (a, b) -> sses.add(a));
    }
    return Response.status(status)
            .entity(new ServiceError(status, exception.getClass().getName(),
                    message, payload,
                    allowClientDebug.test(reqCtx.getSecurityContext())
                            ? new DebugDetail(host + '/' + ctx.getName(),
                              Converters.convert("", ctx.getId().toString(), ctxLogs),
                              Converters.convert(exception), sses)
                            : null))
            .type(getMediaType())
            .build();
  }


  private MediaType getMediaType() {
    List<MediaType> acceptableMediaTypes = reqCtx.getAcceptableMediaTypes();
    for (MediaType mt : acceptableMediaTypes) {
      if (SUPPORTED.contains(mt)) {
        return mt;
      }
    }
    return MediaType.APPLICATION_JSON_TYPE;
  }

  @Override
  public String toString() {
    return "LoggingExceptionMapper{" + "host=" + host + '}';
  }

}
