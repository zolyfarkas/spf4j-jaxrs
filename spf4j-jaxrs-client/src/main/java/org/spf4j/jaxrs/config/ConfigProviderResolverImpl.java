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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.avro.SchemaResolver;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.spf4j.base.Throwables;

public final class ConfigProviderResolverImpl extends ConfigProviderResolver implements AutoCloseable {

  private final SchemaResolver schemaResolver;

  private final ConcurrentMap<ClassLoader, Config> configs = new ConcurrentHashMap<>();

  public ConfigProviderResolverImpl() {
    this(SchemaResolver.NONE);
  }

  public ConfigProviderResolverImpl(final SchemaResolver schemaResolver) {
    this(schemaResolver, new ConfigBuilderImpl(schemaResolver).addDefaultSources()
            .addDiscoveredSources().addDiscoveredConverters().build());
  }

  public ConfigProviderResolverImpl(final SchemaResolver schemaResolver, final Config config) {
    this.schemaResolver = schemaResolver;
    configs.put(Thread.currentThread().getContextClassLoader(), config);
  }

  @Override
  public Config getConfig() {
    return configs.get(Thread.currentThread().getContextClassLoader());
  }

  @Override
  public Config getConfig(final ClassLoader arg) {
    Config result = configs.get(arg);
    ClassLoader parent = arg;
    while (result == null && (parent = parent.getParent()) != null) {
      result = configs.get(parent);
    }
    return result;
  }

  @Override
  public ConfigBuilder getBuilder() {
    return new ConfigBuilderImpl(schemaResolver);
  }

  @Override
  public void registerConfig(final Config cfg, final ClassLoader cl) {
    configs.put(cl, cfg);
  }

  @Override
  public void releaseConfig(final Config cfg) {
    Iterator<Map.Entry<ClassLoader, Config>> it = configs.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<ClassLoader, Config> entry = it.next();
      if (entry.getValue().equals(cfg)) {
        it.remove();
      }
    }
    if (cfg instanceof AutoCloseable) {
      try {
        ((AutoCloseable) cfg).close();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public String toString() {
    return "ConfigProviderResolverImpl{" + "configs=" + configs + '}';
  }

  @Override
  public void close() throws Exception {
    Exception ex = null;
    for (Config config : configs.values()) {
      if (config instanceof AutoCloseable) {
        try {
          ((AutoCloseable) config).close();
        } catch (Exception ex1) {
          if (ex != null) {
            Throwables.suppressLimited(ex1, ex);
          }
          ex = ex1;
        }
      }
    }
    if (ex != null) {
      throw ex;
    }
  }
}
