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
package org.spf4j.actuator.profiles;

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author Zoltan Farkas
 */
public class ProfilesResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(ProfilesResourceTest.class);

  @Test
  public void testProfiles() {
    Spf4jWebTarget target = getTarget();
    List<String> labels = target.path("profiles/local/groups")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() {
    });
    LOG.debug("Labels: {}", labels);
    Assert.assertFalse(labels.isEmpty());
    SampleNode node = target.path("profiles/local/groups/" + labels.get(0))
            .request("application/stack.samples+json").get(new GenericType<SampleNode>() {
    });
    Assert.assertNotNull(node);
    CharSequence html = target.path("profiles/local/visualize/groups/" + labels.get(0))
            .request("*/*").get(CharSequence.class);
    LOG.debug("HTML: {}", html);
    Assert.assertThat(html.toString(), Matchers.containsString("Node profile for"));
  }

}
