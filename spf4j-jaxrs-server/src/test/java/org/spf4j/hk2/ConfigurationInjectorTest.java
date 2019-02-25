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

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Configuration;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.annotations.Service;
import org.spf4j.jaxrs.ConfigProperty;


/**
 *
 * @author Zoltan Farkas
 */
public class ConfigurationInjectorTest {


 @Service
 public static final class TestClass {
   private final String value;

    public TestClass(@ConfigProperty("myProp")  @DefaultValue("bubu") String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }


 }


  @Test
  public void testSomeMethod() {
    ServiceLocator loc = ServiceLocatorFactory.getInstance().create("test");
    ServiceLocatorUtilities.bind(loc, new AbstractBinder() {
      @Override
      protected void configure() {
        bindAsContract(TestClass.class);
        bind(ConfigurationInjector.class)
            .to(new TypeLiteral<InjectionResolver<ConfigProperty>>() { })
            .in(Singleton.class);
        bind(NoConfiguration.class).to(Configuration.class);
      }

    });

   TestClass service = loc.getService(TestClass.class);
   Assert.assertEquals("bubu", service.getValue());
  }

}
