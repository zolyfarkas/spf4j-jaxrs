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
package org.spf4j.jaxrs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

/**
 *
 * @author Zoltan Farkas
 */
public final class NoConfiguration implements Configuration {

  private final RuntimeType runtimeType;

  public NoConfiguration(final RuntimeType runtimeType) {
    this.runtimeType = runtimeType;
  }

  @Override
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.EMPTY_MAP;
  }

  @Override
  @Nullable
  public Object getProperty(final String name) {
    return null;
  }

  @Override
  public Collection<String> getPropertyNames() {
    return Collections.EMPTY_SET;
  }

  @Override
  public boolean isEnabled(final Feature feature) {
    return false;
  }

  @Override
  public boolean isEnabled(final Class<? extends Feature> featureClass) {
    return false;
  }

  @Override
  public boolean isRegistered(final Object component) {
    return false;
  }

  @Override
  public boolean isRegistered(final Class<?> componentClass) {
    return false;
  }

  @Override
  public Map<Class<?>, Integer> getContracts(final Class<?> componentClass) {
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
    return "NoConfiguration{" + "runtimeType=" + runtimeType + '}';
  }

}
