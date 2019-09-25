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
package org.spf4j.hk2;

import javax.inject.Inject;
import javax.inject.Provider;
import org.spf4j.jaxrs.NoConfiguration;
import javax.inject.Singleton;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.annotations.Service;
import org.spf4j.jaxrs.SystemConfiguration;


/**
 *
 * @author Zoltan Farkas
 */
public class ConfigurationInjectorTest {


 @Service
 public static final class TestClass {
   private final String value;

   private final Provider<String> providedValue;

    @Inject
    public TestClass(@ConfigProperty(name = "myProp", defaultValue = "bubu") final  String value,
            @ConfigProperty(name = "myProp2") final Provider<String> providedValue) {
      this.value = value;
      this.providedValue = providedValue;
    }

    public String getValue() {
      return value;
    }

    public String getValue2() {
      return providedValue.get();
    }


 }


  @Test
  public void testConfigInjection() {
    ServiceLocator loc = ServiceLocatorFactory.getInstance().create("test");
    ServiceLocatorUtilities.bind(loc, new AbstractBinder() {
      @Override
      protected void configure() {
        bindAsContract(TestClass.class);
        bind(HK2ConfigurationInjector.class)
            .to(new TypeLiteral<InjectionResolver<ConfigProperty>>() { })
            .in(Singleton.class);
        bind(new SystemConfiguration(new NoConfiguration(RuntimeType.SERVER))).to(Configuration.class);
      }

    });

   TestClass service = loc.getService(TestClass.class);
   Assert.assertEquals("bubu", service.getValue());
   Assert.assertNull(service.getValue2());
   System.setProperty("myProp2", "boooo");
   Assert.assertEquals("boooo", service.getValue2());
   loc.shutdown();
  }

}
