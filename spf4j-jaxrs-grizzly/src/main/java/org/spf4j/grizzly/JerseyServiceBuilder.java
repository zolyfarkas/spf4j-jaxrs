/*
 * Copyright 2020 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.grizzly;

import org.spf4j.jaxrs.JaxRsConfiguration;
import com.google.common.base.Joiner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.Env;
import gnu.trove.set.hash.THashSet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;
import org.apache.avro.SchemaResolver;
import org.apache.avro.SchemaResolvers;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.FixedWebappContext;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.spf4j.avro.NoSnapshotRefsResolver;
import org.spf4j.avro.SchemaClient;
import org.spf4j.concurrent.LifoThreadPoolBuilder;
import org.spf4j.jaxrs.Spf4jBinder;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.jaxrs.server.SecurityAuthenticator;
import org.spf4j.http.multi.MultiURLs;
import org.spf4j.http.multi.Spf4jURLStreamHandlerFactory;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.config.MicroprofileConfigFeature;
import org.spf4j.jaxrs.features.AvroFeature;
import org.spf4j.jaxrs.features.GeneralPurposeFeatures;
import org.spf4j.jaxrs.server.DelegatingAuthenticationClientFilter;
import org.spf4j.jaxrs.server.features.ImmediateFeature;
import org.spf4j.jaxrs.server.providers.DefaultServerProvidersFeatures;
import org.spf4j.servlet.ExecutionContextFilter;
import org.spf4j.stackmonitor.Sampler;

/**
 * @author Zoltan Farkas
 */
public final class JerseyServiceBuilder implements JaxRsConfiguration {


  private String bindAddr;

  private int listenPort;

  private final Set<String> providerPackages;

  private final Set<Class<?>> serviceProviders;

  private final List<Binder> binders;

  private LinkedHashSet<String> mavenRepos;

  private final JvmServices jvmServices;

  private int kernelThreadsCoreSize;

  private int kernelThreadsMaxSize;

  private int workerThreadsCoreSize;

  private int workerThreadsMaxSize;

  private int maxHeaderSizeBytes;

  private long defaultTimeoutNanos;

  private long maximumAllowedTimeoutNanos;

  private SecurityAuthenticator auth = SecurityAuthenticator.NOAUTH;

  /* see https://github.com/jersey/jersey/blob/master/examples/
  https-clientserver-grizzly/src/main/java/org/glassfish/jersey/examples/httpsclientservergrizzly/Server.java */
  private SSLEngineConfigurator sslConfig;

  public JerseyServiceBuilder(final JvmServices jvmServices) {
    this.bindAddr = "0.0.0.0";
    this.listenPort = Env.getValue("APP_SERVICE_PORT", 8080);
    this.providerPackages = new THashSet<>(5);
    this.serviceProviders =  new THashSet<>(7);
    this.binders = new ArrayList<>(4);
    this.jvmServices = jvmServices;
    this.mavenRepos = new LinkedHashSet<>(4);
    mavenRepos.add("https://repo1.maven.org/maven2");
    serviceProviders.add(GeneralPurposeFeatures.class);
    serviceProviders.add(DefaultServerProvidersFeatures.class);
    this.kernelThreadsCoreSize = 1;
    this.kernelThreadsMaxSize = 4;
    this.workerThreadsCoreSize = 4;
    this.workerThreadsMaxSize = 256;
    this.sslConfig = null;
    this.maxHeaderSizeBytes = 256 * 1024;
    this.defaultTimeoutNanos = TimeUnit.SECONDS.toNanos(10);
    this.maximumAllowedTimeoutNanos = TimeUnit.SECONDS.toNanos(25);
  }

  public JerseyServiceBuilder removeDefaults() {
    mavenRepos.clear();
    serviceProviders.clear();
    return this;
  }

  public   JerseyServiceBuilder withBindAddress(final String pbindAddr) {
    this.bindAddr = pbindAddr;
    return this;
  }

  public JerseyServiceBuilder withDefaultTimeout(final long timeout, final TimeUnit tu) {
    this.defaultTimeoutNanos = tu.toNanos(timeout);
    return this;
  }

  public JerseyServiceBuilder withMaximumAllowedTimeout(final long timeout, final TimeUnit tu) {
    this.maximumAllowedTimeoutNanos = tu.toNanos(timeout);
    return this;
  }

  public JerseyServiceBuilder withFeature(final int pmaxHeaderSizeBytes) {
    this.maxHeaderSizeBytes = pmaxHeaderSizeBytes;
    return this;
  }

  public JerseyServiceBuilder withFeature(final Class<? extends Feature> feature) {
    this.serviceProviders.add(feature);
    return this;
  }

  public JerseyServiceBuilder withServiceProvider(final Class<?> serviceClass) {
    this.serviceProviders.add(serviceClass);
    return this;
  }

   public JerseyServiceBuilder withBinder(final Binder binder) {
    this.binders.add(binder);
    return this;
  }

