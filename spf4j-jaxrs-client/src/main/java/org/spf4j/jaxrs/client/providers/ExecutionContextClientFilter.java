package org.spf4j.jaxrs.client.providers;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.log.LogAttribute;

/**
 * A client filter for setting the following HTTP timeout headers:
 * deadline
 * timeout
 * requestId
 *
 * @author Zoltan farkas
 */
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public final class ExecutionContextClientFilter implements ClientRequestFilter,
        ClientResponseFilter {

  private static final Logger LOG =
          new ExecContextLogger(LoggerFactory.getLogger(ExecutionContextClientFilter.class));

  private final DeadlineProtocol protocol;

  private final boolean hideAuthorizationWhenLogging;

  @Inject
  public ExecutionContextClientFilter(final DeadlineProtocol protocol,
    final boolean hideAuthorizationWhenLogging) {
    this.protocol = protocol;
    this.hideAuthorizationWhenLogging = hideAuthorizationWhenLogging;
  }

  @Override
  public void filter(final ClientRequestContext requestContext) {
    ExecutionContext reqCtx = ExecutionContexts.current();
    MultivaluedMap<String, Object> headers = requestContext.getHeaders();
    long deadlineNanos = reqCtx.getDeadlineNanos();
    long timeoutNanos  = protocol.serialize(headers::addFirst, deadlineNanos);
    headers.add(Headers.REQ_ID, reqCtx.getId());
    int readTimeoutMs = (int) (timeoutNanos / 1000000);
    requestContext.setProperty(ClientProperties.READ_TIMEOUT, readTimeoutMs);
    LOG.debug("Invoking {}", new Object[]{requestContext.getUri(),
      LogAttribute.of("headers", hideAuthorizationWhenLogging
              ? Maps.transformEntries(headers, (final String k, final Object v) -> {
        if ("Authorization".equalsIgnoreCase(k)) {
          return "HIDDEN";
        } else {
          return v;
        }
    }) : headers)});
  }

  @Override
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext)
          throws IOException {
    List<String> warnings = responseContext.getHeaders().get(Headers.WARNING);
    if (warnings != null && !warnings.isEmpty()) {
      List<HttpWarning> pws = warnings.stream().map((w) ->  HttpWarning.parse(w))
              .collect(Collectors.toCollection(() -> new ArrayList<>(warnings.size())));
      ExecutionContext reqCtx = ExecutionContexts.current();
      LOG.warn("Done {}", requestContext.getUri(),
        LogAttribute.traceId(reqCtx.getId()),
        LogAttribute.of("warnings", pws),
        LogAttribute.value("httpStatus", responseContext.getStatus()),
        LogAttribute.execTimeMicros(TimeSource.nanoTime() - reqCtx.getStartTimeNanos(), TimeUnit.NANOSECONDS));
    } else if (LOG.isDebugEnabled()) {
      ExecutionContext reqCtx = ExecutionContexts.current();
      LOG.debug("Done {}", requestContext.getUri(),
        LogAttribute.traceId(reqCtx.getId()),
        LogAttribute.value("httpStatus", responseContext.getStatus()),
        LogAttribute.execTimeMicros(TimeSource.nanoTime() - reqCtx.getStartTimeNanos(), TimeUnit.NANOSECONDS));
    }
  }

  @Override
  public String toString() {
    return "ExecutionContextClientFilter{" + "protocol=" + protocol + '}';
  }

}
