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

import org.apache.avro.SchemaResolver;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.spf4j.jaxrs.config.sources.MemoryConfigSource;

/**
 * @author Zoltan Farkas
 */
public final class MemoryConfig {

  private MemoryConfig() { }

  private static final MemoryConfigSource M_CONFIG = new MemoryConfigSource();

  public static void init() {
    ConfigProviderResolver.setInstance(new ConfigProviderResolverImpl(SchemaResolver.NONE,
            new ConfigBuilderImpl(SchemaResolver.NONE).addDefaultSources().withSources(M_CONFIG).build()));
  }

  public static void resetToDefault() {
    ConfigProviderResolver.setInstance(null);
  }

  public static void put(final String key, final String value) {
    M_CONFIG.putValue(key, value);
  }

  public static void clear() {
    M_CONFIG.clear();
  }

}
