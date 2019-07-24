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
package org.spf4j.actuator.health.checks;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import org.slf4j.Logger;
import org.spf4j.actuator.health.HealthCheck;

/**
 * @author Zoltan Farkas
 */
public final class DeadlockHealthCheck implements HealthCheck {

  @Override
  public void test(final Logger logger) throws Exception {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
    if (deadlockedThreads == null) {
      return;
    }
    ThreadInfo[] threadInfo = threadMXBean.getThreadInfo(deadlockedThreads, Integer.MAX_VALUE);
    throw new IllegalStateException("Deadlock detected: " + Arrays.toString(threadInfo));
  }

  @Override
  public String info() {
    return "Checks for deadlocked threads";
  }

  @Override
  public String toString() {
    return "DeadlockHealthCheck";
  }



}
