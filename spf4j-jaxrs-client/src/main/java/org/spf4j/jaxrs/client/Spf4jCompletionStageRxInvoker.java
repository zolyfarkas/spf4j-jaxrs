package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.failsafe.AsyncRetryExecutor;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jCompletionStageRxInvoker
        implements CompletionStageRxInvoker {

  private final Spf4jInvocationBuilder invocation;
  private final AsyncRetryExecutor<Object, Callable<? extends Object>> executor;

  public Spf4jCompletionStageRxInvoker(final Spf4jInvocationBuilder invocation,
          final AsyncRetryExecutor<Object, Callable<? extends Object>> executor) {
    this.invocation = invocation;
    this.executor = executor;
  }

  private String getName(final String method) {
    URI uri = invocation.getTarget().getUri();
    return method + '/' + uri.getHost() + ':' + uri.getPort() + uri.getPath();
  }

  private <T> CompletionStage<T> submit(final Callable<T> what, final String name) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current, invocation.getTimeoutNanos(), TimeUnit.NANOSECONDS);
    Callable<T> pc = Spf4jInvocation.invocationHandler(current, what, name,
            deadlineNanos, invocation.getHttpReqTimeoutNanos());
    return executor.submitRx(pc, nanoTime, deadlineNanos,
            () -> new ContextPropagatingCompletableFuture<>(current, deadlineNanos));
  }

  @Override
  public CompletionStage<Response> get() {
    return submit(invocation.buildGet().getWrapped()::invoke, getName(HttpMethod.GET));
  }

  @Override
  public <T> CompletionStage<T> get(final Class<T> responseType) {
    return submit(() ->  {
      return invocation.buildGet().getWrapped().invoke(responseType);
    }, getName(HttpMethod.GET));
  }

  @Override
  public <T> CompletionStage<T> get(final GenericType<T> responseType) {
     return submit(() -> invocation.buildGet().getWrapped().invoke(responseType), getName(HttpMethod.GET));
  }

  @Override
  public CompletionStage<Response> put(final Entity<?> entity) {
    return submit(() -> invocation.buildPut(entity).getWrapped().invoke(), getName(HttpMethod.PUT));
  }

  @Override
  public <T> CompletionStage<T> put(final Entity<?> entity, final Class<T> clazz) {
    return submit(() -> invocation.buildPut(entity).getWrapped().invoke(clazz), getName(HttpMethod.PUT));
  }

  @Override
  public <T> CompletionStage<T> put(final Entity<?> entity, final GenericType<T> type) {
    return submit(() -> invocation.buildPut(entity).getWrapped().invoke(type), getName(HttpMethod.PUT));
  }

  @Override
  public CompletionStage<Response> post(final Entity<?> entity) {
    return submit(() -> invocation.buildPost(entity).getWrapped().invoke(), getName(HttpMethod.POST));
  }

  @Override
  public <T> CompletionStage<T> post(final Entity<?> entity, final Class<T> clazz) {
    return submit(() -> invocation.buildPost(entity).getWrapped().invoke(clazz), getName(HttpMethod.POST));
  }

  @Override
  public <T> CompletionStage<T> post(final Entity<?> entity, final GenericType<T> type) {
    return submit(() -> invocation.buildPost(entity).getWrapped().invoke(type), getName(HttpMethod.POST));
  }

  @Override
  public CompletionStage<Response> delete() {
    return submit(() -> invocation.buildDelete().getWrapped().invoke(), getName(HttpMethod.DELETE));
  }

  @Override
  public <T> CompletionStage<T> delete(final Class<T> responseType) {
    return submit(() -> invocation.buildDelete().getWrapped().invoke(responseType), getName(HttpMethod.DELETE));
  }

  @Override
  public <T> CompletionStage<T> delete(final GenericType<T> responseType) {
    return submit(() -> invocation.buildDelete().getWrapped().invoke(responseType), getName(HttpMethod.DELETE));
  }

  @Override
  public CompletionStage<Response> head() {
    return submit(() -> invocation.build(HttpMethod.HEAD).getWrapped().invoke(), getName(HttpMethod.HEAD));
  }

  @Override
  public CompletionStage<Response> options() {
    return submit(() -> invocation.build(HttpMethod.OPTIONS).getWrapped().invoke(), getName(HttpMethod.OPTIONS));
  }

  @Override
  public <T> CompletionStage<T> options(final Class<T> responseType) {
    return submit(() -> invocation.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            getName(HttpMethod.OPTIONS));
  }

  @Override
  public <T> CompletionStage<T> options(final GenericType<T> responseType) {
    return submit(() -> invocation.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            getName(HttpMethod.OPTIONS));
  }

  @Override
  public CompletionStage<Response> trace() {
    return submit(() -> invocation.build("TRACE").getWrapped().invoke(), getName("TRACE"));
  }

  @Override
  public <T> CompletionStage<T> trace(final Class<T> responseType) {
    return submit(() -> invocation.build("TRACE").getWrapped().invoke(responseType), getName("TRACE"));
  }

  @Override
  public <T> CompletionStage<T> trace(final GenericType<T> responseType) {
    return submit(() -> invocation.build("TRACE").getWrapped().invoke(responseType), getName("TRACE"));
  }

  @Override
  public CompletionStage<Response> method(final String name) {
    return submit(() -> invocation.build(name).getWrapped().invoke(), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Class<T> responseType) {
    return submit(() -> invocation.build(name).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final GenericType<T> responseType) {
    return submit(() -> invocation.build(name).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public CompletionStage<Response> method(final String name, final Entity<?> entity) {
     return submit(() -> invocation.build(name, entity).getWrapped().invoke(), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
    return submit(() -> invocation.build(name, entity).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    return submit(() -> invocation.build(name, entity).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public String toString() {
    return "Spf4jCompletionStageRxInvoker{" + "invocation=" + invocation + ", executor=" + executor + '}';
  }

}
