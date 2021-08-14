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
import gnu.trove.set.hash.THashSet;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class ConfigImpl implements ExtendedConfig {

  private final ConfigSource[] configs;

  private final ObjectConverters converters;

  private final ObservableConfig observableConfig;

  ConfigImpl(final ObjectConverters converters, final ConfigSource... configs) {
    this.converters = converters;
    this.configs = configs;
    if (configs.length > 0) {
      ConfigSource first =  configs[0];
      if (first instanceof ObservableConfig) {
         this.observableConfig = (ObservableConfig) first;
      } else {
        this.observableConfig = null;
      }
      for (int i = 1; i < configs.length; i++) {
        ConfigSource cs = configs[i];
        if (cs instanceof ObservableConfig) {
          throw new IllegalArgumentException("You should not have more than 2 observable configs in your setup: "
                  + Arrays.toString(configs));
        }
      }
    } else {
       this.observableConfig = null;
    }
  }

  public ObjectConverters getConverters() {
    return converters;
  }

  @Override
  @Nullable
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public <T> T getValue(final String propertyName, final Class<T> clasz) {
    return (T) getValue(propertyName, clasz, null);
  }

  @Override
  @Nullable
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public Object getValue(final String propertyName, final Type propertyType, @Nullable final String defaultValue) {
    if (ExtendedConfig.PROPERTY_NAME.equals(propertyName)) {
      return this;
    }
    Object value = getCfgValue(propertyName, propertyType);
    if (value == null) {
      value = defaultValue == null ? null : convert(propertyType, defaultValue);
    }
    return value;
  }

  @Nullable
  private Object getCfgValue(final String propertyName, final Type type) {
    String strValue = getCfgStrValue(propertyName);
    if (strValue == null) {
      return null;
    }
    return convert(type, strValue);
  }

  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  @Override
  public Object convert(final Type type, final String strValue) {
    if (String.class == type || Object.class == type) {
      return strValue;
    }
    return converters.get(type).apply(strValue, type);
  }

  @Nullable
  private String getCfgStrValue(final String propertyName) {
    String strValue = null;
    for (ConfigSource source : configs) {
      strValue = source.getValue(propertyName);
      if (strValue != null) {
        break;
      }
    }
    return strValue;
  }

  @Override
  public <T> Optional<T> getOptionalValue(final String propertyName, final Class<T> clasz) {
    return Optional.ofNullable(getValue(propertyName, clasz));
  }

  @Override
  public Iterable<String> getPropertyNames() {
    Set<String> result = new THashSet<>(64);
    for (ConfigSource source : configs) {
      result.addAll(source.getPropertyNames());
    }
    return result;
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return Collections.unmodifiableList(Arrays.asList(configs));
  }

  @Override
  public void addWatcher(final ConfigWatcher consumer) {
    if (observableConfig != null) {
      observableConfig.addWatcher(consumer);
    } else {
      consumer.unknownEvents();
    }
  }

  @Override
  public void addWatcher(final String name, final PropertyWatcher consumer) {
    if (observableConfig != null) {
      observableConfig.addWatcher(name, consumer);
    } else {
      consumer.unknownEvents();
    }
  }

  @Override
  public void removeWatcher(final ConfigWatcher consumer) {
    if (observableConfig != null) {
      observableConfig.removeWatcher(consumer);
    }
  }

  @Override
  public void removeWatcher(final String name, final PropertyWatcher consumer) {
    if (observableConfig != null) {
      observableConfig.removeWatcher(name, consumer);
    }
  }

  @Override
  public String toString() {
    return "ConfigImpl{configs=" + Arrays.toString(configs) + '}';
  }

}
