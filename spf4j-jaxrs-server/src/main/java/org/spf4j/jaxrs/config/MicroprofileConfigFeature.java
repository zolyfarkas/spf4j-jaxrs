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
package org.spf4j.jaxrs.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.GenericType;
import org.apache.avro.SchemaResolver;
import org.apache.avro.SchemaResolvers;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ExternalConfigurationProvider;

/**
 * @author Zoltan Farkas
 */
public final class MicroprofileConfigFeature implements Feature {

  static {
    ConfigProviderResolver resolver = ConfigProviderResolver.instance();
    SchemaResolver schemaResolver = SchemaResolvers.getDefault();
    if (resolver instanceof ConfigProviderResolverImpl) {
      ConfigProviderResolver.setInstance(((ConfigProviderResolverImpl) resolver).withNewSchemaResolver(schemaResolver));

    } else {
      ConfigProviderResolver.setInstance(new ConfigProviderResolverImpl(schemaResolver));
    }
  }

  @Override
  public boolean configure(final FeatureContext context) {
    JerseyMicroprofileConfigurationProvider provider = new JerseyMicroprofileConfigurationProvider();
//    context.register(provider);
    context.register(new RegisterAnnotInjector(provider));
    return true;
  }

  public boolean configure(final ResourceConfig context) {
    JerseyMicroprofileConfigurationProvider provider = new JerseyMicroprofileConfigurationProvider();
//    context.register(JerseyMicroprofileConfigurationProvider.class);
    context.register(new RegisterAnnotInjector(provider));
    return true;
  }

  private static class RegisterAnnotInjector extends AbstractBinder {

    private final JerseyMicroprofileConfigurationProvider cfgProvider;

    RegisterAnnotInjector(final JerseyMicroprofileConfigurationProvider cfgProvider) {
      this.cfgProvider = cfgProvider;
    }

    @Override
    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    protected void configure() {
      bind(cfgProvider).to(JerseyMicroprofileConfigurationProvider.class);
      bind(cfgProvider).to(ExternalConfigurationProvider.class);
      bind(HK2ConfigurationInjector.class)
              .to(new GenericType<InjectionResolver<ConfigProperty>>() {
              })
              .in(Singleton.class);

//      bind(JerseyConfigurationInjector.class)
//              .to(new GenericType<org.glassfish.jersey.internal.inject.InjectionResolver<ConfigProperty>>() { })
//              .in(Singleton.class);
    }
  }

}
