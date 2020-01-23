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
package org.spf4j.grizzly;

import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author Zoltan Farkas
 */
public interface JvmServices extends AutoCloseable {

  String getApplicationName();

  String getHostName();

  String getLogFolder();

  Sampler getProfiler();

  static JvmServices current() {
    JvmServices services = JvmServicesBuilder.services;
    if (services == null) {
      throw new IllegalStateException("No application services initialized");
    }
    return services;
  }
}
