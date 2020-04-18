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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 *
 * @author Zoltan Farkas
 */
final class ConfigProviderResolverImpl extends ConfigProviderResolver {

  private final ConcurrentMap<ClassLoader, Config> configs = new ConcurrentHashMap<>();

  ConfigProviderResolverImpl() {
    configs.put(Thread.currentThread().getContextClassLoader(),
            new ConfigBuilderImpl().addDefaultSources().addDiscoveredSources().addDiscoveredConverters().build());
  }

  @Override
  public Config getConfig() {
    return configs.get(Thread.currentThread().getContextClassLoader());
  }

  @Override
  public Config getConfig(final ClassLoader arg) {
    Config result = configs.get(arg);
    ClassLoader parent = arg;
    while (result == null && (parent = parent.getParent()) !=  null) {
      result = configs.get(parent);
    }
    return result;
  }

  @Override
  public ConfigBuilder getBuilder() {
    return new ConfigBuilderImpl();
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
  }

  @Override
  public String toString() {
    return "ConfigProviderResolverImpl{" + "configs=" + configs + '}';
  }
}
