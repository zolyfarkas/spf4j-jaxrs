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
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Either;

/**
 * a more advanced config supplier implementation, where value type conversion and update happens asybchronously
 * reducing the cost of reading the config. These should be used for Singleton scoped services.
 *
 * @author Zoltan Farkas
 */
final class ObservableRXConfigSupplier extends SimpleConfigSupplier implements ObservableSupplier {

  private static final Logger LOG = LoggerFactory.getLogger(ObservableRXConfigSupplier.class);

  private volatile Supplier<Object> value;
  private final PropertyWatcher propertyWatcher;
  private final List<PropertyWatcher> watchers;
  private final ExtendedConfig configuration;

  ObservableRXConfigSupplier(final ExtendedConfig configuration,
          final ConfigurationParam cfgParam, final Either<Type, Function<String, ?>> typeOrConverter) {
    super(configuration, cfgParam, typeOrConverter.isLeft() ? typeOrConverter.getLeft() : String.class);
    this.value = null;
    this.watchers = new CopyOnWriteArrayList<>();
    this.configuration = configuration;
    this.propertyWatcher = new PropertyWatcher() {

      private Object convert(final Object initial) {
        if (initial == null) {
          return null;
        }
        if (typeOrConverter.isLeft()) {
          return initial;
        } else {
          return typeOrConverter.getRight().apply((String) initial);
        }
      }

      public synchronized void accept(final ConfigEvent event) {
        switch (event) {
          case ADDED:
          case MODIFIED:
            updateValue();
            break;
          case DELETED:
            setDefaultValue(cfgParam, typeOrConverter, configuration);
            break;
          default:
            throw new IllegalStateException("Unsupported config event: " + event);
        }
        for (PropertyWatcher watcher : watchers) {
          watcher.accept(event);
        }
      }

      public synchronized void unknownEvents() {
        updateValue();
        for (PropertyWatcher watcher : watchers) {
          watcher.unknownEvents();
        }
      }

      private void updateValue() {
        try {
          Object fetchedConfig = convert(fetch());
          if (fetchedConfig != null) {
            ObservableRXConfigSupplier.this.value = Suppliers.ofInstance(fetchedConfig);
            LOG.info("{} <- {}", ObservableRXConfigSupplier.this, fetchedConfig);
          } else {
            setDefaultValue(cfgParam, typeOrConverter, configuration);
          }
        } catch (RuntimeException ex) {
          LOG.error("Cannot fetch config {} for {}", cfgParam.getPropertyName(), ObservableRXConfigSupplier.this, ex);
        }
      }

      @Override
      public void close() {
        ObservableRXConfigSupplier.this.value = null;
      }
    };
 //   setDefaultValue(cfgParam, typeOrConverter, configuration);
    configuration.addWatcher(cfgParam.getPropertyName(), this.propertyWatcher);
    if (this.value == null) {
      setDefaultValue(cfgParam, typeOrConverter, configuration);
    }
  }


  private void setDefaultValue(final ConfigurationParam cfgParam,
          final Either<Type, Function<String, ?>> typeOrConverter, final ExtendedConfig configuration1) {
    String defVal = cfgParam.getDefaultValue();
    Object defaultValue;
    if (defVal != null) {
      if (typeOrConverter.isLeft()) {
        defaultValue = configuration1.convert(typeOrConverter.getLeft(), defVal);
      } else {
        defaultValue = typeOrConverter.getRight().apply(defVal);
      }
      this.value = Suppliers.ofInstance(defaultValue);
      LOG.info("{} <- {} (default)", this, defaultValue);
    } else {
      this.value = Suppliers.ofInstance(null);
      LOG.info("{} <- null", this);
    }
  }

  @Override
  public void close() {
    this.configuration.removeWatcher(getCfgParam().getPropertyName(), this.propertyWatcher);
  }

  @Override
  public Object get() {
    Supplier<Object> supp = value;
    if (supp == null) {
      throw new UnsupportedOperationException("Attempt to read " + this + " after config is closed");
    }
    return supp.get();
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
