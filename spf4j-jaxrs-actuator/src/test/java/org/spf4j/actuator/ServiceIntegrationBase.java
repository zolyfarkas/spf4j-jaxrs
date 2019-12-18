/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.actuator;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.core.converter.ModelConverters;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import org.apache.avro.SchemaResolvers;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.FixedWebappContext;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.spf4j.actuator.apiBrowser.AvroModelConverter;
import org.spf4j.actuator.apiBrowser.OpenApiResource;
import org.spf4j.actuator.health.checks.DefaultHealthChecksBinder;
import org.spf4j.avro.SchemaClient;
import org.spf4j.base.avro.NetworkProtocol;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.Service;
import org.spf4j.cluster.SingleNodeCluster;
import org.spf4j.concurrent.LifoThreadPoolBuilder;
import org.spf4j.grizzly.GrizzlyErrorPageGenerator;
import org.spf4j.hk2.Spf4jBinder;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jClientBuilder;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.providers.gp.CharSequenceMessageProvider;
import org.spf4j.jaxrs.common.providers.gp.CsvParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.gp.DirectStringMessageProvider;
import org.spf4j.jaxrs.common.providers.GZipEncoderDecoder;
import org.spf4j.jaxrs.common.providers.avro.AvroFeature;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.common.providers.gp.SampleNodeMessageProviderD3Json;
import org.spf4j.jaxrs.common.providers.gp.SampleNodeMessageProviderJson;
import org.spf4j.servlet.ExecutionContextFilter;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("CE_CLASS_ENVY")
public abstract class ServiceIntegrationBase {

  private static HttpServer server;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static String localService;

  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public static HttpServer startHttpServer(final String hostName, final String bindAddr, final int port)
          throws IOException, URISyntaxException {
    WebappContext webappContext = new FixedWebappContext("grizzly web context", "");
    ServletRegistration servletRegistration = webappContext.addServlet("jersey", ServletContainer.class);
    servletRegistration.setInitParameter("javax.ws.rs.Application", TestApplication.class.getName());
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
            "org.spf4j.jaxrs.server.providers;org.spf4j.jaxrs.common.providers.gp");
    servletRegistration.setInitParameter("hostName", hostName);
    servletRegistration.setInitParameter("servlet.bindAddr", bindAddr);
    servletRegistration.setInitParameter("servlet.port", Integer.toString(port));
    servletRegistration.setInitParameter("servlet.protocol", "http");
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setLoadOnStartup(0);
    HttpServer srv = new HttpServer();
    srv.getServerConfiguration()
            .setDefaultErrorPageGenerator(new GrizzlyErrorPageGenerator(
                    new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"))));
//  final ServerConfiguration config = server.getServerConfiguration();
//  config.addHttpHandler(new StaticHttpHandler(docRoot), "/");
    final NetworkListener listener
            = new NetworkListener("http", bindAddr, port);
    CompressionConfig compressionConfig = listener.getCompressionConfig();
    compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON); // the mode
    compressionConfig.setCompressionMinSize(4096); // the min amount of bytes to compress
    compressionConfig.setCompressibleMimeTypes("text/plain",
            "text/html", "text/csv", "application/json",
            "application/octet-stream", "application/avro",
            "application/avro+json", "application/avro-x+json"); // the mime types to compress
    TCPNIOTransport transport = listener.getTransport();
    transport.setKernelThreadPool(LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(Integer.getInteger("spf4j.grizzly.kernel.coreSize", 2))
            .withMaxSize(Integer.getInteger("spf4j.grizzly.kernel.maxSize", 8))
            .withDaemonThreads(true)
            .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.kernel.idleMillis", 120000))
            .withPoolName("gz-core")
            .withQueueSizeLimit(0)
            .enableJmx()
            .build());
    transport.setSelectorRunnersCount(Integer.getInteger("spf4j.grizzly.selectorCount", 4));
    transport.setWorkerThreadPool(LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(Integer.getInteger("spf4j.grizzly.worker.coreSize", 4))
            .withMaxSize(Integer.getInteger("spf4j.grizzly.worker.maxSize", 1024))
            .withDaemonThreads(false)
            .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.worker.idleMillis", 120000))
            .withPoolName("gz-work")
            .withQueueSizeLimit(0)
            .enableJmx()
            .build());
    srv.addListener(listener);
    webappContext.deploy(srv);
    srv.start();
    return srv;
  }

