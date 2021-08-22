package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.spf4j.failsafe.concurrent.FailSafeExecutor;

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


  private <T> CompletionStage<T> submit(final Callable<T> what, final String method) {
    return invocationBuilder.build(method).submit(what);
  }

  @Override
  public CompletionStage<Response> get() {
    return submit(invocationBuilder.buildGet().getWrapped()::invoke,
            HttpMethod.GET);
  }

  @Override
  public <T> CompletionStage<T> get(final Class<T> responseType) {
    return submit(() ->  {
      return invocationBuilder.buildGet().getWrapped().invoke(responseType);
    }, HttpMethod.GET);
  }

  @Override
  public <T> CompletionStage<T> get(final GenericType<T> responseType) {
     return submit(() -> invocationBuilder.buildGet().getWrapped().invoke(responseType),
              HttpMethod.GET);
  }

  @Override
  public CompletionStage<Response> put(final Entity<?> entity) {
    return submit(() -> invocationBuilder.buildPut(entity).getWrapped().invoke(),
             HttpMethod.PUT);
  }

  @Override
  public <T> CompletionStage<T> put(final Entity<?> entity, final Class<T> clazz) {
    return submit(() -> invocationBuilder.buildPut(entity).getWrapped().invoke(clazz),
             HttpMethod.PUT);
  }

  @Override
  public <T> CompletionStage<T> put(final Entity<?> entity, final GenericType<T> type) {
    return submit(() -> invocationBuilder.buildPut(entity).getWrapped().invoke(type),
             HttpMethod.PUT);
  }

  @Override
  public CompletionStage<Response> post(final Entity<?> entity) {
    return submit(() -> invocationBuilder.buildPost(entity).getWrapped().invoke(),
             HttpMethod.POST);
  }

  @Override
  public <T> CompletionStage<T> post(final Entity<?> entity, final Class<T> clazz) {
    return submit(() -> invocationBuilder.buildPost(entity).getWrapped().invoke(clazz),
             HttpMethod.POST);
  }

  @Override
  public <T> CompletionStage<T> post(final Entity<?> entity, final GenericType<T> type) {
    return submit(() -> invocationBuilder.buildPost(entity).getWrapped().invoke(type),
             HttpMethod.POST);
  }

  @Override
  public CompletionStage<Response> delete() {
    return submit(() -> invocationBuilder.buildDelete().getWrapped().invoke(),
            HttpMethod.DELETE);
  }

  @Override
  public <T> CompletionStage<T> delete(final Class<T> responseType) {
    return submit(() -> invocationBuilder.buildDelete().getWrapped().invoke(responseType),
             HttpMethod.DELETE);
  }

  @Override
  public <T> CompletionStage<T> delete(final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.buildDelete().getWrapped().invoke(responseType),
            HttpMethod.DELETE);
  }

  @Override
  public CompletionStage<Response> head() {
    return submit(() -> invocationBuilder.build(HttpMethod.HEAD).getWrapped().invoke(),
             HttpMethod.HEAD);
  }

  @Override
  public CompletionStage<Response> options() {
    return submit(() -> invocationBuilder.build(HttpMethod.OPTIONS).getWrapped().invoke(),
            HttpMethod.OPTIONS);
  }

  @Override
  public <T> CompletionStage<T> options(final Class<T> responseType) {
    return submit(() -> invocationBuilder.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
             HttpMethod.OPTIONS);
  }

  @Override
  public <T> CompletionStage<T> options(final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            HttpMethod.OPTIONS);
  }

  @Override
  public CompletionStage<Response> trace() {
    return submit(() -> invocationBuilder.build("TRACE").getWrapped().invoke(),
           "TRACE");
  }

  @Override
  public <T> CompletionStage<T> trace(final Class<T> responseType) {
    return submit(() -> invocationBuilder.build("TRACE").getWrapped().invoke(responseType),
            "TRACE");
  }

  @Override
  public <T> CompletionStage<T> trace(final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build("TRACE").getWrapped().invoke(responseType),
            "TRACE");
  }

  @Override
  public CompletionStage<Response> method(final String name) {
    return submit(() -> invocationBuilder.build(name).getWrapped().invoke(), name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Class<T> responseType) {
    return submit(() -> invocationBuilder.build(name).getWrapped().invoke(responseType),
             name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build(name).getWrapped().invoke(responseType),
             name);
  }

  @Override
  public CompletionStage<Response> method(final String name, final Entity<?> entity) {
     return submit(() -> invocationBuilder.build(name, entity).getWrapped().invoke(),
              name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
    return submit(() -> invocationBuilder.build(name, entity).getWrapped().invoke(responseType),
             name);
  }

  @Override
  public <T> CompletionStage<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    return submit(() -> invocationBuilder.build(name, entity).getWrapped().invoke(responseType),
             name);
  }

  @Override
  public String toString() {
    return "Spf4jCompletionStageRxInvoker{" + "invocation=" + invocationBuilder + ", executor=" + executor + '}';
  }

}
