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
package org.spf4j.actuator.jmx;

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import static org.spf4j.actuator.ServiceIntegrationBase.getTarget;
import org.spf4j.base.avro.jmx.MBeanAttributeInfo;
import org.spf4j.base.avro.jmx.MBeanOperationInfo;

/**
 *
 * @author Zoltan Farkas
 */
public class JMXResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(JMXResourceTest.class);

  @Test
  public void testJmxEndpoint() {
    List<String> beans = getTarget().path("jmx").request(
            MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
    LOG.debug("Jmx resource ", beans);
    List<String> memory = getTarget().path("jmx/{mbean}").resolveTemplate("mbean", "java.lang:type=Memory")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
    LOG.debug("Jmx memory Mbean resource ", memory);
    List<MBeanAttributeInfo> attrs = getTarget().path("jmx/{mbean}/attributes")
            .resolveTemplate("mbean", "java.lang:type=Memory")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<MBeanAttributeInfo>>() { });
    LOG.debug("Jmx memory Mbean attributes ", attrs);
    MBeanAttributeInfo ai = attrs.get(0);
    Object resp = getTarget().path("jmx/{mbean}/attributes/{attrName}")
            .resolveTemplate("mbean", "java.lang:type=Memory")
            .resolveTemplate("attrName", ai.getName())
            .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
    LOG.debug("Jmx memory Mbean attribute value ", resp);
    List<MBeanOperationInfo> ops = getTarget().path("jmx/{mbean}/operations")
            .resolveTemplate("mbean", "java.lang:type=Memory")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<MBeanOperationInfo>>() { });
    LOG.debug("Jmx memory Mbean attributes ", ops);
  }


}
