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
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
/**
 *
 * @author Zoltan Farkas
 */
public class ProfilesResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(ProfilesResourceTest.class);

  @Test
  public void testProfiles() {
     List<String> labels = getTarget().path("profiles/local/groups")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
     LOG.debug("Labels: {}", labels);
     Assert.assertFalse(labels.isEmpty());
  }

}
