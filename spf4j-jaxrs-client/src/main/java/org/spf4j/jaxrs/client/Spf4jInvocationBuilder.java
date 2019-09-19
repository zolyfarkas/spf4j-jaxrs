
package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.spf4j.failsafe.AsyncRetryExecutor;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jInvocationBuilder implements Invocation.Builder {

  private final Spf4JClient client;
  private final Invocation.Builder ib;
  private final Spf4jWebTarget target;

  private AsyncRetryExecutor<Object, Callable<? extends Object>> executor;
  private long timeoutNanos;

  private long httpReqTimeoutNanos;

  public Spf4jInvocationBuilder(final Spf4JClient client, final Invocation.Builder ib,
          final AsyncRetryExecutor<Object, Callable<? extends Object>> executor,
          final Spf4jWebTarget target) {
    this.client = client;
    this.ib = ib;
    this.executor = executor;
    this.target = target;
    Number timeout =  (Number) client.getConfiguration().getProperty(Spf4jClientProperties.TIMEOUT_NANOS);
    if (timeout != null) {
      this.timeoutNanos = timeout.longValue();
    } else {
      this.timeoutNanos = Long.getLong(Spf4jClientProperties.TIMEOUT_NANOS, 60000000000L);
    }
    this.httpReqTimeoutNanos = -1L;
  }

  public Spf4jWebTarget getTarget() {
    return target;
  }

  public long getTimeoutNanos() {
    return timeoutNanos;
  }

  public long getHttpReqTimeoutNanos() {
    return httpReqTimeoutNanos;
  }

  public Spf4jInvocationBuilder withRetryRexecutor(final AsyncRetryExecutor<Object, Callable<? extends Object>> exec) {
    this.executor = exec;
    return this;
  }

  public Spf4jInvocationBuilder withTimeout(final long timeout, final TimeUnit tu) {
    timeoutNanos = tu.toNanos(timeout);
    return this;
  }

  public Spf4jInvocationBuilder withHttpReqTimeout(final long timeout, final TimeUnit tu) {
    httpReqTimeoutNanos = tu.toNanos(timeout);
    return this;
  }

  @Override
  public Spf4jInvocation build(final String method) {
    return new Spf4jInvocation(ib.build(method), timeoutNanos, httpReqTimeoutNanos, executor, this.target,
            method);
  }

  @Override
  public Spf4jInvocation build(final String method, final Entity<?> entity) {
    return new  Spf4jInvocation(ib.build(method, entity), timeoutNanos, httpReqTimeoutNanos,
            executor, this.target, method);
  }

  @Override
  public Spf4jInvocation buildGet() {
    return new Spf4jInvocation(ib.buildGet(), timeoutNanos, httpReqTimeoutNanos,
            executor, this.target, HttpMethod.GET);
  }

  @Override
  public Spf4jInvocation buildDelete() {
    return new Spf4jInvocation(ib.buildDelete(), timeoutNanos, httpReqTimeoutNanos,
            executor, this.target, HttpMethod.DELETE);
  }

  @Override
  public Spf4jInvocation buildPost(final Entity<?> entity) {
    return new Spf4jInvocation(ib.buildPost(entity), timeoutNanos, httpReqTimeoutNanos,
            executor, this.target, HttpMethod.POST);
  }

  @Override
  public Spf4jInvocation buildPut(final Entity<?> entity) {
    return new Spf4jInvocation(ib.buildPut(entity), timeoutNanos, httpReqTimeoutNanos,
            executor, this.target, HttpMethod.PUT);
  }

  @Override
  public Spf4jAsyncInvoker async() {
    return new Spf4jAsyncInvoker(this);
  }

  @Override
  public Invocation.Builder accept(final String... mediaTypes) {
    Invocation.Builder builder = ib.accept(mediaTypes);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder accept(final MediaType... mediaTypes) {
    Invocation.Builder builder = ib.accept(mediaTypes);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder acceptLanguage(final Locale... locales) {
    Invocation.Builder builder = ib.acceptLanguage(locales);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder acceptLanguage(final String... locales) {
    Invocation.Builder builder = ib.acceptLanguage(locales);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder acceptEncoding(final String... encodings) {
    Invocation.Builder builder = ib.acceptEncoding(encodings);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder cookie(final Cookie cookie) {
    Invocation.Builder builder = ib.cookie(cookie);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder cookie(final String name, final String value) {
    Invocation.Builder builder = ib.cookie(name, value);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder cacheControl(final CacheControl cacheControl) {
    Invocation.Builder builder = ib.cacheControl(cacheControl);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder header(final String name, final Object value) {
    Invocation.Builder builder = ib.header(name, Spf4JClient.convert(
              Spf4JClient.getParamConverters(this.getTarget().getConfiguration()), value));
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public Invocation.Builder headers(final MultivaluedMap<String, Object> headers) {
    MultivaluedHashMap<String, Object> map = null;
    for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
      List<Object> value = entry.getValue();
      List<Object> cValue = Spf4JClient.convert(
              Spf4JClient.getParamConverters(this.getTarget().getConfiguration()), value);
      if (value != cValue) {
        if (map == null) {
          map = new MultivaluedHashMap<>();
          map.putAll(headers);
        }
        map.put(entry.getKey(), cValue);
      }
    }
    ib.headers(map == null ? headers : map);
    return this;
  }

  @Override
  public Spf4jInvocationBuilder property(final String name, final Object value) {
    Invocation.Builder builder = ib.property(name, value);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
  }

  @Override
  public CompletionStageRxInvoker rx() {
    return new Spf4jCompletionStageRxInvoker(this, executor);
  }

  @Override
  public <T extends RxInvoker> T rx(final Class<T> clazz) {
    return ib.rx(clazz);
  }

  @Override
  public Response get() {
    return buildGet().invoke();
  }

  @Override
  public <T> T get(final Class<T> responseType) {
    return buildGet().invoke(responseType);
  }

  @Override
  public <T> T get(final GenericType<T> responseType) {
    return buildGet().invoke(responseType);
  }

  @Override
  public Response put(final Entity<?> entity) {
     return buildPut(entity).invoke();
  }

  @Override
  public <T> T put(final Entity<?> entity, final Class<T> responseType) {
    return buildPut(entity).invoke(responseType);
  }

  @Override
  public <T> T put(final Entity<?> entity, final GenericType<T> responseType) {
    return buildPut(entity).invoke(responseType);
  }

  @Override
  public Response post(final Entity<?> entity) {
    return buildPost(entity).invoke();
  }

  @Override
  public <T> T post(final Entity<?> entity, final Class<T> responseType) {
    return buildPost(entity).invoke(responseType);
  }

  @Override
  public <T> T post(final Entity<?> entity, final GenericType<T> responseType) {
    return buildPost(entity).invoke(responseType);
  }

  @Override
  public Response delete() {
    return buildDelete().invoke();
  }

  @Override
  public <T> T delete(final Class<T> responseType) {
    return buildDelete().invoke(responseType);
  }

  @Override
  public <T> T delete(final GenericType<T> responseType) {
    return buildDelete().invoke(responseType);
  }

  @Override
  public Response head() {
    return build(HttpMethod.HEAD).invoke();
  }

  @Override
  public Response options() {
    return build(HttpMethod.OPTIONS).invoke();
  }

  @Override
  public <T> T options(final Class<T> responseType) {
    return build(HttpMethod.OPTIONS).invoke(responseType);
  }

  @Override
  public <T> T options(final GenericType<T> responseType) {
    return build(HttpMethod.OPTIONS).invoke(responseType);
  }

  @Override
  public Response trace() {
    return build("TRACE").invoke();
  }

  @Override
  public <T> T trace(final Class<T> responseType) {
    return build("TRACE").invoke(responseType);
  }

  @Override
  public <T> T trace(final GenericType<T> responseType) {
    return build("TRACE").invoke(responseType);
  }

  @Override
  public Response method(final String name) {
    return build(name).invoke();
  }

  @Override
  public <T> T method(final String name, final Class<T> responseType) {
    return build(name).invoke(responseType);
  }

  @Override
  public <T> T method(final String name, final GenericType<T> responseType) {
    return build(name).invoke(responseType);
  }

  @Override
  public Response method(final String name, final Entity<?> entity) {
    return build(name, entity).invoke();
  }

  @Override
  public <T> T method(final String name, final Entity<?> entity, final Class<T> responseType) {
    return build(name, entity).invoke(responseType);
  }

  @Override
  public <T> T method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    return build(name, entity).invoke(responseType);
  }

  @Override
  public String toString() {
    return "Spf4jInvocationBuilder{" + "client=" + client + ", ib=" + ib + ", target="
            + target + ", executor=" + executor + ", timeoutNanos=" + timeoutNanos
            + ", httpReqTimeoutNanos=" + httpReqTimeoutNanos + '}';
  }



}
