/*
 * Copyright 2019 SPF4J.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

/**
 * A configuration that will retrieve the wrapped config and fallback
 * to system properties and environment variables.
 * @author Zoltan Farkas
 */
public final class SystemConfiguration implements Configuration {

  private Configuration cfg;

  public SystemConfiguration(final Configuration cfg) {
    this.cfg = cfg;
  }

  @Override
  public RuntimeType getRuntimeType() {
    return cfg.getRuntimeType();
  }

  @Override
  public Map<String, Object> getProperties() {
    Map<String, Object> result = new HashMap<>(cfg.getProperties());
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      result.putIfAbsent(entry.getKey().toString(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      result.putIfAbsent(entry.getKey(), entry.getValue());
    }
    return result;
  }


  @Override
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public Object getProperty(final String name) {
    Object result = cfg.getProperty(name);
    if (result != null) {
      return result;
    }
    result = System.getProperty(name);
    if (result != null) {
      return result;
    }
    return System.getenv(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    Set<String> result = new THashSet<>(cfg.getProperties().keySet());
    for (Object key : System.getProperties().keySet()) {
      result.add(key.toString());
    }
    result.addAll(System.getenv().keySet());
    return result;
  }

  @Override
  public boolean isEnabled(final Feature feature) {
    return cfg.isEnabled(feature);
  }

  @Override
  public boolean isEnabled(final Class<? extends Feature> featureClass) {
    return cfg.isEnabled(featureClass);
  }

  @Override
  public boolean isRegistered(final Object component) {
    return cfg.isRegistered(component);
  }

  @Override
  public boolean isRegistered(final Class<?> componentClass) {
    return cfg.isRegistered(componentClass);
  }

  @Override
  public Map<Class<?>, Integer> getContracts(final Class<?> componentClass) {
    return cfg.getContracts(componentClass);
  }

  @Override
  public Set<Class<?>> getClasses() {
    return cfg.getClasses();
  }

  @Override
  public Set<Object> getInstances() {
    return cfg.getInstances();
  }

  @Override
  public String toString() {
    return "SystemConfiguration{" + "cfg=" + cfg + '}';
  }

}
