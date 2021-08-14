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
import org.spf4j.failsafe.concurrent.FailSafeExecutor;
import org.spf4j.service.avro.HttpExecutionPolicy;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jCompletionStageRxInvoker
        implements CompletionStageRxInvoker {

  private final Spf4jInvocationBuilder invocationBuilder;
  private final FailSafeExecutor executor;

  public Spf4jCompletionStageRxInvoker(final Spf4jInvocationBuilder invocationBuilder,
          final FailSafeExecutor executor) {
    this.invocationBuilder = invocationBuilder;
    this.executor = executor;
  }

  private String getName(final String method) {
    URI uri = invocationBuilder.getTarget().getUri();
    return method + '/' + uri.getHost() + ':' + uri.getPort() + uri.getPath();
  }


  private <T> CompletionStage<T> submit(final Callable<T> what, final String name, final String method) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    HttpExecutionPolicy execPolicy = invocationBuilder.getExecPolicy(method);
    long deadlineNanos = ExecutionContexts.computeDeadline(current,
            execPolicy.getOverallTimeout().toNanos(), TimeUnit.NANOSECONDS);
    Spf4jWebTarget target = invocationBuilder.getTarget();
    HttpCallable<T> pc = HttpCallable.invocationHandler(current, what, name,
            target.getUri(),
            method,
            target.getClient().getExceptionMapper(),
            deadlineNanos, execPolicy.getAttemptTimeout().toNanos());
    return invocationBuilder.buildExecutor(execPolicy, executor).submitRx(pc, nanoTime, deadlineNanos,
            () -> new ContextPropagatingCompletableFuture<>(current, deadlineNanos));
  }

  @Override
  public CompletionStage<Response> get() {
    return submit(invocationBuilder.buildGet().getWrapped()::invoke, getName(HttpMethod.GET),
            HttpMethod.GET);
  }

  @Override
  public <T> CompletionStage<T> get(final Class<T> responseType) {
    return submit(() ->  {
      return invocationBuilder.buildGet().getWrapped().invoke(responseType);
    }, getName(HttpMethod.GET), HttpMethod.GET);
  }

  @Override
  public <T> CompletionStage<T> get(final GenericType<T> responseType) {
     return submit(() -> invocationBuilder.buildGet().getWrapped().invoke(responseType),
             getName(HttpMethod.GET), HttpMethod.GET);
  }

  @Override
  public CompletionStage<Response> put(final Entity<?> entity) {
    return submit(() -> invocationBuilder.buildPut(entity).getWrapped().invoke(),
            getName(HttpMethod.PUT), HttpMethod.PUT);
  }

  @Override
  public <T> CompletionStage<T> put(final Entity<?> entity, final Class<T> clazz) {
    return submit(() -> invocationBuilder.buildPut(entity).getWrapped().invoke(clazz),
            getName(HttpMethod.PUT), HttpMethod.PUT);
  }

  @Override
  public <T> CompletionStage<T> put(final Entity<?> entity, final GenericType<T> type) {
    return submit(() -> invocationBuilder.buildPut(entity).getWrapped().invoke(type),
            getName(HttpMethod.PUT), HttpMethod.PUT);
  }

  @Override
  public CompletionStage<Response> post(final Entity<?> entity) {
    return submit(() -> invocationBuilder.buildPost(entity).getWrapped().invoke(),
            getName(HttpMethod.POST), HttpMethod.POST);
  }

  @Override
  public <T> CompletionStage<T> post(final Entity<?> entity, final Class<T> clazz) {
    return submit(() -> invocationBuilder.buildPost(entity).getWrapped().invoke(clazz),
            getName(HttpMethod.POST), HttpMethod.POST);
  }

  @Override
  public <T> CompletionStage<T> post(final Entity<?> entity, final GenericType<T> type) {
    return submit(() -> invocationBuilder.buildPost(entity).getWrapped().invoke(type),
            getName(HttpMethod.POST), HttpMethod.POST);
  }

  @Override
  public CompletionStage<Response> delete() {
    return submit(() -> invocationBuilder.buildDelete().getWrapped().invoke(),
            getName(HttpMethod.DELETE), HttpMethod.DELETE);
  }

  @Override
  public <T> CompletionStage<T> delete(final Class<T> responseType) {
    return submit(() -> invocationBuilder.buildDelete().getWrapped().invoke(responseType),
            getName(HttpMethod.DELETE), HttpMethod.DELETE);
  }

  @Override
  public <T> CompletionStage<T> delete(final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.buildDelete().getWrapped().invoke(responseType),
            getName(HttpMethod.DELETE), HttpMethod.DELETE);
  }

  @Override
  public CompletionStage<Response> head() {
    return submit(() -> invocationBuilder.build(HttpMethod.HEAD).getWrapped().invoke(),
            getName(HttpMethod.HEAD), HttpMethod.HEAD);
  }

  @Override
  public CompletionStage<Response> options() {
    return submit(() -> invocationBuilder.build(HttpMethod.OPTIONS).getWrapped().invoke(),
            getName(HttpMethod.OPTIONS), HttpMethod.OPTIONS);
  }

  @Override
  public <T> CompletionStage<T> options(final Class<T> responseType) {
    return submit(() -> invocationBuilder.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            getName(HttpMethod.OPTIONS), HttpMethod.OPTIONS);
  }

  @Override
  public <T> CompletionStage<T> options(final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            getName(HttpMethod.OPTIONS), HttpMethod.OPTIONS);
  }

  @Override
  public CompletionStage<Response> trace() {
    return submit(() -> invocationBuilder.build("TRACE").getWrapped().invoke(), getName("TRACE"),
           "TRACE");
  }

  @Override
  public <T> CompletionStage<T> trace(final Class<T> responseType) {
    return submit(() -> invocationBuilder.build("TRACE").getWrapped().invoke(responseType),
            getName("TRACE"), "TRACE");
  }

  @Override
  public <T> CompletionStage<T> trace(final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build("TRACE").getWrapped().invoke(responseType),
            getName("TRACE"), "TRACE");
  }

  @Override
  public CompletionStage<Response> method(final String name) {
    return submit(() -> invocationBuilder.build(name).getWrapped().invoke(), getName(name), name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Class<T> responseType) {
    return submit(() -> invocationBuilder.build(name).getWrapped().invoke(responseType),
            getName(name), name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build(name).getWrapped().invoke(responseType),
            getName(name), name);
  }

  @Override
  public CompletionStage<Response> method(final String name, final Entity<?> entity) {
     return submit(() -> invocationBuilder.build(name, entity).getWrapped().invoke(),
             getName(name), name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
    return submit(() -> invocationBuilder.build(name, entity).getWrapped().invoke(responseType),
            getName(name), name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build(name, entity).getWrapped().invoke(responseType),
            getName(name), name);
  }

  @Override
  public String toString() {
    return "Spf4jCompletionStageRxInvoker{" + "invocation=" + invocationBuilder + ", executor=" + executor + '}';
  }

}
