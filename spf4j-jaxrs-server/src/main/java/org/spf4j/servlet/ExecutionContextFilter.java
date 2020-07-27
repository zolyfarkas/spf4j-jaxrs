package org.spf4j.servlet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.http.ContextTags;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.avro.SchemaResolver;
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.base.Arrays;
import org.spf4j.base.Env;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.StackSamples;
import org.spf4j.base.SysExits;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.server.SecurityAuthenticator;
import org.spf4j.io.LazyOutputStreamWrapper;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.common.providers.avro.XJsonAvroMessageBodyWriter;
import org.spf4j.log.Level;
import org.spf4j.log.LogAttribute;
import org.spf4j.log.LogUtils;
import org.spf4j.log.Slf4jLogRecord;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.impl.RecorderFactory;

/**
 * A Filter for REST services.
 *
 * This filter implements the following:
 * <ul>
 * <li>Deadline propagation, fully customizable via:
 * DeadlineProtocol and the DefaultDeadlineProtocol implementation.</li>
 * <li>Configurable header overwrite via query Parameters.</li>
 * <li>Standard access log</li>
 * <li>Debug logs on error.</li>
 * <li>Profiling information on error.</li>
 * <li>Execution time, timeout relative access log level upgrade.</li>
 * <li>Execution context creation/closing.</li>
 * <li>Context log level overwrites.</li>
 * </ul>
 */
@WebFilter(asyncSupported = true)
@ParametersAreNonnullByDefault
public final class ExecutionContextFilter implements Filter {

  public static final String CFG_ID_HEADER_NAME = "spf4j.jaxrs.idHeaderName";

  public static final String CFG_CTX_LOG_LEVEL_HEADER_NAME = "spf4j.jaxrs.ctxLogLevelHeaderName";

  public static final String CFG_HEADER_OVERWRITE_QP_PREFIX = "spf4j.jaxrs.headerOverwriteQueryParamPrefix";

  private static final MeasurementRecorderSource EXEC_TIME_STATS
                    = RecorderFactory.createScalableQuantizedRecorderSource("requestExecTime", "microSecond",
                      60000, 1000, 0, 6, 10);


  private static final MeasurementRecorder BYTES_IN
                     = RecorderFactory.createScalableMinMaxAvgRecorder("ContentBytesIn", "Bytes", 60000);

  private static final MeasurementRecorder BYTES_OUT
                     = RecorderFactory.createScalableMinMaxAvgRecorder("ContentBytesOut", "Bytes", 60000);

  private final DeadlineProtocol deadlineProtocol;

  private final SecurityAuthenticator auth;

  private String idHeaderName;

  private String ctxLogLevelHeaderName;

  private Logger log;

  private float warnThreshold;

  private float errorThreshold;

  private String headerOverwriteQueryParamPrefix;


  public ExecutionContextFilter(final SecurityAuthenticator auth) {
    this(new DefaultDeadlineProtocol(), auth);
  }

  public ExecutionContextFilter(final DeadlineProtocol deadlineProtocol, final SecurityAuthenticator auth) {
    this(deadlineProtocol, auth, Env.getValue("WARN_TIME_ERROR_THRESHOLD", 0.4f),
            Env.getValue("EXEC_TIME_ERROR_THRESHOLD", 0.9f));
  }

  public ExecutionContextFilter(final DeadlineProtocol deadlineProtocol,
          final SecurityAuthenticator auth,
          final float warnThreshold, final float errorThreshold) {
    this.deadlineProtocol = deadlineProtocol;
    this.auth = auth;
    this.warnThreshold = warnThreshold;
    this.errorThreshold = errorThreshold;
    this.headerOverwriteQueryParamPrefix = "_";
  }

  public DeadlineProtocol getDeadlineProtocol() {
    return deadlineProtocol;
  }

