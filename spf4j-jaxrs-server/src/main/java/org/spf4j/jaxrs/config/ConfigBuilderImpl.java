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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.spf4j.io.ObjectAppender;
import org.spf4j.jaxrs.config.sources.ClassPathPropertiesConfigSource;
import org.spf4j.jaxrs.config.sources.DirConfigMapConfigSource;
import org.spf4j.jaxrs.config.sources.EnvConfigSource;
import org.spf4j.jaxrs.config.sources.SysPropConfigSource;

/**
 *
 * @author Zoltan Farkas
 */
public final class ConfigBuilderImpl implements ConfigBuilder {

  private final List<ConfigSource> sources;

  private final Map<Type, SortedMap<Integer, Converter<?>>> converters;

  private ClassLoader cl;

  public ConfigBuilderImpl() {
    sources = new ArrayList<>(4);
    converters = new HashMap<>();
    cl = Thread.currentThread().getContextClassLoader();
  }

  @Override
  public ConfigBuilder addDefaultSources() {
    sources.add(new SysPropConfigSource());
    sources.add(new EnvConfigSource());
    sources.add(new DirConfigMapConfigSource());
    sources.add(new ClassPathPropertiesConfigSource(cl));
    return this;
  }

  @Override
  public ConfigBuilder addDiscoveredSources() {
    ServiceLoader<ConfigSource> loader = ServiceLoader.load(ConfigSource.class);
    for (ConfigSource source : loader) {
      sources.add(source);
    }
    return this;
  }

  @Override
  public ConfigBuilder addDiscoveredConverters() {
    ServiceLoader<Converter> loader = ServiceLoader.load(Converter.class);
    for (Converter converter : loader) {
      addConverter(converter);
    }
    return this;
  }

  private void addConverter(final Converter converter) {
    Type type = getConverterType(converter);
    converters.compute(type, (k, v) -> {
      SortedMap<Integer, Converter<?>> result;
      if (v == null) {
        result = new TreeMap<>();
      } else {
        result = v;
      }
      Converter<?> ex = result.put(getPriority(converter), converter);
      if (ex != null) {
        Logger.getLogger(ConfigBuilderImpl.class.getName()).log(Level.WARNING,
                "Converter {0} ignored due to dupe {1}", new Object[]{ex, converter});
      }
      return result;
    });
  }

  private static int getPriority(final Object obj) {
    Priority annotation = obj.getClass().getAnnotation(Priority.class);
    if (annotation == null) {
      return 100;
    }
    return annotation.value();
  }

  private static Type getConverterType(final Converter converter) {
    Class<?> aClass = converter.getClass();
    Class<?> converterType = null;
    do {
      Type[] genericInterfaces = aClass.getGenericInterfaces();
      for (Type type : genericInterfaces) {
        if (type instanceof ParameterizedType) {
          ParameterizedType pType = (ParameterizedType) type;
          if (pType.getRawType() == ObjectAppender.class) {
            Type actualTypeArgument = pType.getActualTypeArguments()[0];
            if (actualTypeArgument instanceof ParameterizedType) {
              converterType = (Class) ((ParameterizedType) actualTypeArgument).getRawType();
            } else {
              converterType = (Class) actualTypeArgument;
            }
            break;
          }
        }
      }
      aClass = aClass.getSuperclass();
    } while (converterType == null && aClass != null);
    if (converterType == null) {
      return Object.class;
    }
    return converterType;
  }

  @Override
  public ConfigBuilder forClassLoader(final ClassLoader ncl) {
    this.cl = ncl;
    return this;
  }

  @Override
  public ConfigBuilder withSources(final ConfigSource... psources) {
    for (ConfigSource source : psources) {
      this.sources.add(source);
    }
    return this;
  }

  @Override
  public ConfigBuilder withConverters(final Converter<?>... pconverters) {
    for (Converter converter : pconverters) {
      addConverter(converter);
    }
    return this;
  }

  @Override
  public <T> ConfigBuilder withConverter(final Class<T> type, final int priority, final Converter<T> converter) {
    converters.compute(type, (k, v) -> {
      SortedMap<Integer, Converter<?>> result;
      if (v == null) {
        result = new TreeMap<>();
      } else {
        result = v;
      }
      Converter<?> ex = result.put(priority, converter);
      if (ex != null) {
        Logger.getLogger(ConfigBuilderImpl.class.getName()).log(Level.WARNING,
                "Converter {0} ignored due to dupe {1}, use different priorities", new Object[] {ex, converter});
      }
      return result;
    });
    return this;
  }

  @Override
  public Config build() {
   Collections.sort(sources, (a, b) -> a.getOrdinal() - b.getOrdinal());
   return new ConfigImpl(new ObjectConverters(converters), sources.toArray(new ConfigSource[sources.size()]));
  }

  @Override
  public String toString() {
    return "ConfigBuilderImpl{" + "sources=" + sources + ", converters=" + converters + ", cl=" + cl + '}';
  }

}