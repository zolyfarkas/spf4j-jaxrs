
package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientExecutor;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.spf4j.base.Arrays;
import org.spf4j.failsafe.concurrent.DefaultFailSafeExecutor;
import org.spf4j.failsafe.concurrent.FailSafeExecutor;
import org.spf4j.jaxrs.common.providers.ProviderUtils;

/**
 * A improved JAX-RS client, that will do the following in addition to the stock Jersey client:
 * 1) retried + hedged execution.
 * 2) timeout propagation.
 * 3) Execution context propagation.
 * 4) JAX-RS Parameter converters in the client!
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class Spf4JClient implements Client {

  static {
    // time to cache DNS entries in seconds.
    System.setProperty("networkaddress.cache.ttl", "20");
    // time to cache failed attempts in seconds.
    System.setProperty("networkaddress.cache.negative.ttl", "5");
  }

  private final Client cl;

  private final FailSafeExecutor executor;

  private final ClientExceptionMapper exceptionMapper;

  public Spf4JClient(final  Client cl) {
    this(cl,  DefaultFailSafeExecutor.instance(),
            DefaultClientExceptionMapper.INSTANCE);
  }

  public Spf4JClient(final  Client cl,
          final FailSafeExecutor fsExec) {
    this(cl, fsExec, DefaultClientExceptionMapper.INSTANCE);
  }

  @SuppressWarnings("unchecked")
  public Spf4JClient(final  Client cl,
          final FailSafeExecutor fsExec, final ClientExceptionMapper exceptionMapper) {
    this.cl = cl;
    ClientConfig configuration = (ClientConfig) cl.getConfiguration();
    HttpUrlConnectorProvider httpUrlConnectorProvider = new HttpUrlConnectorProvider();
    httpUrlConnectorProvider.connectionFactory(CustomConnectionFactory.INSTANCE);
    configuration.connectorProvider(httpUrlConnectorProvider);
    this.executor = fsExec;
    this.exceptionMapper = exceptionMapper;
  }

  public static Spf4JClient  create(final Client cl) {
    if (cl instanceof Spf4JClient) {
      return (Spf4JClient) cl;
    }
    return new Spf4JClient(cl);
  }

  public ClientExceptionMapper getExceptionMapper() {
    return exceptionMapper;
  }

  public static List<ParamConverterProvider> getParamConverters(final Configuration pconfig) {
    if (!(pconfig instanceof ClientConfig)) {
      throw new IllegalArgumentException("Not a Jerjey Client Config: " + pconfig);
    }
    ClientExecutor clientExecutor = ((ClientConfig) pconfig).getClientExecutor();
    Configuration config;
    try {
      Method m = clientExecutor.getClass().getDeclaredMethod("getConfig");
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          m.setAccessible(true);
          return null;
        }
      });
      config = (Configuration) m.invoke(clientExecutor);
    } catch (IllegalAccessException
            | NoSuchMethodException | InvocationTargetException | RuntimeException e) {
      throw new IllegalStateException(e);
    }

    List<ParamConverterProvider> paramConverters = null;
    Set<Object> instances = config.getInstances();
    for (Object prov : instances) {
      if (prov instanceof ParamConverterProvider) {
        if (paramConverters == null) {
          paramConverters = new ArrayList<>(2);
        }
        paramConverters.add((ParamConverterProvider) prov);
      }
    }
    return paramConverters == null ? Collections.EMPTY_LIST : ProviderUtils.ordered(paramConverters);
  }


  @Nullable
  public static ParamConverter getConverter(final Class type, final Iterable<ParamConverterProvider> paramConverters) {
    for (ParamConverterProvider pcp : paramConverters) {
      ParamConverter converter = pcp.getConverter(type, type, Arrays.EMPTY_ANNOT_ARRAY);
      if (converter != null) {
        return converter;
      }
    }
    return null;
  }

  public static Object[] convert(final List<ParamConverterProvider> paramConverters, final Object... params) {
    Object[] result = null;
    for (int i = 0; i < params.length; i++) {
      Object oo = params[i];
      if (oo != null) {
        ParamConverter converter = getConverter(oo.getClass(), paramConverters);
        if (converter != null) {
          if (result == null) {
            result = params.clone();
          }
          try {
            result[i] = URLEncoder.encode(converter.toString(oo), StandardCharsets.UTF_8.name());
          } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }
    return result == null ? params : result;
  }

  public static List<Object> convert(final List<ParamConverterProvider> paramConverters, final List<Object> params) {
    List<Object> result = null;
    for (int i = 0, l = params.size(); i < l; i++) {
      Object oo = params.get(i);
      if (oo != null) {
        ParamConverter converter = getConverter(oo.getClass(), paramConverters);
        if (converter != null) {
          if (result == null) {
            result = new ArrayList<>(params);
          }
          try {
            result.set(i, URLEncoder.encode(converter.toString(oo), StandardCharsets.UTF_8.name()));
          } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }
    return result == null ? params : result;
  }

  @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
  @Nullable
  static Object convert(final List<ParamConverterProvider> paramConverters, final Object param) {
    if (param == null) {
      return null;
    }
    ParamConverter converter = getConverter(param.getClass(), paramConverters);
    if (converter != null) {
      try {
        return URLEncoder.encode(converter.toString(param), StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    } else {
      return param;
    }
  }

  public Spf4JClient withExceptionMapper(final ClientExceptionMapper pexceptionMapper) {
    return new Spf4JClient(cl,  executor, pexceptionMapper);
  }

  @Override
  public void close() {
    cl.close();
  }

  @Override
  public Spf4jWebTarget target(final String uri) {
    return new Spf4jWebTarget(this, cl.target(uri), executor);
  }

  @Override
  public Spf4jWebTarget target(final URI uri) {
    return new Spf4jWebTarget(this, cl.target(uri), executor);
  }

  @Override
  public Spf4jWebTarget target(final UriBuilder uriBuilder) {
    return new Spf4jWebTarget(this, cl.target(uriBuilder), executor);
  }

  @Override
  public Spf4jWebTarget target(final Link link) {
    return new Spf4jWebTarget(this, cl.target(link), executor);
  }

  @Override
  public Spf4jInvocationBuilder invocation(final Link link) {
    return new Spf4jInvocationBuilder(this, cl.invocation(link), executor, new Spf4jWebTarget(this, cl.target(link),
            executor));
  }

  @Override
  public SSLContext getSslContext() {
    return cl.getSslContext();
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return cl.getHostnameVerifier();
  }

  @Override
  public Configuration getConfiguration() {
    return cl.getConfiguration();
  }

  @Override
  public Spf4JClient property(final String name, final Object value) {
    cl.property(name, value);
    return this;
  }

  @Override
  public Spf4JClient register(final Class<?> componentClass) {
    cl.register(componentClass);
    return this;
  }

  @Override
  public Spf4JClient register(final Class<?> componentClass, final int priority) {
    cl.register(componentClass, priority);
    return this;
  }

  @Override
  public Spf4JClient register(final Class<?> componentClass, final Class<?>... contracts) {
    cl.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4JClient register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
    cl.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4JClient register(final Object component) {
    cl.register(component);
    return this;
  }

  @Override
  public Spf4JClient register(final Object component, final int priority) {
    cl.register(component, priority);
    return this;
  }

  @Override
  public Spf4JClient register(final Object component, final Class<?>... contracts) {
    cl.register(component, contracts);
    return this;
  }

  @Override
  public Spf4JClient register(final Object component, final Map<Class<?>, Integer> contracts) {
    cl.register(component, contracts);
    return this;
  }

  @Override
  public String toString() {
    return "Spf4JClient{" + "cl=" + cl +  ", executor=" + executor + '}';
  }

  private static class CustomConnectionFactory implements HttpUrlConnectorProvider.ConnectionFactory {

    private static final CustomConnectionFactory INSTANCE = new CustomConnectionFactory();

    /**
     * Attempt to client side load balance...
     * Approach works for HTTP only.
     * for HTTPS  we would need to register a new HTTP URL connection. to do implemented later.
     *
     * @param url
     * @return
     * @throws IOException
     */
    @Override
    @SuppressFBWarnings({"PREDICTABLE_RANDOM", "URLCONNECTION_SSRF_FD"})
    public HttpURLConnection getConnection(final URL url) throws IOException {
      try {
        String protocol = url.getProtocol();
        if ("file".equalsIgnoreCase(protocol)) {
          throw new IOException("File protocol not supported: " + url);
        }
        if ("https".equalsIgnoreCase(protocol)) {
          return (HttpURLConnection) url.openConnection();
        }
        URI uri = url.toURI();
        String host = uri.getHost();
        if (host == null) {
          return (HttpURLConnection) url.openConnection();
        }
        InetAddress[] targets = InetAddress.getAllByName(host);
        if (targets.length <= 1) {
           return (HttpURLConnection) url.openConnection();
        }
        InetAddress chosen = targets[ThreadLocalRandom.current().nextInt(targets.length)];
        URI newUri = UriBuilder.fromUri(uri).host(chosen.getHostAddress()).build();
        return (HttpURLConnection) newUri.toURL().openConnection();
      } catch (URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

}
