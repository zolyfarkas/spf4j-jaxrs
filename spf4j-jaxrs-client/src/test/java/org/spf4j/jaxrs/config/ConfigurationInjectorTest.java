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
package org.spf4j.jaxrs.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Configuration;
import org.apache.avro.SchemaResolver;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.annotations.Service;
import org.spf4j.jaxrs.config.sources.MemoryConfigSource;

/**
 *
 * @author Zoltan Farkas
 */
public class ConfigurationInjectorTest {

  private static final MemoryConfigSource M_CONFIG = new MemoryConfigSource();

  static {
    ConfigProviderResolver.setInstance(new ConfigProviderResolverImpl(SchemaResolver.NONE,
            new ConfigBuilderImpl(SchemaResolver.NONE).addDefaultSources().withSources(M_CONFIG).build()));
    System.setProperty("SysPropEnv", "1");
  }

  @Service
  @Singleton
  public static final class TestClass {

    private final String value;

    private final Provider<String> providedValue;

    private final int envSys;

    @Inject
    public TestClass(@ConfigProperty(name = "myProp", defaultValue = "bubu") final String value,
            @Nullable @ConfigProperty(name = "myProp2") final Provider<String> providedValue,
            @ConfigProperty(name = "SysPropEnv") final int envSys) {
      this.value = value;
      this.providedValue = providedValue;
      this.envSys = envSys;
    }

    public int getEnvSys() {
      return envSys;
    }

    public String getValue() {
      return value;
    }

    public String getValue2() {
      return providedValue.get();
    }

  }

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testConfigInjection() {
    ServiceLocator loc = ServiceLocatorFactory.getInstance().create("test");
    ServiceLocatorUtilities.bind(loc, new AbstractBinder() {
      @Override
      protected void configure() {
        bindAsContract(TestClass.class).in(Singleton.class);
        bind(HK2ConfigurationInjector.class)
                .to(new TypeLiteral<InjectionResolver<ConfigProperty>>() {
                })
                .in(Singleton.class);
        bind(new JerseyMicroprofileConfigurationModel(
                (ConfigImpl) ConfigProvider.getConfig())).to(Configuration.class);
        bind(new JerseyMicroprofileConfigurationProvider()).to(JerseyMicroprofileConfigurationProvider.class);
      }

    });

    TestClass service = loc.getService(TestClass.class);
    Assert.assertEquals("bubu", service.getValue());
    Assert.assertNull(service.getValue2());
    System.setProperty("myProp2", "boooo");
    Assert.assertNull(service.getValue2()); // properties/envs are considered immutable configurations.
    M_CONFIG.putValue("myProp2", "boooo");
    Assert.assertEquals("boooo", service.getValue2());
    Assert.assertSame(service, loc.getService(TestClass.class));
    Assert.assertEquals(1, service.getEnvSys());
    loc.shutdown();
  }

  @Test
  public void testConfig() {
    Config config = ConfigProvider.getConfig();
    System.setProperty("testCfg", "val");
    String value = config.getValue("testCfg", String.class);
    Assert.assertEquals("val", value);
    String value2 = config.getValue("testCfgNonEx", String.class);
    Assert.assertNull(value2);
    System.setProperty("testIntList", "1,2,3");
    Assert.assertArrayEquals(new int[]{1, 2, 3}, config.getValue("testIntList", int[].class));
    System.setProperty("testDurationList", "P1D,P2D");
    Assert.assertArrayEquals(new Duration[]{Duration.parse("P1D"), Duration.parse("P2D")},
            config.getValue("testDurationList", Duration[].class));
  }

}
