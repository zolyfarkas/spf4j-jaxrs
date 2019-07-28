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
package org.spf4j.jaxrs;

import javax.ws.rs.ServiceUnavailableException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.failsafe.RetryDecision.Type;

/**
 *
 * @author Zoltan Farkas
 */
public class UtilsTest {


  @Test
  public void testUtils() {
    long nanoTime = System.nanoTime();
    RetryDecision decision = Utils.DEFAULT_HTTP_RETRY_POLICY.getRetryPredicate(nanoTime, nanoTime + 1000000000L)
            .getExceptionDecision(new ServiceUnavailableException(0L), null);
    Assert.assertEquals(0L, decision.getDelayNanos());
    Assert.assertEquals(Type.Retry, decision.getDecisionType());
  }

}
