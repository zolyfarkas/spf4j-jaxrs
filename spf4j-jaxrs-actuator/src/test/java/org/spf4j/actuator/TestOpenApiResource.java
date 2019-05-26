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
package org.spf4j.actuator;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.spf4j.actuator.ServiceIntegrationBase.getTarget;

/**
 *
 * @author Zoltan Farkas
 */
public class TestOpenApiResource extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(TestOpenApiResource.class);



  @Test
  public void testOpenApi() {
    CharSequence json = getTarget().path("openapi.json")
            .request(MediaType.APPLICATION_JSON)
            .get(CharSequence.class);
    LOG.debug("api spec", json);
  }

  @Test
  public void testOpenApiUI() throws InterruptedException {
    CharSequence json = getTarget().path("openapi.json")
            .request(MediaType.APPLICATION_JSON)
            .get(CharSequence.class);
    LOG.debug("api spec", json);
    Thread.sleep(1000000000);
  }


}
