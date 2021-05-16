package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.base.Wrapper;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.service.avro.HttpExecutionPolicy;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jInvocation implements Invocation, Wrapper<Invocation> {


  private final Invocation invocation;
  private final AsyncRetryExecutor<Object, HttpCallable<?>> executor;
  private final Spf4jWebTarget target;
  private final HttpExecutionPolicy execPolicy;
  private final String method;

  public Spf4jInvocation(final Invocation invocation, final HttpExecutionPolicy execPolicy,
          final AsyncRetryExecutor<Object, HttpCallable<?>> policy,
          final Spf4jWebTarget target, final String method) {
    this.invocation = invocation;
    this.executor = policy;
    this.target = target;
    this.method = method;
    this.execPolicy = execPolicy;
  }


  public String getMethod() {
    return method;
  }

  public long getTimeoutNanos() {
    return execPolicy.getOverallTimeout().toNanos();
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
      return new Spf4jInvocation(invc, execPolicy, executor, target, method);
    }
  }

  private <T> T invoke(final Callable<T> what) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current,
            execPolicy.getOverallTimeout().toNanos(), TimeUnit.NANOSECONDS);
    try {
      return executor.call(HttpCallable.invocationHandler(current, what, getName(),
                      this.target.getUri(),
                      this.method,
                      this.target.getClient().getExceptionMapper(),
                      deadlineNanos, execPolicy.getAttemptTimeout().toNanos()),
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
    long deadlineNanos = ExecutionContexts.computeDeadline(current,
            execPolicy.getOverallTimeout().toNanos(), TimeUnit.NANOSECONDS);
    HttpCallable pc = HttpCallable.invocationHandler(current, what, getName(),
            this.target.getUri(),
            this.method,
            this.target.getClient().getExceptionMapper(),
            deadlineNanos, execPolicy.getAttemptTimeout().toNanos());
    return executor.submitRx(pc, nanoTime, deadlineNanos,
            () -> new ContextPropagatingCompletableFuture<>(current, deadlineNanos));
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



  @Override
  public Invocation getWrapped() {
    return this.invocation;
  }

  @Override
  public String toString() {
    return "Spf4jInvocation{" + "invocation=" + invocation + ", executor="
            + executor + ", target=" + target + ", execPOlicy=" + this.execPolicy
            + ", method=" + method + '}';
  }

}
