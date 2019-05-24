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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.Entity;
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
import org.spf4j.base.avro.jmx.OperationInvocation;

/**
 *
 * @author Zoltan Farkas
 */
public class JMXResourceTest extends ServiceIntegrationBase {

  static {
    System.setProperty("avro.generic.default.stringClass", String.class.getName());
  }

  private static final Logger LOG = LoggerFactory.getLogger(JMXResourceTest.class);

  @Test
  public void testJmxEndpoint() {
    List<String> beans = getTarget().path("jmx/local").request(
            MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
    LOG.debug("Jmx resource ", beans);
    for (String beanName : beans) {
      List<String> memory = getTarget().path("jmx/local/{mbean}").resolveTemplate("mbean", beanName)
              .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
      LOG.debug("Jmx {} resource ", beanName, memory);
      List<MBeanAttributeInfo> attrs = getTarget().path("jmx/local/{mbean}/attributes")
            .resolveTemplate("mbean", beanName)
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<MBeanAttributeInfo>>() { });
      LOG.debug("Jmx {} attributes ", beanName, attrs);

      List<AttributeValue> values = getTarget().path("jmx/local/{mbean}/attributes/values")
              .resolveTemplate("mbean", beanName)
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<AttributeValue>>() { });
      LOG.debug("Jmx {} attributes values ", beanName, values);

      if (!attrs.isEmpty()) {
        MBeanAttributeInfo ai = attrs.get(0);
        Object resp = getTarget().path("jmx/local/{mbean}/attributes/values/{attrName}")
              .resolveTemplate("mbean", beanName)
              .resolveTemplate("attrName", ai.getName())
              .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
        LOG.debug("Jmx {} attribute  {} value ", beanName, ai.getName(), resp);
      }
      List<MBeanOperationInfo> ops = getTarget().path("jmx/local/{mbean}/operations")
            .resolveTemplate("mbean", beanName)
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<MBeanOperationInfo>>() { });
      LOG.debug("Jmx {} operations ", beanName, ops);
    }
  }

  @Test
  public void testGetAttribute() {
     Object resp = getTarget().path("jmx/local/{mbean}/attributes/values/{attrName}")
              .resolveTemplate("mbean", "java.lang:name=Metaspace,type=MemoryPool")
              .resolveTemplate("attrName", "Name")
              .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
     LOG.debug("Jmx {} attribute  {} value ", "java.lang:name=Metaspace,type=MemoryPool", "Name", resp);
  }

  @Test
  public void testGetAttributeCluster() {
     Object resp = getTarget().path("jmx/cluster/127.0.0.1/{mbean}/attributes/values/{attrName}")
              .resolveTemplate("mbean", "java.lang:name=Metaspace,type=MemoryPool")
              .resolveTemplate("attrName", "Name")
              .request(MediaType.APPLICATION_JSON).get(new GenericType<Object>() { });
     LOG.debug("Jmx {} attribute  {} value ", "java.lang:name=Metaspace,type=MemoryPool", "Name", resp);
  }


  @Test
  public void testInvokeOperation() {
    /* calling: com.sun.management:type=DiagnosticCommand operation: gcClassHistogram.
     {
    "name": "gcClassHistogram",
    "parameters": [{
        "name": "arguments",
        "type": "[Ljava.lang.String;",
        "avroSchema": "string",
        "description": "Array of Diagnostic Commands Arguments and Options",
        "descriptor": {}
      }],
    "returnType": "java.lang.String",
    "returnAvroSchema": "string",
    "description": "Provide statistics about the Java heap usage.",
    "impact": "ACTION_INFO",
    "descriptor": {
      "dcmd.permissionClass=java.lang.management.ManagementPermission": null,
      "dcmd.enabled=(true)": null,
      "dcmd.arguments=({-all={dcmd.arg.description=Inspect all objects,
    including unreachable objects, dcmd.arg.isMandatory=false,
    dcmd.arg.isMultiple=false, dcmd.arg.isOption=true, dcmd.arg.name=-all,
    dcmd.arg.position=-1, dcmd.arg.type=BOOLEAN}})": null,
      "dcmd.help=GC.class_histogram\nProvide statistics about the Java heap usage.
    \n\nImpact: High: Depends on Java heap size and content.\n\nPermission:
    java.lang.management.ManagementPermission(monitor)\n\nSyntax : GC.class_histogram
    [options]\n\nOptions: (options must be specified using the <key> or <key>=<value> syntax)\n
    \t-all : [optional] Inspect all objects, including unreachable objects (BOOLEAN, false)\n": null,
      "dcmd.permissionName=monitor": null,
      "dcmd.name=GC.class_histogram": null,
      "dcmd.description=Provide statistics about the Java heap usage.": null,
      "dcmd.vmImpact=High: Depends on Java heap size and content.": null,
      "dcmd.permissionAction=": null
    }
  }
    */
    OperationInvocation invocation = new OperationInvocation("gcClassHistogram",
            Arrays.asList("[Ljava.lang.String;"), Collections.singletonList(Collections.singletonList("-all")));
     Object resp = getTarget().path("jmx/local/{mbean}/operations")
              .resolveTemplate("mbean", "com.sun.management:type=DiagnosticCommand")
              .request(MediaType.APPLICATION_JSON).
             post(Entity.entity(invocation, MediaType.APPLICATION_JSON), new GenericType<Object>() { });
     LOG.debug("Jmx {} operation  {} returned", "com.sun.management:type=DiagnosticCommand", "gcClassHistogram", resp);
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
