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
import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.ws.rs.core.Configuration;

/**
 *
 * @author Zoltan Farkas
 */
final class ConfigSupplier implements Supplier, Provider, AutoCloseable {

  private final BiFunction<Object, Type, Object> typeConv;
  private final ConfigurationParam cfgParam;
  private final Type type;
  private final Configuration configuration;
  private volatile Supplier<Object> value;
  private final PropertyWatcher propertyWatcher;
  private final List<ObservableConfigSource> cfgSource;

  ConfigSupplier(final Configuration configuration, final BiFunction<Object, Type, Object> typeConv,
          final ConfigurationParam cfgParam, final Type type) {
    this.configuration = configuration;
    this.typeConv = typeConv;
    this.cfgParam = cfgParam;
    this.type = type;
    this.cfgSource = (List<ObservableConfigSource>) configuration.getProperty(ObservableConfigSource.PROPERTY_NAME);
    this.propertyWatcher = new PropertyWatcher() {
          public void accept(final ConfigEvent event) {
            switch (event) {
              case ADDED:
              case MODIFIED:
                try {
                  ConfigSupplier.this.value = Suppliers.ofInstance(fetch());
                } catch (RuntimeException ex) {
                  Logger.getLogger(ConfigSupplier.class.getName())
                          .log(Level.SEVERE, ex, () -> "Cannot fetch config: " + cfgParam.getPropertyName());
                }
                break;
              case DELETED:
                ConfigSupplier.this.value = null;
                break;
              default:
                throw new IllegalStateException("Unsupported config event: " + event);
            }

          }

          public void unknownEvents() {
            try {
              ConfigSupplier.this.value = Suppliers.ofInstance(fetch());
            } catch (RuntimeException ex) {
              Logger.getLogger(ConfigSupplier.class.getName())
                      .log(Level.SEVERE, ex, () -> "Cannot fetch config: " + cfgParam.getPropertyName());
            }
          }
        };
    if (cfgSource != null && !cfgSource.isEmpty()) {
      this.value = Suppliers.ofInstance(fetch());
      for (ObservableConfigSource s : this.cfgSource) {
        s.addWatcher(cfgParam.getPropertyName(), this.propertyWatcher);
      }
    } else {
      ConfigSupplier.this.value = this::fetch;
    }
  }

  @Override
  public void close() {
    for (ObservableConfigSource s : this.cfgSource) {
      s.removeWatcher(cfgParam.getPropertyName(), this.propertyWatcher);
    }
  }

  @Override
  public Object get() {
    return value.get();
  }

  @Nullable
  private Object fetch() {
    Object val = configuration.getProperty(cfgParam.getPropertyName());
    if (val != null) {
      return typeConv.apply(val, type);
    } else {
      String dval = cfgParam.getDefaultValue();
      if (dval != null) {
        return typeConv.apply(dval, type);
      } else {
        if (cfgParam.isNullable()) {
          return null;
        } else {
          throw new IllegalArgumentException("Unable to supply " + cfgParam + ", not nullable");
        }
      }
    }
  }

  @Override
  public String toString() {
    return "ConfigSupplier{" + "typeConv=" + typeConv + ", cfgParam=" + cfgParam
            + ", type=" + type + ", configuration=" + configuration + '}';
  }

}
