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
package org.spf4j.api_browser;

import javax.ws.rs.core.MediaType;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class ApiBrowserResourceTest  extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(ApiBrowserResourceTest.class);

  @Test
  public void testUiIndex() {
    CharSequence html = getTarget().path("apiBrowser")
            .request(MediaType.WILDCARD_TYPE).get(CharSequence.class);
    LOG.debug("application info", html);
    Assert.assertNotNull(html);
    Assert.assertThat(html.toString(), Matchers.containsString("html"));
  }
}
