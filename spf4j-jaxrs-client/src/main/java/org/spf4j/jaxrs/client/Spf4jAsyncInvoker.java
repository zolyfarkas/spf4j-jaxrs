
package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Future;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jAsyncInvoker implements AsyncInvoker {

  private final Spf4jInvocationBuilder invocation;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public Spf4jAsyncInvoker(final Spf4jInvocationBuilder invocation) {
    this.invocation = invocation;
  }

  @Override
  public Future<Response> get() {
    return invocation.buildGet().submit();
  }

  @Override
  public <T> Future<T> get(final Class<T> responseType) {
    return invocation.buildGet().submit(responseType);
  }

  @Override
  public <T> Future<T> get(final GenericType<T> responseType) {
    return invocation.buildGet().submit(responseType);
  }

  @Override
  public <T> Future<T> get(final InvocationCallback<T> callback) {
    return invocation.buildGet().submit(callback);
  }

  @Override
  public Future<Response> put(final Entity<?> entity) {
    return invocation.buildPut(entity).submit();
  }

  @Override
  public <T> Future<T> put(final Entity<?> entity, final Class<T> responseType) {
    return invocation.buildPut(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> put(final Entity<?> entity, final GenericType<T> responseType) {
    return invocation.buildPut(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> put(final Entity<?> entity, final InvocationCallback<T> callback) {
    return invocation.buildPut(entity).submit(callback);
  }

  @Override
  public Future<Response> post(final Entity<?> entity) {
    return invocation.buildPost(entity).submit();
  }

  @Override
  public <T> Future<T> post(final Entity<?> entity, final Class<T> responseType) {
    return invocation.buildPost(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> post(final Entity<?> entity, final GenericType<T> responseType) {
    return invocation.buildPost(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> post(final Entity<?> entity, final InvocationCallback<T> callback) {
    return invocation.buildPost(entity).submit(callback);
  }

  @Override
  public Future<Response> delete() {
    return invocation.buildDelete().submit();
  }

  @Override
  public <T> Future<T> delete(final Class<T> responseType) {
    return invocation.buildDelete().submit(responseType);
  }

  @Override
  public <T> Future<T> delete(final GenericType<T> responseType) {
    return invocation.buildDelete().submit(responseType);
  }

  @Override
  public <T> Future<T> delete(final InvocationCallback<T> callback) {
    return invocation.buildDelete().submit(callback);
  }

  @Override
  public Future<Response> head() {
    return invocation.build(HttpMethod.HEAD).submit();
  }

  @Override
  public Future<Response> head(final InvocationCallback<Response> callback) {
    return invocation.build(HttpMethod.HEAD).submit(callback);
  }

  @Override
  public Future<Response> options() {
    return invocation.build(HttpMethod.OPTIONS).submit();
  }

  @Override
  public <T> Future<T> options(final Class<T> responseType) {
    return invocation.build(HttpMethod.OPTIONS).submit(responseType);
  }

  @Override
  public <T> Future<T> options(final GenericType<T> responseType) {
    return invocation.build(HttpMethod.OPTIONS).submit(responseType);
  }

  @Override
  public <T> Future<T> options(final InvocationCallback<T> callback) {
     return invocation.build(HttpMethod.OPTIONS).submit(callback);
  }

  @Override
  public Future<Response> trace() {
    return invocation.build("TRACE").submit();
  }

  @Override
  public <T> Future<T> trace(final Class<T> responseType) {
    return invocation.build("TRACE").submit(responseType);
  }

  @Override
  public <T> Future<T> trace(final GenericType<T> responseType) {
    return invocation.build("TRACE").submit(responseType);
  }

  @Override
  public <T> Future<T> trace(final InvocationCallback<T> callback) {
    return invocation.build("TRACE").submit(callback);
  }

  @Override
  public Future<Response> method(final String name) {
    return invocation.build(name).submit();
  }

  @Override
  public <T> Future<T> method(final String name, final Class<T> responseType) {
    return invocation.build(name).submit(responseType);
  }

  @Override
  public <T> Future<T> method(final String name, final GenericType<T> responseType) {
    return invocation.build(name).submit(responseType);
  }

  @Override
  public <T> Future<T> method(final String name, final InvocationCallback<T> callback) {
    return invocation.build(name).submit(callback);
  }

  @Override
  public Future<Response> method(final String name, final Entity<?> entity) {
    return invocation.build(name, entity).submit();
  }

  @Override
  public <T> Future<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
    return invocation.build(name, entity).submit(responseType);
  }

  @Override
  public <T> Future<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    return invocation.build(name, entity).submit(responseType);
  }

  @Override
  public <T> Future<T> method(final String name, final Entity<?> entity, final InvocationCallback<T> callback) {
    return invocation.build(name, entity).submit(callback);
  }

  @Override
  public String toString() {
    return "Spf4jAsyncInvoker{" + "invocation=" + invocation + '}';
  }

}