  public JerseyServiceBuilder withProviderPackage(final String pkg) {
    this.providerPackages.add(pkg);
    return this;
  }

  public JerseyServiceBuilder withPort(final int port) {
    this.listenPort = port;
    return this;
  }

  public JerseyServiceBuilder withKernelThreadsCoreSize(final int size) {
    this.kernelThreadsCoreSize = size;
    return this;
  }

  public JerseyServiceBuilder withKernelThreadsMaxSize(final int size) {
    this.kernelThreadsMaxSize = size;
    return this;
  }

  public JerseyServiceBuilder withWorkerThreadsMaxSize(final int size) {
    this.workerThreadsMaxSize = size;
    return this;
  }

  public JerseyServiceBuilder withWorkerThreadsCoreSize(final int size) {
    this.workerThreadsCoreSize = size;
    return this;
  }

  public JerseyServiceBuilder withSSLEngineConfigurator(final SSLEngineConfigurator configurator) {
    this.sslConfig = configurator;
    return this;
  }

  public JerseyServiceBuilder withMavenRepoURL(final String mavenRepoUrl) {
    LinkedHashSet<String> nRepos = new LinkedHashSet<>((int) ((mavenRepos.size() + 1) * 1.4));
    nRepos.addAll(this.mavenRepos);
    nRepos.add(mavenRepoUrl);
    this.mavenRepos = nRepos;
    return this;
  }

  public JerseyServiceBuilder withSecurityAuthenticator(final SecurityAuthenticator pauth) {
    this.auth = pauth;
    return this;
  }

  public Set<String> getProviderPackages() {
    return providerPackages;
  }

  public List<Binder> getBinders() {
    return binders;
  }


  public JerseyService build() throws IOException {
    return new JerseyServiceImpl();
  }

  @Override
  public String toString() {
    return "JerseyServiceBuilder{" + "bindAddr=" + bindAddr + ", listenPort="
            + listenPort + ", providerPackages=" + providerPackages + ", serviceProviders="
            + serviceProviders + ", binders=" + binders + ", mavenRepos=" + mavenRepos
            + ", jvmServices=" + jvmServices + ", kernelThreadsCoreSize=" + kernelThreadsCoreSize
            + ", kernelThreadsMaxSize=" + kernelThreadsMaxSize + ", workerThreadsCoreSize="
            + workerThreadsCoreSize + ", workerThreadsMaxSize=" + workerThreadsMaxSize
            + ", sslConfig=" + sslConfig + '}';
  }

  @Override
  public Set<Class<? extends Feature>> getFeatures() {
    Set<Class<? extends Feature>> res = new THashSet<>(serviceProviders.size());
    for (Class<?> clasz : serviceProviders) {
      if (Feature.class.isAssignableFrom(clasz)) {
        res.add((Class<? extends Feature>) clasz);
      }
    }
    return res;
  }

  private  class JerseyServiceImpl implements JerseyService {

    private final HttpServer server;

    private ResourceConfig resourceConfig;


    JerseyServiceImpl() throws IOException {
      server = createHttpServer();
    }