  @Override
  public void init(final FilterConfig filterConfig) {
    log = Logger.getLogger("org.spf4j.servlet." + filterConfig.getFilterName());
    ctxLogLevelHeaderName = Filters.getStringParameter(filterConfig,
            CFG_CTX_LOG_LEVEL_HEADER_NAME, Headers.CTX_LOG_LEVEL);
    idHeaderName = Filters.getStringParameter(filterConfig, CFG_ID_HEADER_NAME, Headers.REQ_ID);
    headerOverwriteQueryParamPrefix = Filters.getStringParameter(filterConfig, CFG_HEADER_OVERWRITE_QP_PREFIX, "_");
  }


  @SuppressFBWarnings("SERVLET_QUERY_STRING")
  private HttpServletRequest overwriteHeadersIfNeeded(final HttpServletRequest request) {
    String queryStr = request.getQueryString();
    if (queryStr == null || !queryStr.contains(headerOverwriteQueryParamPrefix)) {
      return request;
    }
    MultivaluedMap<String, String> qPS = UriComponent.decodeQuery(queryStr, true);
    MultivaluedMap<String, String> overwrites = null;
    for (Map.Entry<String, List<String>> entry : qPS.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(headerOverwriteQueryParamPrefix)) {
        if (overwrites == null) {
          overwrites = new MultivaluedHashMap<>(4);
        }
        overwrites.put(key.substring(headerOverwriteQueryParamPrefix.length()), entry.getValue());
      }
    }
    if (overwrites == null) {
      return request;
    } else {
      return new HeaderOverwriteHttpServletRequest(request, overwrites);
    }
  }


  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
          throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }
    CountingHttpServletRequest httpReq = new CountingHttpServletRequest(
            overwriteHeadersIfNeeded((HttpServletRequest) request));
    CountingHttpServletResponse httpResp = new CountingHttpServletResponse((HttpServletResponse) response);
    String name = httpReq.getMethod() + '/' + httpReq.getRequestURL();
    String reqId = httpReq.getHeader(idHeaderName);
    JaxRsSecurityContext secCtx = auth.authenticate(httpReq::getHeader);
    long startTimeNanos = TimeSource.nanoTime();
    long deadlineNanos;
    try {
      deadlineNanos = deadlineProtocol.deserialize(httpReq::getHeader, startTimeNanos);
    } catch (IllegalArgumentException ex) {
      errorResponse(httpResp, 400, "Invalid deadline/timeout", "", ex, secCtx);
      logRequestEnd(org.spf4j.log.Level.WARN, name, reqId, httpReq, httpResp);
      return;
    }
    ExecutionContext ctx = ExecutionContexts.start(name, reqId, null, startTimeNanos, deadlineNanos);
    ctx.put(ContextTags.HTTP_REQ, httpReq);
    ctx.put(ContextTags.HTTP_RESP, httpResp);
    ctx.put(ContextTags.SECURITY_CONTEXT, secCtx);
    String ctxLoglevel = httpReq.getHeader(ctxLogLevelHeaderName);
    if (ctxLoglevel != null) {
      try {
        ctx.setBackendMinLogLevel(Level.valueOf(ctxLoglevel));
      } catch (IllegalArgumentException ex) {
        errorResponse(httpResp, 400, "Invalid log level", ctxLoglevel, ex, secCtx);
        logRequestEnd(org.spf4j.log.Level.WARN, name, reqId, httpReq, httpResp);
        return;
      }
    }
    try {
      chain.doFilter(httpReq, httpResp);
      if (request.isAsyncStarted()) {
        ctx.detach();
        AsyncContext asyncContext = request.getAsyncContext();
        asyncContext.setTimeout(ctx.getMillisToDeadline());
        asyncContext.addListener(new AsyncListener() {
          @Override
          public void onComplete(final AsyncEvent event) {
            try {
              httpResp.flushBuffer();
            } catch (IOException ex) {
              throw new UncheckedIOException(ex);
            } finally {
              logRequestEnd(org.spf4j.log.Level.INFO, ctx, httpReq, httpResp);
              ctx.close();
            }
          }

          @Override
          public void onTimeout(final AsyncEvent event) {
            ctx.accumulate(ContextTags.LOG_LEVEL, Level.ERROR);
            ctx.accumulateComponent(ContextTags.LOG_ATTRIBUTES, LogAttribute.of("warning", "Request timed out"));
          }

          @Override
          public void onError(final AsyncEvent event) {
            ctx.accumulate(ContextTags.LOG_LEVEL, Level.ERROR);
            ctx.accumulateComponent(ContextTags.LOG_ATTRIBUTES, event.getThrowable());
          }

          @Override
          public void onStartAsync(final AsyncEvent event) {
          }
        }, request, response);
      } else {
        logRequestEnd(org.spf4j.log.Level.INFO, ctx, httpReq, httpResp);
        ctx.close();
      }
    } catch (Throwable t) {
      if (Throwables.isNonRecoverable(t)) {
        org.spf4j.base.Runtime.goDownWithError(t, SysExits.EX_SOFTWARE);
      }
      ctx.accumulateComponent(ContextTags.LOG_ATTRIBUTES, t);
      logRequestEnd(org.spf4j.log.Level.ERROR, ctx, httpReq, httpResp);
    }
  }

  @SuppressFBWarnings("UCC_UNRELATED_COLLECTION_CONTENTS")
  private void logRequestEnd(final Level plevel,
          final ExecutionContext ctx, final CountingHttpServletRequest req, final CountingHttpServletResponse resp) {
    org.spf4j.log.Level level;
    org.spf4j.log.Level ctxOverride = ctx.get(ContextTags.LOG_LEVEL);
    if (ctxOverride != null && ctxOverride.ordinal() > plevel.ordinal()) {
      level = ctxOverride;
    } else {
      level = plevel;
    }
    Object[] args;
    List<Object> logAttrs = ctx.get(ContextTags.LOG_ATTRIBUTES);
    long startTimeNanos = ctx.getStartTimeNanos();
    long execTimeNanos = TimeSource.nanoTime() - startTimeNanos;
    long maxTime = ctx.getDeadlineNanos() - startTimeNanos;
    long etn = (long) (maxTime * errorThreshold);
    if (execTimeNanos > etn) {
      if (logAttrs == null) {
        logAttrs = new ArrayList<>(2);
      }
      logContextProfile(log, execTimeNanos, ctx);
      logAttrs.add(LogAttribute.of("performanceError", "exec time > " + etn + " ns"));
      if (level.ordinal() < Level.ERROR.ordinal()) {
        level = level.ERROR;
      }
    } else {
      long wtn = (long) (maxTime * warnThreshold);
      if (execTimeNanos > wtn) {
        if (logAttrs == null) {
          logAttrs = new ArrayList<>(2);
        }
        logContextProfile(log, execTimeNanos, ctx);
        logAttrs.add(LogAttribute.of("performanceWarning", "exec time > " + wtn + " ns"));
        if (level.ordinal() < Level.WARN.ordinal()) {
          level = level.WARN;
        }
      }
    }
    boolean clientWarning = false;
    Set<HttpWarning> warnings = ctx.get(ContextTags.HTTP_WARNINGS);
    if (warnings != null) {
      if (logAttrs == null) {
        logAttrs = new ArrayList<>(warnings);
      } else {
        logAttrs.addAll(warnings);
      }
      if (level.ordinal() < Level.WARN.ordinal()) {
        level = level.WARN;
        clientWarning = true;
      }
    }
    long execTimeMicros = TimeUnit.NANOSECONDS.toMicros(execTimeNanos);
    String remoteHost = getRemoteHost(req);
    int status = resp.getStatus();
    long bytesRead = req.getBytesRead();
    long bytesWritten = resp.getBytesWritten();
    EXEC_TIME_STATS.getRecorder(req.getMethod()).record(execTimeMicros);
    BYTES_IN.record(bytesRead);
    BYTES_OUT.record(bytesWritten);
    if (logAttrs == null) {
      args = new Object[]{ctx.getName(),
        LogAttribute.traceId(ctx.getId()),
        LogAttribute.of("clientHost", remoteHost),
        LogAttribute.value("httpStatus", status),
        LogAttribute.execTimeMicros(execTimeMicros, TimeUnit.MICROSECONDS),
        LogAttribute.value("inBytes", bytesRead),
        LogAttribute.value("outBytes", bytesWritten)
      };
    } else {
      args = new Object[7 + logAttrs.size()];
      args[0] = ctx.getName();
      args[1] = LogAttribute.traceId(ctx.getId());
      args[2] = LogAttribute.of("clientHost", remoteHost);
      args[3] = LogAttribute.value("httpStatus", status);
      args[4] = LogAttribute.execTimeMicros(execTimeMicros, TimeUnit.MICROSECONDS);
      args[5] = LogAttribute.value("inBytes", bytesRead);
      args[6] = LogAttribute.value("outBytes", bytesWritten);
      int i = 7;
      for (Object obj : logAttrs) {
        args[i++] = obj;
      }
    }
    try {
      if (!clientWarning && level.getIntValue() >= Level.WARN.getIntValue()) {
        try {
          logContextLogs(log, ctx, req, level);
        } catch (RuntimeException ex) {
          log.log(Level.ERROR.getJulLevel(), "Exception while dumping context detail", ex);
        }
      }
    } finally {
      log.log(level.getJulLevel(), "Done {0}", args);
    }
  }

  private void logRequestEnd(final Level level, final String reqStr,
          final String reqId, final CountingHttpServletRequest req,
          final CountingHttpServletResponse resp) {
    Object[] args;
    args = new Object[]{reqStr,
      LogAttribute.traceId(reqId),
      LogAttribute.of("clientHost", getRemoteHost(req)),
      LogAttribute.value("httpStatus", resp.getStatus()),
      LogAttribute.execTimeMicros(0, TimeUnit.NANOSECONDS),
      LogAttribute.value("inBytes", req.getBytesRead()), LogAttribute.value("outBytes", resp.getBytesWritten())
    };
    log.log(level.getJulLevel(), "Done {0}", args);
  }


  private void errorResponse(final HttpServletResponse resp,
          final int status, final String reasonPhrase, final String description,
          @Nullable final Throwable exception, final JaxRsSecurityContext secCtx)
          throws IOException {
    resp.setStatus(status);
    ServiceError.Builder errBuilder = ServiceError.newBuilder()
            .setCode(status)
            .setMessage(reasonPhrase + "; " + description);
    if (secCtx.isUserInRole(JaxRsSecurityContext.OPERATOR_ROLE)) {
      errBuilder.setDetail(new DebugDetail("origin", Collections.EMPTY_LIST,
                    exception != null ? Converters.convert(exception) : null, Collections.EMPTY_LIST));
    }
    ServiceError err = errBuilder.build();
    XJsonAvroMessageBodyWriter writer = new XJsonAvroMessageBodyWriter(new DefaultSchemaProtocol(
            SchemaResolver.NONE));
    try {
      MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<String, Object>(2);
      writer.writeTo(err, err.getClass(), err.getClass(),
              Arrays.EMPTY_ANNOT_ARRAY,
              MediaType.APPLICATION_JSON_TYPE, headers,
              new LazyOutputStreamWrapper(new HeaderWriteBeforeOutput(headers, resp)));
    } catch (RuntimeException ex) {
      if (exception != null) {
        Throwables.suppressLimited(ex, exception);
      }
      log.log(java.util.logging.Level.SEVERE, "Exception while writing detail", ex);
      throw ex;
    } catch (IOException ex) {
      if (exception != null) {
        Throwables.suppressLimited(ex, exception);
      }
      log.log(java.util.logging.Level.SEVERE, "Exception while writing detail", ex);
      throw new UncheckedIOException(ex);
    } finally {
      resp.flushBuffer();
    }
  }


  @SuppressFBWarnings("SERVLET_HEADER") // no sec decisions are made with this. (only logged)
  private String getRemoteHost(final HttpServletRequest req) {
      String addr;
      try {
        addr = req.getRemoteAddr();
      } catch (RuntimeException ex2) {
        log.log(java.util.logging.Level.FINE, "Unable to obtain remote address", ex2);
        addr = "Unknown.direct";
      }
      String fwdFor;
      try {
        fwdFor = req.getHeader("x-forwarded-for");
      } catch (RuntimeException ex2) { // java.lang.IllegalStateException:Null request object
        log.log(java.util.logging.Level.FINE, "Unable to obtain header x-forwarded-for", ex2);
        return addr;
      }
      if (fwdFor == null) {
        return addr;
      } else {
        return fwdFor + ',' + addr;
      }
  }

  private static void logContextLogs(final Logger logger, final ExecutionContext ctx,
          final HttpServletRequest req, final Level level) {
    List<Slf4jLogRecord> ctxLogs = new ArrayList<>();
    ctx.streamLogs((log) -> {
      if (!log.isLogged()) {
        ctxLogs.add(log);
      }
    });
    Collections.sort(ctxLogs, Slf4jLogRecord::compareByTimestamp);
    LogAttribute<CharSequence> traceId = LogAttribute.traceId(ctx.getId());
    for (Slf4jLogRecord log : ctxLogs) {
      LogUtils.logUpgrade(logger, org.spf4j.log.Level.INFO, "Detail on {0}", level,
              traceId, log.toLogRecord("", ""));
    }
    logRequestHeaders(req, logger, traceId);
  }

  private  static void logRequestHeaders(final HttpServletRequest req, final Logger logger, final LogAttribute trId) {
    Enumeration<String> names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String headerName = names.nextElement();
      if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
        continue;
      }
      LogUtils.logUpgrade(logger, org.spf4j.log.Level.INFO, "request.header.{0}", headerName,
              Collections.list(req.getHeaders(headerName)), trId);
    }
  }

  private static void logContextProfile(final Logger logger, final long execTimeNanos, final ExecutionContext ctx) {
    StackSamples stackSamples = ctx.getAndClearStackSamples();
    if (stackSamples != null) {
      logger.log(java.util.logging.Level.INFO, "Profile Detail for {0}",
              new Object[]{ctx.getName(), LogAttribute.traceId(ctx.getId()),
                LogAttribute.execTimeMicros(execTimeNanos, TimeUnit.NANOSECONDS),
                LogAttribute.profileSamples(stackSamples)});
    }
  }

  @Override
  public void destroy() {
    // nothing to destroy
  }

  @Override
  public String toString() {
    return "ExecutionContextFilter{" + "deadlineProtocol=" + deadlineProtocol + ", idHeaderName="
            + idHeaderName + ", warnThreshold=" + warnThreshold + ", errorThreshold="
            + errorThreshold + '}';
  }

  @SuppressFBWarnings("DMC_DUBIOUS_MAP_COLLECTION")
  private static class HeaderWriteBeforeOutput implements Supplier<OutputStream> {

    private final MultivaluedHashMap<String, Object> headers;
    private final HttpServletResponse resp;

    HeaderWriteBeforeOutput(final MultivaluedHashMap<String, Object> headers, final HttpServletResponse resp) {
      this.headers = headers;
      this.resp = resp;
    }

    @Override
    @SuppressFBWarnings("HTTP_RESPONSE_SPLITTING")
    public OutputStream get() {
      for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
        String key = entry.getKey();
        for (Object val : entry.getValue()) {
          String strVal = val.toString();
          if (strVal.indexOf('\n') >= 0 || strVal.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("No multiline warning messages supported: " + strVal);
          }
          resp.addHeader(key, strVal);
        }
      }
      try {
        return resp.getOutputStream();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

}
