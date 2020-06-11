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

import java.time.Instant;
import java.util.Collections;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.avro.LogLevel;
import org.spf4j.base.avro.LogRecord;

/**
 *
 * @author Zoltan Farkas
 */
public class LogsResourceTest {

  @Test
  public void testLogExpr() {
    Function<LogRecord, Comparable> fieldExtractor = LogsResource.getFieldExtractor("log.ts");
    Instant now = Instant.now();
    LogRecord rec = new LogRecord("test", "testId",
            LogLevel.DEBUG, now, "test", "testThrr", "bla", Collections.EMPTY_LIST,
            Collections.EMPTY_MAP, null, Collections.EMPTY_LIST);
    Instant ts = (Instant) fieldExtractor.apply(rec);
    Assert.assertEquals(now, ts);
  }

}
