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

import java.lang.reflect.Type;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Provider;

/**
 *
 * @author Zoltan Farkas
 */
class SimpleConfigSupplier implements Supplier, Provider {

  private final ConfigurationParam cfgParam;
  private final Type suppliedType;
  private final ExtendedConfig configuration;

  SimpleConfigSupplier(final ExtendedConfig configuration,
          final ConfigurationParam cfgParam, final Type suppliedType) {
    this.configuration = configuration;
    this.cfgParam = cfgParam;
    this.suppliedType = suppliedType;
  }

  public ConfigurationParam getCfgParam() {
    return cfgParam;
  }

  /**
   * Can be overwritten for advanced value retrieval implementations.
   * @return the property value.
   */
  @Override
  public Object get() {
    return fetch();
  }

  @Nullable
  public final Object fetch() {
    Object val = configuration.getValue(cfgParam.getPropertyName(), suppliedType, cfgParam.getDefaultValue());
    if (val == null)  {
        if (cfgParam.isNullable()) {
          return null;
        } else {
          throw new IllegalArgumentException("Unable to supply " + cfgParam + ", not nullable");
        }
    }
    return val;
  }

  @Override
  public String toString() {
    return "ConfigSupplier{, cfgParam=" + cfgParam
            + ", type=" + suppliedType + ", configuration=" + configuration + '}';
  }

}
