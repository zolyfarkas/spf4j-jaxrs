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

import org.spf4j.log.LogbackService;
import org.spf4j.perf.ProcessVitals;
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

  ProcessVitals getVitals();

  LogbackService getLoggingService();

  @Override
  default void close() {
    try {
      getProfiler().stop();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    } finally {
      try {
        getVitals().close();
      } finally {
        getLoggingService().stop();
      }
    }
  }

  default JvmServices start() {
    getLoggingService().start();
    getProfiler().start();
    getVitals().start();
    return this;
  }

  default JvmServices closeOnShutdown() {
    org.spf4j.base.Runtime.queueHookAtEnd(() -> {
      try {
        this.close();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    });
    return this;
  }

}
