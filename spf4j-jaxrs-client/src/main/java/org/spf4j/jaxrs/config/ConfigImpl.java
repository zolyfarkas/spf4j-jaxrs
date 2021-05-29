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

import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author Zoltan Farkas
 */
public final class ConfigImpl implements Config {

  private final ConfigSource[] configs;

  private final ObjectConverters converters;

  ConfigImpl(final ObjectConverters converters, final ConfigSource... configs) {
    this.converters = converters;
    this.configs = configs;
  }

  public ObjectConverters getConverters() {
    return converters;
  }

  @Override
  @Nullable
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public <T> T getValue(final String propertyName, final Class<T> clasz) {
    if (ObservableConfigSource.PROPERTY_NAME.equals(propertyName)) {
       List<ObservableConfigSource> sources = new java.util.ArrayList<>(2);
       for (ConfigSource source : configs) {
         if (source instanceof ObservableConfigSource) {
           sources.add((ObservableConfigSource) source);
         }
       }
       return (T) sources;
    }
    if (propertyName.startsWith("raw:")) {
      String actualProp = propertyName.substring(4);
      List<String> values = new ArrayList<>(2);
      for (ConfigSource source : configs) {
        String strValue = source.getValue(actualProp);
        if (strValue != null) {
          values.add(strValue);
        }
      }
      return (T) values;
    }
    String strValue = null;
    for (ConfigSource source : configs) {
      strValue = source.getValue(propertyName);
      if (strValue != null) {
        break;
      }
    }
    if (strValue == null) {
      return null;
    }
    if (String.class == clasz || Object.class == clasz) {
      return (T) strValue;
    }
    return (T) converters.get(clasz).apply(strValue, clasz);
  }

  @Override
  public <T> Optional<T> getOptionalValue(final String propertyName, final Class<T> clasz) {
    return Optional.ofNullable(getValue(propertyName, clasz));
  }

  @Override
  public Iterable<String> getPropertyNames() {
    Iterable<String>[] names = new Iterable[configs.length];
    int i = 0;
    for (ConfigSource source : configs) {
      names[i++] = source.getPropertyNames();
    }
    return Iterables.concat(names);
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return Collections.unmodifiableList(Arrays.asList(configs));
  }

  @Override
  public String toString() {
    return "ConfigImpl{configs=" + Arrays.toString(configs) + '}';
  }

}
