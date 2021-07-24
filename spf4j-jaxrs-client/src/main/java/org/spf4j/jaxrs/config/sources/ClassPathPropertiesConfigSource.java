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
package org.spf4j.jaxrs.config.sources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 * @author Zoltan Farkas
 */
public final class ClassPathPropertiesConfigSource implements ConfigSource {

  private final Properties properties;


  public ClassPathPropertiesConfigSource(final String propertyFileName,
          final ClassLoader loader, final Charset charset) {
    properties = new Properties();
    Enumeration<URL> resources;
    try {
      resources = loader.getResources(propertyFileName);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    while (resources.hasMoreElements()) {
      try (InputStream stream = resources.nextElement().openStream();
              Reader reader = new InputStreamReader(stream, charset)) {
        properties.load(reader);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  public ClassPathPropertiesConfigSource(final ClassLoader cl) {
    this("META-INF/microprofile-config.properties", cl,
            StandardCharsets.UTF_8);
  }

  public ClassPathPropertiesConfigSource() {
    this("META-INF/microprofile-config.properties", Thread.currentThread().getContextClassLoader(),
            StandardCharsets.UTF_8);
  }

  @Override
  public Map<String, String> getProperties() {
    return (Map) properties;
  }

  @Override
  public Set<String> getPropertyNames() {
    return (Set) properties.keySet();
  }

  @Override
  public int getOrdinal() {
    return 300;
  }

  @Override
  public String getValue(final String propertyName) {
    return properties.getProperty(propertyName);
  }

  @Override
  public String getName() {
    return ClassPathPropertiesConfigSource.class.getName();
  }

  @Override
  public String toString() {
    return "ClassPathPropertiesConfigSource{" + "properties=" + properties + '}';
  }

}
