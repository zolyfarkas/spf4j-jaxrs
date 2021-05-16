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

import gnu.trove.set.hash.THashSet;
import java.util.Arrays;
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
public final class MergedConfigs implements Configuration {

  private Configuration[] cfgs;

  public MergedConfigs(final Configuration... cfgs) {
    this.cfgs = cfgs;
  }

  @Override
  public RuntimeType getRuntimeType() {
    return cfgs[0].getRuntimeType();
  }

  @Override
  public Map<String, Object> getProperties() {
    Map<String, Object> result = new HashMap<>(cfgs[cfgs.length - 1].getProperties());
    for (int i = cfgs.length - 1; i >= 0; i--) {
      result.putAll(cfgs[i].getProperties());
    }
    return result;
  }


  @Override
  public Object getProperty(final String name) {
    for (Configuration cfg : cfgs) {
      Object property = cfg.getProperty(name);
      if (property != null) {
        return property;
      }
    }
    return null;
  }

  @Override
  public Collection<String> getPropertyNames() {
    Set<String> result = new THashSet<>(cfgs[0].getPropertyNames());
    for (int i = 1; i < cfgs.length; i++) {
      result.addAll(cfgs[i].getPropertyNames());
    }
    return result;
  }

  @Override
  public boolean isEnabled(final Feature feature) {
    for (Configuration cfg : cfgs) {
      if (cfg.isEnabled(feature)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isEnabled(final Class<? extends Feature> featureClass) {
    for (Configuration cfg : cfgs) {
      if (cfg.isEnabled(featureClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isRegistered(final Object component) {
    for (Configuration cfg : cfgs) {
      if (cfg.isRegistered(component)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isRegistered(final Class<?> componentClass) {
    for (Configuration cfg : cfgs) {
      if (cfg.isRegistered(componentClass)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Map<Class<?>, Integer> getContracts(final Class<?> componentClass) {
    Map<Class<?>, Integer> result = new HashMap<>(cfgs[cfgs.length - 1].getContracts(componentClass));
    for (int i = cfgs.length - 1; i >= 0; i--) {
      result.putAll(cfgs[i].getContracts(componentClass));
    }
    return result;
  }

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> result = new THashSet<>(cfgs[0].getClasses());
    for (int i = 1; i < cfgs.length; i++) {
      result.addAll(cfgs[i].getClasses());
    }
    return result;
  }

  @Override
  public Set<Object> getInstances() {
    Set<Object> result = new THashSet<>(cfgs[0].getInstances());
    for (int i = 1; i < cfgs.length; i++) {
      result.addAll(cfgs[i].getInstances());
    }
    return result;
  }

  @Override
  public String toString() {
    return "MergedConfigs{" + "cfgs=" + Arrays.toString(cfgs) + '}';
  }



}
