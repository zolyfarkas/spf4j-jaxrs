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
package org.spf4j.http;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class DefaultDeadlineProtocolTest {


  @Test
  public void testDefaultDeadlineProtocol() {
    DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol("deadline", "timeout", 10, 10000000000L);
    ImmutableMap<String, String> hds = ImmutableMap.of("timeout", "1 S");
    long deadline = dp.deserialize(hds::get, 0);
    Assert.assertEquals(1000000000, deadline);
  }

  @Test
  public void testDefaultDeadlineProtocol2() {
    DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol("deadline", "timeout", 10, 10000000000L);
    ImmutableMap<String, String> hds = ImmutableMap.of("timeout", "1S");
    long deadline = dp.deserialize(hds::get, 0);
    Assert.assertEquals(1000000000, deadline);
  }

}
