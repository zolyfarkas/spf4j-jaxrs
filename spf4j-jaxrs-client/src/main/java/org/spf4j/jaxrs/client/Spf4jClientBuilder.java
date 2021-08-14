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
package org.spf4j.jaxrs.client;

import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.spf4j.jaxrs.config.ExtendedConfig;

/**
 * A failsafe capable client builder.
 * @author Zoltan Farkas
 */
public final class Spf4jClientBuilder extends ClientBuilder {

  /**
   * if other ClientBuidlers are available via ServiceLoader Api, they will be prefered.
   */
  public static void registerAsDefaultClientBuilder() {
    System.setProperty(ClientBuilder.JAXRS_DEFAULT_CLIENT_BUILDER_PROPERTY,
            Spf4jClientBuilder.class.getName());
  }


  private final JerseyClientBuilder jerseyBuilder;

  public Spf4jClientBuilder() {
    this.jerseyBuilder = new JerseyClientBuilder();
  }


  @Override
  public Spf4jClientBuilder withConfig(final Configuration config) {
    jerseyBuilder.withConfig(config);
    return this;
  }

  @Override
  public Spf4jClientBuilder sslContext(final SSLContext sslContext) {
    jerseyBuilder.sslContext(sslContext);
    return this;
  }

  @Override
  public Spf4jClientBuilder keyStore(final KeyStore keyStore, final char[] password) {
    jerseyBuilder.keyStore(keyStore, password);
    return this;
  }

  @Override
  public Spf4jClientBuilder trustStore(final KeyStore trustStore) {
    jerseyBuilder.trustStore(trustStore);
    return this;
  }

  @Override
  public Spf4jClientBuilder hostnameVerifier(final HostnameVerifier verifier) {
    jerseyBuilder.hostnameVerifier(verifier);
    return this;
  }

  @Override
  public Spf4jClientBuilder executorService(final ExecutorService executorService) {
    jerseyBuilder.executorService(executorService);
    return this;
  }

  @Override
  public Spf4jClientBuilder scheduledExecutorService(
          final ScheduledExecutorService scheduledExecutorService) {
    jerseyBuilder.scheduledExecutorService(scheduledExecutorService);
    return this;
  }

  @Override
  public Spf4jClientBuilder connectTimeout(final long timeout, final TimeUnit unit) {
    jerseyBuilder.connectTimeout(timeout, unit);
    return this;
  }

  @Override
  public Spf4jClientBuilder readTimeout(final long timeout, final TimeUnit unit) {
    jerseyBuilder.readTimeout(timeout, unit);
    return this;
  }

  @Override
  public Spf4JClient build() {
    if (this.jerseyBuilder.getConfiguration().getProperty(ExtendedConfig.PROPERTY_NAME) == null) {
      this.jerseyBuilder.property(ExtendedConfig.PROPERTY_NAME, ConfigProviderResolver.instance().getConfig());
    }
    return new Spf4JClient(jerseyBuilder.build());
  }

  @Override
  public Configuration getConfiguration() {
    return jerseyBuilder.getConfiguration();
  }

  @Override
  public Spf4jClientBuilder property(final String name, final Object value) {
    jerseyBuilder.property(name, value);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Class<?> componentClass) {
    jerseyBuilder.register(componentClass);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Class<?> componentClass, final int priority) {
    jerseyBuilder.register(componentClass, priority);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Class<?> componentClass, final Class<?>... contracts) {
    jerseyBuilder.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
    jerseyBuilder.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Object component) {
    jerseyBuilder.register(component);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Object component, final int priority) {
    jerseyBuilder.register(component, priority);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Object component, final Class<?>... contracts) {
    jerseyBuilder.register(component, contracts);
    return this;
  }

  @Override
  public Spf4jClientBuilder register(final Object component, final Map<Class<?>, Integer> contracts) {
    jerseyBuilder.register(component, contracts);
    return this;
  }

  @Override
  public String toString() {
    return "Spf4jClientBuilder{" + "jerseyBuilder=" + jerseyBuilder + '}';
  }

}
