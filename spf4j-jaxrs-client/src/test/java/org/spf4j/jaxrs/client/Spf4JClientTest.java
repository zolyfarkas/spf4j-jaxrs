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
package org.spf4j.jaxrs.client;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class Spf4JClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(Spf4JClientTest.class);

  @Test
  public void testClient() {
    Spf4JClient client = new Spf4jClientBuilder().build();
    Spf4jWebTarget target = client.target("http://localhost/test")
              .path("{metricName}")
              .resolveTemplate("metricName", "testMetric")
              .queryParam("from", "-P5D")
              .queryParam("to", "now");
    URI uri = target.getUri();
    LOG.debug("URI: {}", uri);
    Assert.assertEquals("http://localhost/test/testMetric?from=-P5D&to=now", uri.toString());
  }

  @Test
  public void testClient2() throws URISyntaxException {
      URI uri = new URI("http", null,
              "localhost", 8080, "/metrics/local", null, null);
      Spf4JClient client = new Spf4jClientBuilder().build();
      Spf4jWebTarget target = client.target(uri)
                .path("{metricName}")
                .resolveTemplate("metricName", "testMetric");
    URI uri1 = target.getUri();
    LOG.debug("URI: {}", uri1);
    Assert.assertEquals("http://localhost:8080/metrics/local/testMetric", uri1.toString());

  }

}
