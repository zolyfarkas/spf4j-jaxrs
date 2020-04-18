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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
final class ConfigurationParam {

  private final String propertyName;


  private final String defaultValue;

  ConfigurationParam(final String propertyName, @Nullable final String defaultValue) {
    this.propertyName = propertyName;
    this.defaultValue = ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue) ? null : defaultValue;
  }

  public String getPropertyName() {
    return propertyName;
  }

  @Nullable
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String toString() {
    return "ConfigurationParam{" + "propertyName=" + propertyName + ", defaultValue=" + defaultValue + '}';
  }


}