  @BeforeClass
  public static void setUp() throws IOException, URISyntaxException {
    // start the server
    ModelConverters.getInstance().addConverter(AvroModelConverter.INSTANCE);
    server = startHttpServer("127.0.0.1", "127.0.0.1", 9090);
    client = TestApplication.getInstance().getRestClient();
    localService = "http://127.0.0.1:" + server.getListener("http").getPort();
    target = client.target(localService);
  }

  @AfterClass
  public static void tearDown() {
    server.shutdownNow();
    ModelConverters.getInstance().removeConverter(AvroModelConverter.INSTANCE);
  }

  @Singleton
  @ApplicationPath("/")
  private static class TestApplication extends ResourceConfig {

    private static volatile TestApplication instance;

    private final SchemaClient schemaClient;

    private final Spf4JClient restClient;

    @Inject
    TestApplication(@Context final ServletContext srvContext, final ServiceLocator locator) {
      ServiceLocatorUtilities.enableImmediateScope(locator);
      DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol();
      FilterRegistration testFilterReg = srvContext.addFilter("server", new ExecutionContextFilter(dp));
      testFilterReg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
      try {
        schemaClient = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
      } catch (URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
      SchemaResolvers.registerDefault(schemaClient);
      AvroFeature avroFeature = new AvroFeature(new DefaultSchemaProtocol(schemaClient), schemaClient);
      restClient = new Spf4jClientBuilder()
              .connectTimeout(2, TimeUnit.SECONDS)
              .readTimeout(60, TimeUnit.SECONDS)
              .register(new SampleNodeMessageProviderJson())
              .register(new SampleNodeMessageProviderD3Json())
              .register(new ExecutionContextClientFilter(dp, true))
              .register(ClientCustomExecutorServiceProvider.class)
              .register(ClientCustomScheduledExecutionServiceProvider.class)
              .register(new CsvParameterConverterProvider(Collections.EMPTY_LIST))
              .register(new CharSequenceMessageProvider())
              .register(GZipEncoder.class)
              .register(DeflateEncoder.class)
              .register(EncodingFilter.class)
              .register(avroFeature)
              .property(ClientProperties.USE_ENCODING, "gzip")
              .build();
      register(new Spf4jBinder(schemaClient, restClient, (x) -> true));
      register(avroFeature);
      register(CsvParameterConverterProvider.class);
      register(GZipEncoderDecoder.class);
      register(new DirectStringMessageProvider());
      register(new CharSequenceMessageProvider());
      Sampler profiler = new Sampler(5);
      profiler.start();
      register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(profiler).to(Sampler.class);
        }
      });
      javax.servlet.ServletRegistration servletRegistration = srvContext.getServletRegistration("jersey");
      String initParameter = servletRegistration.getInitParameter("servlet.port");
      String bindAddr = servletRegistration.getInitParameter("servlet.bindAddr");
      register(new ClusterBinder(bindAddr, Integer.parseInt(initParameter)));
      register(new DefaultHealthChecksBinder());
      packages("org.spf4j.actuator");
      registerClasses(OpenApiResource.class);
      if (instance != null) {
        throw new IllegalStateException("Application already initialized " + instance);
      }
      instance = this;
    }

    @PreDestroy
    public void cleanup() {
      instance = null;
    }

    public static TestApplication getInstance() {
      return instance;
    }

    public SchemaClient getSchemaClient() {
      return schemaClient;
    }

    public Spf4JClient getRestClient() {
      return restClient;
    }

  }

  private static class ClusterBinder extends AbstractBinder {

    private String bindAddr;

    private final int port;

    @Inject
    ClusterBinder(
            @ConfigProperty(name = "servlet.bindAddr") final String bindAddr,
            @ConfigProperty(name = "servlet.port") final int port) {
      this.bindAddr = bindAddr;
      this.port = port;
    }

    @Override
    protected void configure() {
      try {
        SingleNodeCluster singleNodeCluster = new SingleNodeCluster(
                ImmutableSet.copyOf(InetAddress.getAllByName(bindAddr)),
                Collections.singleton(new NetworkService("http",
                        port, NetworkProtocol.TCP)));
        bind(singleNodeCluster).to(Cluster.class);
        bind(singleNodeCluster).to(Service.class);
      } catch (UnknownHostException ex) {
        throw new RuntimeException(ex);
      }
    }

  }

  public static HttpServer getServer() {
    return server;
  }

  public static Spf4jWebTarget getTarget() {
    return target;
  }

  public static Spf4JClient getClient() {
    return client;
  }

  public static String getLocalService() {
    return localService;
  }

}
