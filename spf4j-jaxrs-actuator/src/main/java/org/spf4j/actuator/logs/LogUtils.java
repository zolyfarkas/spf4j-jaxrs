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
package org.spf4j.actuator.logs;

import java.util.Comparator;
import java.util.PriorityQueue;
import org.spf4j.base.avro.LogRecord;

/**
 *
 * @author Zoltan Farkas
 */
public final class LogUtils {

  private LogUtils() { }

  public static final Comparator<LogRecord> TS_ORDER_DESC = new Comparator<LogRecord>() {
    @Override
    public int compare(final LogRecord o1, final LogRecord o2) {
      return o2.getTs().compareTo(o1.getTs());
    }
  };

  public static final Comparator<LogRecord> TS_ORDER_ASC = new Comparator<LogRecord>() {
    @Override
    public int compare(final LogRecord o1, final LogRecord o2) {
      return o1.getTs().compareTo(o2.getTs());
    }
  };

  public static void addAll(final int limit,
          final PriorityQueue<LogRecord> result,
          final Iterable<LogRecord> records) {
    for (LogRecord log : records) {
      result.add(log);
      while (result.size() > limit) {
        result.remove();
      }
    }
  }

}
