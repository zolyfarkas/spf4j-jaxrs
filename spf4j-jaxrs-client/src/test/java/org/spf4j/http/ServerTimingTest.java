/*
 * Copyright 2023 SPF4J.
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

import org.junit.Assert;
import org.junit.Test;
import org.spf4j.http.ServerTiming.ServerTimingMetric;
import org.spf4j.http.ServerTiming.ServerTimingParameter;

/**
 *
 * @author Zoltan Farkas
 */
public class ServerTimingTest {

  @Test
  public void testServerTimingParameter() {
    ServerTimingParameter param = new ServerTimingParameter("dur", "123");
    String strVal = param.toString();
    Assert.assertEquals("dur=123", strVal);
    ServerTimingParameter param2 = ServerTimingParameter.parse(strVal);
    Assert.assertEquals(param, param2);
  }

  @Test
  public void testServerTimingParameterQuoted() {
    ServerTimingParameter param = new ServerTimingParameter("description", "A 123");
    String strVal = param.toString();
    Assert.assertEquals("description=\"A 123\"", strVal);
    ServerTimingParameter param2 = ServerTimingParameter.parse(strVal);
    Assert.assertEquals(param, param2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testServerTimingParameternInvalid() {
    new ServerTimingParameter("description ", "A 123");
  }

  @Test
  public void testServerTimingMetric() {
    ServerTimingMetric param = new ServerTimingMetric("server_time", 123.5, "server side timing in millis");
    String strVal = param.toString();
    Assert.assertEquals("server_time;dur=123.5;description=\"server side timing in millis\"", strVal);
    ServerTimingMetric param2 = ServerTimingMetric.parse(strVal);
    Assert.assertEquals(param, param2);
  }

  @Test
  public void testServerTimingMetricEmpty() {
    ServerTimingMetric param = new ServerTimingMetric("server_time", null, "");
    String strVal = param.toString();
    Assert.assertEquals("server_time", strVal);
    ServerTimingMetric param2 = ServerTimingMetric.parse(strVal);
    Assert.assertEquals(param, param2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testServerTimingMetricInvalid() {
    new ServerTimingMetric("description ", null, null);
  }

  @Test
  public void testServerTiming() {
    ServerTiming testTiming = new ServerTiming(new ServerTimingMetric("server_time_total", 10.5, ""),
            new ServerTimingMetric("sql_time", 5.3, "database access time"));
    String strVal = testTiming.toString();
    ServerTiming testTiming2 = ServerTiming.valueOf(strVal);
    Assert.assertEquals(testTiming, testTiming2);
  }

  @Test
  public void testServerTimingSingle() {
    ServerTiming testTiming = new ServerTiming(new ServerTimingMetric("server_time_total", 10.5, ""));
    String strVal = testTiming.toString();
    ServerTiming testTiming2 = ServerTiming.valueOf(strVal);
    Assert.assertEquals(testTiming, testTiming2);
  }


  @Test(expected = IllegalArgumentException.class)
  public void testServerTimingIllegal() {
    ServerTiming.valueOf("");
  }


}