    private HttpServer createHttpServer()
            throws IOException {
      String jerseyAppName = bindAddr + ':' + listenPort;
      FixedWebappContext webappContext = new FixedWebappContext(jerseyAppName, "");
      DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol(defaultTimeoutNanos, maximumAllowedTimeoutNanos,
              TimeUnit.NANOSECONDS);
      FilterRegistration fr = webappContext.addFilter("ecFilter", new ExecutionContextFilter(dp, auth));
      fr.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

      SchemaResolver schemaClient;
      try {
        if (mavenRepos.size() == 1) {
          schemaClient = new NoSnapshotRefsResolver(new SchemaClient(new URI(mavenRepos.iterator().next())));
        } else {
          try {
            URL.setURLStreamHandlerFactory(new Spf4jURLStreamHandlerFactory());
          } catch (Error e) {
            String msg = e.getMessage();
            if (msg == null || !msg.contains("factory already defined")) {
              throw e;
            }
          }
          schemaClient = new NoSnapshotRefsResolver(new SchemaClient(MultiURLs.newURL(MultiURLs.Protocol.mhttps,
                mavenRepos.toArray(new String[mavenRepos.size()])).toURI()));
        }
      } catch (URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
      SchemaResolvers.registerDefault(schemaClient);
      String hostName = jvmServices.getHostName();
      resourceConfig = ResourceConfig.forApplicationClass(Spf4jJaxrsApplication.class);
      resourceConfig.setApplicationName(jerseyAppName);
      new MicroprofileConfigFeature().configure(resourceConfig);
      resourceConfig.register(new MicroprofileConfigFeature());
      //resourceConfig.packages(true, providerPackages.toArray(new String[providerPackages.size()]));
      resourceConfig.registerInstances(binders.toArray(new Object[binders.size()]));
      resourceConfig.registerClasses((Set) serviceProviders);
      resourceConfig.property(JaxRsConfiguration.class.getName(), JerseyServiceBuilder.this);
      resourceConfig.property("hostName", hostName);
      resourceConfig.property("servlet.bindAddr", bindAddr);
      resourceConfig.property("servlet.port", listenPort);
      resourceConfig.property("servlet.protocol", sslConfig == null ? "http" : "https");
      resourceConfig.property("application.logFilesPath", jvmServices.getLogFolder());
      resourceConfig.property(ServerProperties.PROVIDER_PACKAGES, Joiner.on(';').join(providerPackages));
      resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, "true");
      resourceConfig.register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(jvmServices.getProfiler()).to(Sampler.class);
        }
      });

    AvroFeature avroFeature = new AvroFeature(
            new DefaultSchemaProtocol(schemaClient), schemaClient);
    Spf4JClient restClient = Spf4JClient.create(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .register(new ExecutionContextClientFilter(dp, true))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(new GeneralPurposeFeatures())
            .register(EncodingFilter.class)
            .register(avroFeature)
            .register(DelegatingAuthenticationClientFilter.class)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());
      resourceConfig.register(new Spf4jBinder(schemaClient, restClient));
      resourceConfig.register(avroFeature);
      resourceConfig.register(ImmediateFeature.class);
      ServletContainer servletContainer = new ServletContainer(resourceConfig);
      resourceConfig.property(ServletContainer.class.getName(), servletContainer);
      ServletRegistration servletRegistration = webappContext.addServlet("jersey", servletContainer);
      servletRegistration.addMapping("/*");
      servletRegistration.setLoadOnStartup(0);
      HttpServer result = new HttpServer();
      ServerConfiguration serverConfig = result.getServerConfiguration();
      serverConfig.setDefaultErrorPageGenerator(new GrizzlyErrorPageGenerator(schemaClient));
//    config.addHttpHandler(new CLStaticHttpHandler(Thread.currentThread().getContextClassLoader(), "/static/"),
//            "/*.ico", "/*.png");
      result.addListener(createHttpListener(bindAddr, listenPort));
      webappContext.deploy(result);
      webappContext.setServerInfo(null);
      return result;
    }

    private NetworkListener createHttpListener(final String pbindAddr,
            final int port) {
      if (sslConfig == null) {
        return createHttpListener("http", pbindAddr, port);
      } else {
        NetworkListener nl = createHttpListener("https", pbindAddr, port);
        nl.setSecure(true);
        nl.setSSLEngineConfig(sslConfig);
        return nl;
      }
    }

    private NetworkListener createHttpListener(final String name, final String pbindAddr,
            final int port) {
      final String poolNameBase = name + ':' + port;
      final NetworkListener listener
              = new NetworkListener("http", pbindAddr, port);
      listener.setMaxHttpHeaderSize(maxHeaderSizeBytes);
      // Create default HTTP/2 configuration and provide it to the AddOn
      //Http2Configuration configuration = Http2Configuration.builder().build();
      //Http2AddOn http2Addon = new Http2AddOn(configuration);

      // Register the Addon.
      //listener.registerAddOn(http2Addon);

      CompressionConfig compressionConfig = listener.getCompressionConfig();
      compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON); // the mode
      compressionConfig.setCompressionMinSize(4096); // the min amount of bytes to compress
      compressionConfig.setCompressibleMimeTypes("text/plain",
              "text/html", "text/csv", "application/json",
              "application/octet-stream", "application/avro",
              "application/avro+json", "application/avro-x+json"); // the mime types to compress
      TCPNIOTransport transport = listener.getTransport();
      transport.setKernelThreadPool(LifoThreadPoolBuilder.newBuilder()
              .withCoreSize(kernelThreadsCoreSize)
              .withMaxSize(kernelThreadsMaxSize)
              .withDaemonThreads(true)
              .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.kernel.idleMillis", 120000))
              .withPoolName(poolNameBase + "-kernel")
              .withQueueSizeLimit(64)
              .enableJmx()
              .build());
      transport.setSelectorRunnersCount(Integer.getInteger("spf4j.grizzly.selectorCount", 4));
      transport.setWorkerThreadPool(LifoThreadPoolBuilder.newBuilder()
              .withCoreSize(workerThreadsCoreSize)
              .withMaxSize(workerThreadsMaxSize)
              .withDaemonThreads(false)
              .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.worker.idleMillis", 120000))
              .withPoolName(poolNameBase + "-worker")
              .withQueueSizeLimit(0)
              .enableJmx()
              .build());
      return listener;
    }

    @Override
    public void close() {
      server.shutdown(30, TimeUnit.SECONDS);
    }

    @Override
    public void start() throws IOException {
      server.start();
    }

    @Override
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE") // should blow up.
    public Spf4jJaxrsApplication getApplication() {
      return (Spf4jJaxrsApplication) resourceConfig.getApplication();
    }

  }

}
