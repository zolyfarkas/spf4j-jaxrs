
package org.spf4j.jaxrs.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.ClientBuilder;
import org.apache.avro.SchemaResolver;
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.providers.avro.AvroFeature;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;

/**
 * Utility class to set connectTimeout for jax-rs 2.1, backward compat with 2.0
 * @author Zoltan Farkas
 */
public final class ClientBuilderUtil {

  private ClientBuilderUtil() { }

  /**
   * Utility to set the default connect timeout, for JAX-RS 2.1
   * runtime compatible with JAX-RS 2.0
   * @return
   */
  public static ClientBuilder setConnectTimeout(final ClientBuilder builder,
          final long value, final TimeUnit unit) {
    try {
      Method method = builder.getClass().getMethod("connectTimeout", long.class, TimeUnit.class);
      return (ClientBuilder) method.invoke(builder, value, unit);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException ex) {
      // do nothing
      return builder;
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Utility to set the default read timeout, for JAX-RS 2.1
   * runtime compatible with JAX-RS 2.0
   * @return
   */
  public static ClientBuilder setReadTimeout(final ClientBuilder builder,
          final long value, final TimeUnit unit) {
    try {
      Method method = builder.getClass().getMethod("readTimeout", long.class, TimeUnit.class);
      return (ClientBuilder) method.invoke(builder, value, unit);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException ex) {
      // do nothing
      return builder;
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    }
  }


  /**
   * Create a JAX-RS client to call non-spf4j (no timeout propagation, no schema protocol ...) services.
   * @param connectTimeoutMillis
   * @param defaultReadTimeoutMillis
   * @return
   */
  public static Spf4JClient createClientNonSpf4jRest(final long connectTimeoutMillis,
          final long defaultReadTimeoutMillis) {
    javax.ws.rs.client.Client jaxRsClient = createClientBuilderNonSpf4jRest(connectTimeoutMillis,
            defaultReadTimeoutMillis) .build();
    return new Spf4JClient(jaxRsClient)
            .withHedgePolicy(HedgePolicy.NONE);
  }

  public static ClientBuilder createClientBuilderNonSpf4jRest(final long connectTimeoutMillis,
          final long defaultReadTimeoutMillis) {
    AvroFeature avroFeature = new AvroFeature(SchemaProtocol.NONE, SchemaResolver.NONE);
    ClientBuilder clBuilder = ClientBuilder
            .newBuilder()
            .register(new ExecutionContextClientFilter(DeadlineProtocol.NONE, true))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(avroFeature)
            .property("jersey.config.client.useEncoding", "gzip"); //see ClientProperties is jersey 2.28+
    clBuilder = ClientBuilderUtil.setConnectTimeout(clBuilder, connectTimeoutMillis, TimeUnit.MILLISECONDS);
    return ClientBuilderUtil.setReadTimeout(clBuilder, defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
  }


}
