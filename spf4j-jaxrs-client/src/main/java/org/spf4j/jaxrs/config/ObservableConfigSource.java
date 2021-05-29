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

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author Zoltan Farkas
 */
public interface ObservableConfigSource extends ConfigSource {

  /**
   * A Observable configuration source will be able to return a instance of itself
   */
  String PROPERTY_NAME = "_OBS_CFG_SOURCE";

  void addWatcher(ConfigWatcher consumer);

  void addWatcher(String name, PropertyWatcher consumer);

  void removeWatcher(ConfigWatcher consumer);
  
  void removeWatcher(String name, PropertyWatcher consumer);


}
