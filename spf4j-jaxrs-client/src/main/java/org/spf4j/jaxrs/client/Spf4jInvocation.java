package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.base.Wrapper;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.service.avro.DestinationTraffic;
import org.spf4j.service.avro.HttpExecutionPolicy;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jInvocation implements Invocation, Wrapper<Invocation> {


  private final Invocation invocation;
  private final AsyncRetryExecutor<Object, HttpCallable<?>> aexecutor;
  private final Spf4jWebTarget target;
  private final HttpExecutionPolicy execPolicy;
  private final String method;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public Spf4jInvocation(final Invocation invocation, final HttpExecutionPolicy execPolicy,
          final AsyncRetryExecutor<Object, HttpCallable<?>> aexecutor,
          final Spf4jWebTarget target, final String method) {
    this.invocation = invocation;
    this.target = target;
    this.method = method;
    this.execPolicy = execPolicy;
    this.aexecutor = aexecutor;
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
      return new Spf4jInvocation(invc, execPolicy, aexecutor, target, method);
    }
  }

  private static class HttpInvocations<T> {
    private final HttpCallable<T> primaryCall;
    private final List<HttpCallable<T>> backgroundCalls;

    HttpInvocations(final HttpCallable<T> primaryCall, final List<HttpCallable<T>> backgroundCalls) {
      this.primaryCall = primaryCall;
      this.backgroundCalls = backgroundCalls;
    }

    public HttpCallable<T> getPrimaryCall() {
      return primaryCall;
    }

    public List<HttpCallable<T>> getBackgroundCalls() {
      return backgroundCalls;
    }


  }

  @SuppressFBWarnings({ "PREDICTABLE_RANDOM", "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS" })
  <T> HttpInvocations<T> createCall(final ExecutionContext current, final Callable<T> what) {
    if (execPolicy.getCircuitBreaker()) {
      throw new ServiceUnavailableException("Circuit breaker active: " + getName());
    }
    long nanoTime = TimeSource.nanoTime();
    long deadlineNanos = ExecutionContexts.computeDeadline(current,
            execPolicy.getOverallTimeout().toNanos(), TimeUnit.NANOSECONDS);
    URI uri = this.target.getUri();
    List<DestinationTraffic> split = execPolicy.getSplitTraffic();
    double rnd = -1d;
    if (!split.isEmpty()) {
      rnd = ThreadLocalRandom.current().nextDouble();
      for (DestinationTraffic destination : split) {
        if (rnd < destination.getRatio()) {
          uri = UriBuilder.fromUri(uri).host(destination.getDestination()).build();
          break;
        }
      }
    }
    HttpCallable<T> primary = HttpCallable.invocationHandler(current, what, getName(),
            uri,
            this.method,
            this.target.getClient().getExceptionMapper(),
            nanoTime,
            deadlineNanos, execPolicy.getAttemptTimeout().toNanos());
    List<DestinationTraffic> shadow =  execPolicy.getShadowTraffic();
    if (shadow.isEmpty()) {
      return new HttpInvocations(primary, Collections.EMPTY_LIST);
    } else {
      if (rnd < 0) {
        rnd = ThreadLocalRandom.current().nextDouble();
      }
      List<HttpCallable<T>> shadowInvocations = new ArrayList<>(shadow.size());
      for (DestinationTraffic destination : shadow) {
        if (rnd < destination.getRatio()) {
          HttpCallable<T> shCall = HttpCallable.invocationHandler(current, what, getName(),
                  UriBuilder.fromUri(uri).host(destination.getDestination()).build(),
                  this.method,
                  this.target.getClient().getExceptionMapper(),
                  nanoTime,
                  deadlineNanos, execPolicy.getAttemptTimeout().toNanos());
          shadowInvocations.add(shCall);
        }
      }
      return new HttpInvocations<>(primary, shadowInvocations);
    }
  }

  private <T> T invoke(final Callable<T> what) {
    ExecutionContext current = ExecutionContexts.current();
    HttpInvocations<T> hc = createCall(current, what);
    HttpCallable<T> pc = hc.getPrimaryCall();
    for (HttpCallable<T> call : hc.getBackgroundCalls()) {
      submit(current, call);
    }
    try {
      return aexecutor.call(pc,
               RuntimeException.class, pc.getStartNanos(), pc.getDeadlineNanos());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    }
  }

  <T> CompletableFuture<T> submit(final Callable<T> what) {
    ExecutionContext current = ExecutionContexts.current();
    HttpInvocations<T> hc = createCall(current, what);
    HttpCallable<T> pc = hc.getPrimaryCall();
    for (HttpCallable<T> call : hc.getBackgroundCalls()) {
      submit(current, call);
    }
    return submit(current, pc);
  }

  private <T> CompletableFuture<T> submit(final ExecutionContext current, final HttpCallable<T> pc) {
    return aexecutor.submitRx(pc, pc.getStartNanos(), pc.getDeadlineNanos(),
            () -> new ContextPropagatingCompletableFuture<>(current, pc.getDeadlineNanos()));
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
    if (execPolicy.getCircuitBreaker()) {
      throw new ServiceUnavailableException("Circuit breaker active: " + getName());
    }
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
    return "Spf4jInvocation{" + "invocation=" + invocation + ", aexecutor="
            + aexecutor + ", target=" + target + ", execPOlicy=" + this.execPolicy
            + ", method=" + method + '}';
  }

}
