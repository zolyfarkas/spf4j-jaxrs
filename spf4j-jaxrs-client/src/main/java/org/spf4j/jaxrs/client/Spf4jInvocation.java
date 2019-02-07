package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import static org.spf4j.base.ExecutionContexts.start;
import org.spf4j.base.TimeSource;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.base.Wrapper;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.base.avro.StackSamples;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.http.Headers;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.log.Level;
import org.spf4j.ssdump2.Converter;

/**
 * @author Zoltan Farkas
 */
public final class Spf4jInvocation implements Invocation, Wrapper<Invocation> {

  private static final ExecContextLogger LOG = new ExecContextLogger(LoggerFactory.getLogger(Spf4jInvocation.class));

  private final Invocation invocation;
  private final AsyncRetryExecutor<Object, Callable<? extends Object>> executor;
  private final Spf4jWebTarget target;
  private final long timeoutNanos;
  private final long httpReqTimeoutNanos;
  private final String method;

  public Spf4jInvocation(final Invocation invocation, final long timeoutNanos, final long httpReqTimeoutNanos,
          final AsyncRetryExecutor<Object, Callable<? extends Object>> policy,
          final Spf4jWebTarget target, final String method) {
    this.invocation = invocation;
    this.executor = policy;
    this.timeoutNanos = timeoutNanos;
    this.target = target;
    this.method = method;
    this.httpReqTimeoutNanos = httpReqTimeoutNanos;
  }

  public String getMethod() {
    return method;
  }

  public long getTimeoutNanos() {
    return timeoutNanos;
  }

  public Spf4jWebTarget getTarget() {
    return target;
  }

  public String getName() {
    URI uri = target.getUri();
    return method + '/' + uri.getHost() + ':' + uri.getPort() + uri.getPath();
  }

  @Override
  public Spf4jInvocation property(final String name, final Object value) {
    Invocation invc = invocation.property(name, value);
    if (invc == invocation) {
      return this;
    } else {
      return new Spf4jInvocation(invc, timeoutNanos, httpReqTimeoutNanos, executor, target, method);
    }
  }

