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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import static org.spf4j.actuator.ServiceIntegrationBase.getTarget;
import org.spf4j.base.avro.jmx.AttributeValue;
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
    for (String beanName : beans) {
      List<String> memory = getTarget().path("jmx/{mbean}").resolveTemplate("mbean", beanName)
              .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
      LOG.debug("Jmx {} resource ", beanName, memory);
      List<MBeanAttributeInfo> attrs = getTarget().path("jmx/{mbean}/attributes")
            .resolveTemplate("mbean", beanName)
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<MBeanAttributeInfo>>() { });
      LOG.debug("Jmx {} attributes ", beanName, attrs);

      List<AttributeValue> values = getTarget().path("jmx/{mbean}/attributes/values")
              .resolveTemplate("mbean", beanName)
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<AttributeValue>>() { });
      LOG.debug("Jmx {} attributes values ", beanName, values);

      if (!attrs.isEmpty()) {
        MBeanAttributeInfo ai = attrs.get(0);
        Object resp = getTarget().path("jmx/{mbean}/attributes/values/{attrName}")
              .resolveTemplate("mbean", beanName)
              .resolveTemplate("attrName", ai.getName())
              .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
        LOG.debug("Jmx {} attribute  {} value ", beanName, ai.getName(), resp);
      }
      List<MBeanOperationInfo> ops = getTarget().path("jmx/{mbean}/operations")
            .resolveTemplate("mbean", beanName)
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<MBeanOperationInfo>>() { });
      LOG.debug("Jmx{} attributes ", beanName, ops);
    }
  }

  @Test
  public void testGetAttribute() {
     Object resp = getTarget().path("jmx/{mbean}/attributes/values/{attrName}")
              .resolveTemplate("mbean", "java.lang:name=Metaspace,type=MemoryPool")
              .resolveTemplate("attrName", "Name")
              .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
     LOG.debug("Jmx {} attribute  {} value ", "java.lang:name=Metaspace,type=MemoryPool", "Name", resp);
  }



}
