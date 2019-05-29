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
package org.spf4j.actuator.cluster.jmx;

import java.util.Arrays;
import java.util.Collections;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import static org.spf4j.actuator.ServiceIntegrationBase.getTarget;
import org.spf4j.base.avro.jmx.OperationInvocation;

/**
 *
 * @author Zoltan Farkas
 */
public class JMXClusterResourceTest extends ServiceIntegrationBase {

  static {
    System.setProperty("avro.generic.default.stringClass", String.class.getName());
  }

  private static final Logger LOG = LoggerFactory.getLogger(JMXClusterResourceTest.class);

  @Test
  public void testGetAttributeCluster() {
     Object resp = getTarget().path("jmx/cluster/127.0.0.1/{mbean}/attributes/values/{attrName}")
              .resolveTemplate("mbean", "java.lang:name=Metaspace,type=MemoryPool")
              .resolveTemplate("attrName", "Name")
              .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
     LOG.debug("Jmx {} attribute  {} value ", "java.lang:name=Metaspace,type=MemoryPool", "Name", resp);
  }


 @Test
  public void testInvokeOperationCluster() {
    OperationInvocation invocation = new OperationInvocation("gcClassHistogram",
            Arrays.asList("[Ljava.lang.String;"), Collections.singletonList(Collections.singletonList("-all")));
     Object resp = getTarget().path("jmx/cluster/127.0.0.1/{mbean}/operations")
              .resolveTemplate("mbean", "com.sun.management:type=DiagnosticCommand")
              .request(MediaType.APPLICATION_JSON).
             post(Entity.entity(invocation, MediaType.APPLICATION_JSON), new GenericType<Object>() { });
     LOG.debug("Jmx {} operation  {} returned", "com.sun.management:type=DiagnosticCommand", "gcClassHistogram", resp);
  }



}