  private <T> T invoke(final Callable<T> what) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current, timeoutNanos, TimeUnit.NANOSECONDS);
    try {
      return executor.call(
              propagatingServiceExceptionHandlingCallable(current, what, getName(),
                      deadlineNanos, httpReqTimeoutNanos),
               RuntimeException.class, nanoTime, deadlineNanos);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    }
  }

  private <T> Future<T> submit(final Callable<T> what) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current, timeoutNanos, TimeUnit.NANOSECONDS);
    Callable<T> pc = propagatingServiceExceptionHandlingCallable(current, what, getName(),
            deadlineNanos, httpReqTimeoutNanos);
    return executor.submitRx(pc, nanoTime, deadlineNanos,
            () -> new ContextPropagatingCompletableFuture<>(current, deadlineNanos));
  }

  static <T> Callable<T> propagatingServiceExceptionHandlingCallable(
          final ExecutionContext ctx,
          final Callable<T> callable, @Nullable final String name, final long deadlineNanos,
          final long callableTimeoutNanos) {
    return new PropagatingServiceExceptionHandler(callable, ctx, name, deadlineNanos, callableTimeoutNanos);
  }

  private static final class PropagatingServiceExceptionHandler<T> implements Callable<T>, Wrapper<Callable<T>> {

    private final Callable<T> task;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    private final long callableTimeoutNanos;

    PropagatingServiceExceptionHandler(final Callable<T> task, final ExecutionContext current,
            @Nullable final String name, final long deadlineNanos, final long callableTimeoutNanos) {
      this.task = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
      this.callableTimeoutNanos = callableTimeoutNanos;
    }

    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public T call() throws Exception {
      long aDeadlineNanos;
      if (callableTimeoutNanos < 0) {
        aDeadlineNanos = deadlineNanos;
      } else {
        aDeadlineNanos = TimeSource.getDeadlineNanos(callableTimeoutNanos, TimeUnit.NANOSECONDS);
        if (aDeadlineNanos > deadlineNanos) {
          aDeadlineNanos = deadlineNanos;
        }
      }
      try (ExecutionContext ctx = start(toString(), current, aDeadlineNanos)) {
        return task.call();
      } catch (Exception ex) {
        Throwable rex = com.google.common.base.Throwables.getRootCause(ex);
        if (rex instanceof WebApplicationException) {
          handleServiceError((WebApplicationException) rex, current);
        }
        throw ex;
      }
    }

    @Override
    public String toString() {
      return name == null ? task.toString() : name;
    }

    @Override
    public Callable<T> getWrapped() {
      return task;
    }

  }

  @Override
  public Response invoke() {
    return invoke(invocation::invoke);
  }

  @Override
  public <T> T invoke(final Class<T> responseType) {
    return invoke(() -> invocation.invoke(responseType));
  }

  @Override
  public <T> T invoke(final GenericType<T> responseType) {
    return invoke(() -> invocation.invoke(responseType));
  }

  @Override
  public Future<Response> submit() {
    return submit(invocation::invoke);
  }

  @Override
  public <T> Future<T> submit(final Class<T> responseType) {
    return submit(() -> invocation.invoke(responseType));
  }

  @Override
  public <T> Future<T> submit(final GenericType<T> responseType) {
    return submit(() -> invocation.invoke(responseType));
  }

  @Override
  public <T> Future<T> submit(final InvocationCallback<T> callback) {
    final Type callbackParamType;
    final ReflectionHelper.DeclaringClassInterfacePair pair
            = ReflectionHelper.getClass(callback.getClass(), InvocationCallback.class);
    final Type[] typeArguments = ReflectionHelper.getParameterizedTypeArguments(pair);
    if (typeArguments == null || typeArguments.length == 0) {
      callbackParamType = Object.class;
    } else {
      callbackParamType = typeArguments[0];
    }
    final Class<T> callbackParamClass = ReflectionHelper.erasure(callbackParamType);
    if (Response.class == callbackParamClass) {
      return (Future<T>) submit(() -> {
        try {
          Response resp = invocation.invoke();
          callback.completed((T) resp);
          return resp;
        } catch (Throwable t) {
          callback.failed(t);
          throw t;
        }
      });
    } else {
      return (Future<T>) submit(() -> {
        try {
          T resp = invocation.invoke(new GenericType<>(callbackParamType));
          callback.completed(resp);
          return resp;
        } catch (Throwable t) {
          callback.failed(t);
          throw t;
        }
      });
    }
  }

  private static void handleServiceError(final WebApplicationException ex,
          final ExecutionContext current) {
    Response response = ex.getResponse();
    if (response.getHeaders().getFirst(Headers.CONTENT_SCHEMA) == null) {
      return;
    }
    ServiceError se;
    try {
      se = response.readEntity(ServiceError.class);
    } catch (RuntimeException e) {
      // not a Propagable service error.
      ex.addSuppressed(e);
      return;
    }
    LOG.debug("ServiceError: {}", se.getMessage());
    DebugDetail detail = se.getDetail();
    Throwable rootCause = null;
    if (detail != null) {
      org.spf4j.base.avro.Throwable throwable = detail.getThrowable();
      if (throwable != null) {
        rootCause = Converters.convert(detail.getOrigin(), throwable);
      }
      String origin = detail.getOrigin();
      if (current != null) {
        for (LogRecord log : detail.getLogs()) {
          if (log.getOrigin().isEmpty()) {
            log.setOrigin(origin);
          }
          LOG.log(current, Level.DEBUG, log);
        }
        List<StackSampleElement> stackSamples = detail.getStackSamples();
        if (!stackSamples.isEmpty()) {
          LOG.debug("remoteProfileDetail", new StackSamples(stackSamples));
          current.add(Converter.convert(stackSamples.iterator()));
        }
      }
    }
    WebApplicationException nex = new WebApplicationException(rootCause,
            Response.fromResponse(response).entity(se).build());
    nex.setStackTrace(ex.getStackTrace());
    throw nex;
  }

  @Override
  public Invocation getWrapped() {
    return this.invocation;
  }

  @Override
  public String toString() {
    return "Spf4jInvocation{" + "invocation=" + invocation + ", executor="
            + executor + ", target=" + target + ", timeoutNanos=" + timeoutNanos
            + ", httpReqTimeoutNanos=" + httpReqTimeoutNanos + ", method=" + method + '}';
  }

}
