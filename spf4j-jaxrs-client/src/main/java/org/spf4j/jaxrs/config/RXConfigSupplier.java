/*
 * Copyright 2021 SPF4J.
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

import com.google.common.base.Suppliers;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Configuration;

/**
 * a more advanced config supplier implementation, where value type conversion and update happens asybchronously
 * reducing the cost of reading the config.
 * These should be used  for Singleton scoped services.
 * @author Zoltan Farkas
 */
final class RXConfigSupplier extends SimpleConfigSupplier implements AutoCloseable {


  private volatile Supplier<Object> value;
  private final PropertyWatcher propertyWatcher;
  private final List<ObservableConfigSource> cfgSource;

  RXConfigSupplier(final Configuration configuration, final BiFunction<Object, Type, Object> typeConv,
          final ConfigurationParam cfgParam, final Type type) {
    super(configuration, typeConv, cfgParam, type);
    this.cfgSource = (List<ObservableConfigSource>) configuration.getProperty(ObservableConfigSource.PROPERTY_NAME);
    this.propertyWatcher = new PropertyWatcher() {
          public void accept(final ConfigEvent event) {
            switch (event) {
              case ADDED:
              case MODIFIED:
                try {
                  RXConfigSupplier.this.value = Suppliers.ofInstance(fetch());
                } catch (RuntimeException ex) {
                  Logger.getLogger(RXConfigSupplier.class.getName())
                          .log(Level.SEVERE, ex, () -> "Cannot fetch config: " + cfgParam.getPropertyName());
                }
                break;
              case DELETED:
                RXConfigSupplier.this.value = null;
                break;
              default:
                throw new IllegalStateException("Unsupported config event: " + event);
            }

          }

          public void unknownEvents() {
            try {
              RXConfigSupplier.this.value = Suppliers.ofInstance(fetch());
            } catch (RuntimeException ex) {
              Logger.getLogger(RXConfigSupplier.class.getName())
                      .log(Level.SEVERE, ex, () -> "Cannot fetch config: " + cfgParam.getPropertyName());
            }
          }
        };
    if (cfgSource != null && !cfgSource.isEmpty()) {
      for (ObservableConfigSource s : this.cfgSource) {
        s.addWatcher(cfgParam.getPropertyName(), this.propertyWatcher);
      }
      this.value = Suppliers.ofInstance(fetch());
    } else {
      RXConfigSupplier.this.value = this::fetch;
    }
  }

  @Override
  public void close() {
    for (ObservableConfigSource s : this.cfgSource) {
      s.removeWatcher(getCfgParam().getPropertyName(), this.propertyWatcher);
    }
  }

  @Override
  public Object get() {
    return value.get();
  }

}
