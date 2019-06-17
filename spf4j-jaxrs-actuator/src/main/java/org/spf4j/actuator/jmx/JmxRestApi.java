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

import com.google.common.annotations.Beta;
import java.io.IOException;
import javax.annotation.security.RolesAllowed;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.spf4j.base.avro.jmx.AttributeValue;
import org.spf4j.base.avro.jmx.MBeanAttributeInfo;
import org.spf4j.base.avro.jmx.MBeanOperationInfo;
import org.spf4j.base.avro.jmx.OperationInvocation;
import org.spf4j.jaxrs.StreamingArrayContent;

/**
 *
 * @author Zoltan Farkas
 */
@RolesAllowed("operator")
@Beta
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
public interface JmxRestApi {

  @GET
  @Path(value = "{mbeanName}")
  String[] get(@PathParam(value = "mbeanName") String mbeanName);

  @GET
  @Path(value = "{mbeanName}/attributes/values/{attrName}")
  Object getMBeanAttribute(@PathParam(value = "mbeanName") String mbeanName,
          @PathParam(value = "attrName") String attrName)
          throws MBeanException, ReflectionException, IOException;

  @GET
  @Path(value = "{mbeanName}/attributes/values")
  StreamingArrayContent<AttributeValue> getMBeanAttributeValues(@PathParam(value = "mbeanName") String mbeanName);

  @GET
  @Path(value = "{mbeanName}/attributes")
  StreamingArrayContent<MBeanAttributeInfo> getMBeanAttributes(@PathParam(value = "mbeanName") String mbeanName);

  @GET
  @Path(value = "{mbeanName}/operations")
  StreamingArrayContent<MBeanOperationInfo> getMBeanOperations(@PathParam(value = "mbeanName") String mbeanName);

  @GET
  StreamingArrayContent<String> getMBeans();

  @POST
  @Path(value = "/{mbeanName}/operations")
  @Consumes(value = {"application/avro-x+json",
    "application/json", "application/avro+json",
    "application/avro", "application/octet-stream"})
  Object invoke(@PathParam(value = "mbeanName") String mbeanName, OperationInvocation invocation)
          throws MBeanException, ReflectionException, IOException;

  @POST
  @Path(value = "{mbeanName}/attributes/values/{attrName}")
  @Consumes(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  void setMBeanAttribute(@PathParam(value = "mbeanName") String mbeanName,
          @PathParam(value = "attrName") String attrName, Object value)
          throws MBeanException, ReflectionException, IOException;

}
