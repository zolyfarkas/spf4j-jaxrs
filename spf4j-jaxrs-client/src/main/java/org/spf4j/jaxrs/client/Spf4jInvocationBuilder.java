package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.failsafe.InvalidRetryPolicyException;
import org.spf4j.failsafe.RetryPolicies;
import org.spf4j.failsafe.TimeoutRelativeHedge;
import org.spf4j.failsafe.avro.RetryPolicy;
import org.spf4j.failsafe.avro.TimeoutRelativeHedgePolicy;
import org.spf4j.failsafe.concurrent.FailSafeExecutor;
import org.spf4j.jaxrs.Utils;
import org.spf4j.service.avro.HttpExecutionPolicy;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4jInvocationBuilder implements Invocation.Builder {


  private static final String UTF8 = StandardCharsets.UTF_8.name();
  private final Spf4JClient client;
  private final Invocation.Builder ib;
  private final Spf4jWebTarget target;

  private FailSafeExecutor executor;

  private final HttpExecutionPolicy.Builder execPolicyBuilder;

  public Spf4jInvocationBuilder(final Spf4JClient client, final Invocation.Builder ib,
          final FailSafeExecutor executor,
          final Spf4jWebTarget target) {
    this.client = client;
    this.ib = ib;
    this.executor = executor;
    this.target = target;
    Number timeout = (Number) client.getConfiguration().getProperty(Spf4jClientProperties.TIMEOUT_NANOS);
    this.execPolicyBuilder = HttpExecutionPolicy.newBuilder();
    if (timeout != null) {
      Duration overallTimeout = Duration.ofNanos(timeout.longValue());
      this.execPolicyBuilder.setOverallTimeout(overallTimeout);
      this.execPolicyBuilder.setAttemptTimeout(overallTimeout);
    } else {
      Duration defaultOverallTimeout = Duration.ofNanos(
              Long.getLong(Spf4jClientProperties.TIMEOUT_NANOS, 30000000000L));
      this.execPolicyBuilder.setOverallTimeout(defaultOverallTimeout);
      this.execPolicyBuilder.setAttemptTimeout(defaultOverallTimeout);
    }
  }

  public Spf4jWebTarget getTarget() {
    return target;
  }

  public static Map<String, List<String>> getQuery(final URI uri) {
    String rawQuery = uri.getRawQuery();
    if (rawQuery == null) {
      return Collections.emptyMap();
    }
    final Map<String, List<String>> queryPairs = new LinkedHashMap<>();
    final String[] pairs = rawQuery.split("&");
    for (String pair : pairs) {
      final int idx = pair.indexOf('=');
      final String key;
      if (idx >= 0) {
        try {
          key = URLDecoder.decode(pair.substring(0, idx), UTF8);
          List<String> values = queryPairs.computeIfAbsent(key, k -> new ArrayList<>(2));
          values.add(URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException ex) {
          throw new RuntimeException(ex);
        }
      } else {
        key = pair;
        queryPairs.computeIfAbsent(key, k -> new ArrayList<>(2));
      }
    }
    return queryPairs;
  }

  HttpExecutionPolicy getExecPolicy(final String method) {
    URI uri = target.getUri();
    HttpExecutionPolicy execPolicy = this.client.getEndpointConfig().getHttpExecutionPolicy(uri.getHost(),
            uri.getPort(), uri.getPath(), method, getQuery(uri), Collections.EMPTY_MAP);
    HttpExecutionPolicy.Builder builder;
    if (execPolicy != null) {
      builder = HttpExecutionPolicy.newBuilder(execPolicy);
    } else {
      builder = HttpExecutionPolicy.newBuilder();
    }
    if (this.execPolicyBuilder.hasAttemptTimeout()) {
      builder.setAttemptTimeout(this.execPolicyBuilder.getAttemptTimeout());
    }
    if (this.execPolicyBuilder.hasCircuitBreaker()) {
      builder.setCircuitBreaker(this.execPolicyBuilder.getCircuitBreaker());
    }
    if (this.execPolicyBuilder.hasConnectTimeout()) {
      builder.setConnectTimeout(this.execPolicyBuilder.getConnectTimeout());
    }
    if (this.execPolicyBuilder.hasHedgePolicy()) {
      builder.setHedgePolicy(this.execPolicyBuilder.getHedgePolicy());
    }
    if (this.execPolicyBuilder.hasOverallTimeout()) {
      builder.setOverallTimeout(this.execPolicyBuilder.getOverallTimeout());
    }
    if (this.execPolicyBuilder.hasPoolMaxIdle()) {
      builder.setPoolMaxIdle(this.execPolicyBuilder.getPoolMaxIdle());
    }
    if (this.execPolicyBuilder.hasRetryPolicy()) {
      builder.setRetryPolicy(this.execPolicyBuilder.getRetryPolicy());
    }
    if (this.execPolicyBuilder.hasShadowTraffic()) {
      builder.setShadowTraffic(this.execPolicyBuilder.getShadowTraffic());
    }
    if (this.execPolicyBuilder.hasSplitTraffic()) {
      builder.setSplitTraffic(this.execPolicyBuilder.getSplitTraffic());
    }
    return builder.build();
  }

  public Spf4jInvocationBuilder withRetryRexecutor(final FailSafeExecutor exec) {
    this.executor = exec;
    return this;
  }

  public Spf4jInvocationBuilder withTimeout(final long timeout, final TimeUnit tu) {
    this.execPolicyBuilder.setOverallTimeout(Duration.ofNanos(tu.toNanos(timeout)));
    return this;
  }

  public Spf4jInvocationBuilder withHttpReqTimeout(final long timeout, final TimeUnit tu) {
    this.execPolicyBuilder.setAttemptTimeout(Duration.ofNanos(tu.toNanos(timeout)));
    return this;
  }

  public Spf4jInvocationBuilder withHedgePolicy(final TimeoutRelativeHedgePolicy hedgePolicy) {
    this.execPolicyBuilder.setHedgePolicy(hedgePolicy);
    return this;
  }

  public Spf4jInvocationBuilder withRetryPolicy(final RetryPolicy policy) {
    this.execPolicyBuilder.setRetryPolicy(policy);
    return this;
  }

  public AsyncRetryExecutor<Object, HttpCallable<?>> buildExecutor(final HttpExecutionPolicy policy,
          final FailSafeExecutor exec) {
    org.spf4j.failsafe.RetryPolicy.Builder<Object, HttpCallable<?>> builder
            = org.spf4j.failsafe.RetryPolicy.newBuilder();
    RetryPolicy retryPolicy = policy.getRetryPolicy();
    if (retryPolicy != null) {
      try {
        RetryPolicies.addRetryPolicy(builder, retryPolicy,
                client.getEndpointConfig().toResultMatcherSupplier());
      } catch (InvalidRetryPolicyException ex) {
        Logger log = Logger.getLogger(Spf4jInvocation.class.getName());
        log.log(Level.WARNING, "Unable to set exec policy {0}", new Object[]{policy, ex});
      }
    }
    Utils.addDefaultRetryPredicated(builder);
    TimeoutRelativeHedgePolicy trp = policy.getHedgePolicy();
    return org.spf4j.failsafe.RetryPolicy.async(c -> builder.build(),
           trp == null ? c -> HedgePolicy.NONE : c -> new TimeoutRelativeHedge(trp), exec);
  }

  @Override
  public Spf4jInvocation build(final String method) {
    HttpExecutionPolicy execPolicy = getExecPolicy(method);
    return new Spf4jInvocation(ib.build(method), execPolicy, buildExecutor(execPolicy, executor), this.target,
            method);
  }

  @Override
  public Spf4jInvocation build(final String method, final Entity<?> entity) {
    HttpExecutionPolicy execPolicy = getExecPolicy(method);
    return new Spf4jInvocation(ib.build(method, entity), execPolicy,
            buildExecutor(execPolicy, executor), this.target, method);
  }

  @Override
  public Spf4jInvocation buildGet() {
    HttpExecutionPolicy execPolicy = getExecPolicy(HttpMethod.GET);
    return new Spf4jInvocation(ib.buildGet(), execPolicy,
            buildExecutor(execPolicy, executor), this.target, HttpMethod.GET);
  }

  @Override
  public Spf4jInvocation buildDelete() {
    HttpExecutionPolicy execPolicy = getExecPolicy(HttpMethod.DELETE);
    return new Spf4jInvocation(ib.buildDelete(), execPolicy,
            buildExecutor(execPolicy, executor), this.target, HttpMethod.DELETE);
  }

  @Override
  public Spf4jInvocation buildPost(final Entity<?> entity) {
    HttpExecutionPolicy execPolicy = getExecPolicy(HttpMethod.POST);
    return new Spf4jInvocation(ib.buildPost(entity), execPolicy,
            buildExecutor(execPolicy, executor), this.target, HttpMethod.POST);
  }

  @Override
  public Spf4jInvocation buildPut(final Entity<?> entity) {
    HttpExecutionPolicy execPolicy = getExecPolicy(HttpMethod.PUT);
    return new Spf4jInvocation(ib.buildPut(entity), execPolicy,
            buildExecutor(execPolicy, executor), this.target, HttpMethod.PUT);
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
    Invocation.Builder builder = ib.headers(map == null ? headers : map);
    if (builder == ib) {
      return this;
    } else {
      return new Spf4jInvocationBuilder(client, builder, executor, target);
    }
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
    return new Spf4jCompletionStageRxInvoker(this,  executor);
  }

  @Override
  public <T extends RxInvoker> T rx(final Class<T> clazz) {
    if (clazz == Spf4jCompletionStageRxInvoker.class) {
      return (T)  new Spf4jCompletionStageRxInvoker(this, executor);
    } else {
      return ib.rx(clazz);
    }
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
            + target + ", executor=" + executor + ", execPolicyBuilder=" + execPolicyBuilder
            + '}';
  }

}
