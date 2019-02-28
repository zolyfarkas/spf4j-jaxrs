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
package org.spf4j.kube;

import com.google.common.net.HostAndPort;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class ClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(ClientTest.class);

  @Test
  public void testKubeClient() {
    Client kubeCl = new Client("http://127.0.0.1:32768", null, null);
    List<HostAndPort> endpoints = kubeCl.getEndpoints("default", "jaxrs-spf4j-demo");
    LOG.debug("Endpoints: {} ", endpoints);
    Assert.assertFalse(endpoints.isEmpty());

  }

}
