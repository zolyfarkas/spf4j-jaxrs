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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Feature;
import org.eclipse.microprofile.config.Config;
import org.glassfish.jersey.spi.ExternalConfigurationModel;

/**
 * @author Zoltan Farkas
 */
public final class JerseyMicroprofileConfigurationModel implements ExternalConfigurationModel<Config> {

  private final ConfigImpl config;

  private final ObjectConverters converters;

  private Map<String, Object> mergedProperties;


  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public JerseyMicroprofileConfigurationModel(final ConfigImpl config) {
    this.config =  config;
    this.mergedProperties = Collections.EMPTY_MAP;
    converters = config.getConverters();
  }

  public ObjectConverters getConverters() {
    return converters;
  }

  @Override
  public <T> T as(final String propertyName, final Class<T> targetType) {
    return config.getValue(propertyName, targetType);
  }

  @Override
  public <T> Optional<T> getOptionalProperty(final String propertyName, final Class<T> targetType) {
    return Optional.ofNullable(as(propertyName, targetType));
  }

  @Override
  public ExternalConfigurationModel mergeProperties(final Map<String, Object> properties) {
    if (mergedProperties.isEmpty()) {
      mergedProperties = new HashMap<>(properties);
    } else {
      this.mergedProperties.putAll(properties);
    }
    return this;
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ExtendedConfig getConfig() {
    return config;
  }

  @Override
  public boolean isProperty(final String propertyName) {
    if (mergedProperties.containsKey(propertyName)) {
      return true;
    }
    return config.getValue(propertyName, Object.class) != null;
  }

  @Override
  @Nullable
  public RuntimeType getRuntimeType() {
    return null;
  }

  @Override
  public Map<String, Object> getProperties() {
    Map<String, Object> properties = new HashMap<>();
    config.getPropertyNames().forEach(c -> properties.put(c, config.getValue(c, Object.class)));
    properties.putAll(mergedProperties);
    return properties;
  }

  @Override
  @Nullable
  public Object getProperty(final String propertyName) {
    Object val = mergedProperties.get(propertyName);
    if (val != null) {
      return val;
    }
    return config.getValue(propertyName, Object.class);
  }

  @Override
  public Collection<String> getPropertyNames() {
    final Set<String> names = new THashSet<>();
    config.getPropertyNames().forEach(names::add);
    names.addAll(mergedProperties.keySet());
    return names;
  }

  @Override
  public boolean isEnabled(final Feature feature) {
    return false;
  }

  @Override
  public boolean isEnabled(final Class<? extends Feature> featureClasz) {
    return false;
  }

  @Override
  public boolean isRegistered(final Object object) {
   return false;
  }

  @Override
  public boolean isRegistered(final Class<?> clasz) {
    return false;
  }

  @Override
  public Map<Class<?>, Integer> getContracts(final Class<?> clasz) {
        return Collections.EMPTY_MAP;
  }

  @Override
  public Set<Class<?>> getClasses() {
    return Collections.EMPTY_SET;
  }

  @Override
  public Set<Object> getInstances() {
    return Collections.EMPTY_SET;
  }

  @Override
  public String toString() {
    return "JerseyMicroprofileConfigurationModel{" + "config=" + config + '}';
  }

}
