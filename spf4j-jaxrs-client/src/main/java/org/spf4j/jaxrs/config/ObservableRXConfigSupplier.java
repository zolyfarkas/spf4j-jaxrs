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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a more advanced config supplier implementation, where value type conversion and update happens asybchronously
 * reducing the cost of reading the config.
 * These should be used  for Singleton scoped services.
 * @author Zoltan Farkas
 */
final class ObservableRXConfigSupplier extends SimpleConfigSupplier implements ObservableSupplier {


  private volatile Supplier<Object> value;
  private final PropertyWatcher propertyWatcher;
  private final List<PropertyWatcher> watchers;
  private final ExtendedConfig configuration;

  ObservableRXConfigSupplier(final ExtendedConfig configuration,
          final ConfigurationParam cfgParam, final Type type) {
    super(configuration, cfgParam, type);
    this.watchers = new CopyOnWriteArrayList<>();
    this.configuration = configuration;
    this.propertyWatcher = new PropertyWatcher() {
          public synchronized void accept(final ConfigEvent event) {
            switch (event) {
              case ADDED:
              case MODIFIED:
                try {
                  ObservableRXConfigSupplier.this.value = Suppliers.ofInstance(fetch());
                } catch (RuntimeException ex) {
                  Logger.getLogger(ObservableRXConfigSupplier.class.getName())
                          .log(Level.SEVERE, ex, () -> "Cannot fetch config: " + cfgParam.getPropertyName());
                }
                break;
              case DELETED:
                ObservableRXConfigSupplier.this.value = null;
                break;
              default:
                throw new IllegalStateException("Unsupported config event: " + event);
            }
            for (PropertyWatcher watcher : watchers) {
              watcher.accept(event);
            }
          }

          public synchronized void unknownEvents() {
            try {
              ObservableRXConfigSupplier.this.value = Suppliers.ofInstance(fetch());
            } catch (RuntimeException ex) {
              Logger.getLogger(ObservableRXConfigSupplier.class.getName())
                      .log(Level.SEVERE, ex, () -> "Cannot fetch config: " + cfgParam.getPropertyName());
            }
            for (PropertyWatcher watcher : watchers) {
              watcher.unknownEvents();
            }
          }
        };
        configuration.addWatcher(cfgParam.getPropertyName(), this.propertyWatcher);
        this.value = Suppliers.ofInstance(null);
  }

  @Override
  public void close() {
    this.configuration.removeWatcher(getCfgParam().getPropertyName(), this.propertyWatcher);
  }

  @Override
  public Object get() {
    return value.get();
  }

  @Override
  public void add(final PropertyWatcher watcher) {
    watchers.add(watcher);
    watcher.unknownEvents();
  }

  @Override
  public boolean remove(final PropertyWatcher watcher) {
    return watchers.remove(watcher);
  }

}
