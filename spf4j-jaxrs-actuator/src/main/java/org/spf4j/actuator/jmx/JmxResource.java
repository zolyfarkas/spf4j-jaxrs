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
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.glassfish.hk2.api.Immediate;
import org.spf4j.base.avro.jmx.OperationImpact;
import org.spf4j.base.avro.jmx.OperationInvocation;
import org.spf4j.jaxrs.ArrayWriter;
import org.spf4j.jaxrs.StreamingArrayOutput;

/**
 *
 * @author Zoltan Farkas
 */
@Path("jmx")
@Immediate
@RolesAllowed("operator")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Beta
public class JmxResource {

  @GET
  @Path("")
  @Produces({"application/json", "application/avro"})
  public StreamingArrayOutput<String> getMBeans() {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectInstance> beans;
    try {
      beans = srv.queryMBeans(new ObjectName("*:*"), null);
    } catch (MalformedObjectNameException | IOException ex) {
      throw new RuntimeException(ex);
    }
    return new StreamingArrayOutput<String>() {
      @Override
      public void write(final ArrayWriter<String> output) throws IOException {
        for (ObjectInstance bean : beans) {
          output.accept(bean.getObjectName().getCanonicalName());
        }
      }
    };
  }

  @GET
  @Path("/{mbeanName}")
  @Produces({"application/json", "application/avro"})
  public String[] get(@PathParam("mbeanName") final String mbeanName) {
    return new String[]{"attributes", "operations"};
  }

  @GET
  @Path("/{mbeanName}/attributes")
  @Produces({"application/json", "application/avro"})
  public StreamingArrayOutput<org.spf4j.base.avro.jmx.MBeanAttributeInfo> getMBeanAttributes(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanAttributeInfo[] attrs = getMBeanInfo(mbeanName).getAttributes();
    return new StreamingArrayOutput<org.spf4j.base.avro.jmx.MBeanAttributeInfo>() {
      @Override
      public void write(final ArrayWriter<org.spf4j.base.avro.jmx.MBeanAttributeInfo> output) throws IOException {
        for (MBeanAttributeInfo attr : attrs) {
          Map<String, Object> descriptorMap = toDescriptorMap(attr.getDescriptor());
          output.accept(new org.spf4j.base.avro.jmx.MBeanAttributeInfo(attr.getName(), attr.getType(),
                  attr.getDescription(), attr.isReadable(), attr.isWritable(), attr.isIs(), descriptorMap));
        }
      }
    };
  }

  @GET
  @Path("/{mbeanName}/attributes/{attrName}")
  @Produces({"application/json", "application/avro"})
  public Object getMBeanAttribute(@PathParam("mbeanName") final String mbeanName,
          @PathParam("attrName") final String attrName) throws MBeanException, ReflectionException, IOException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    try {
      return srv.getAttribute(mname, attrName);
    } catch (AttributeNotFoundException | InstanceNotFoundException ex) {
      throw new NotFoundException("Atttr not found: " + attrName + " for " + mbeanName, ex);
    }
  }

  @POST
  @Path("/{mbeanName}/attributes/{attrName}")
  @Produces({"application/json", "application/avro"})
  public void setMBeanAttribute(@PathParam("mbeanName") final String mbeanName,
          @PathParam("attrName") final String attrName,
          final Object value) throws MBeanException, ReflectionException, IOException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    try {
      srv.setAttribute(mname, new Attribute(attrName, value));
    } catch (AttributeNotFoundException | InstanceNotFoundException ex) {
      throw new NotFoundException("Atttr not found: " + attrName + " for " + mbeanName, ex);
    } catch (InvalidAttributeValueException ex) {
      throw new ClientErrorException("Invalid value for " + mbeanName + ':' + attrName + value, 400, ex);
    }
  }

  @GET
  @Path("/{mbeanName}/operations")
  @Produces({"application/json", "application/avro"})
  public StreamingArrayOutput<org.spf4j.base.avro.jmx.MBeanOperationInfo> getMBeanOperations(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanOperationInfo[] operations = getMBeanInfo(mbeanName).getOperations();
    return new StreamingArrayOutput<org.spf4j.base.avro.jmx.MBeanOperationInfo>() {
      @Override
      public void write(final ArrayWriter<org.spf4j.base.avro.jmx.MBeanOperationInfo> output) throws IOException {
        for (MBeanOperationInfo op : operations) {
          Map<String, Object> descriptorMap = toDescriptorMap(op.getDescriptor());
          MBeanParameterInfo[] signature = op.getSignature();
          List<org.spf4j.base.avro.jmx.MBeanParameterInfo> params = new ArrayList<>(signature.length);
          for (MBeanParameterInfo pi : signature) {
            params.add(new org.spf4j.base.avro.jmx.MBeanParameterInfo(pi.getName(),
                    pi.getType(), pi.getDescription(), toDescriptorMap(pi.getDescriptor())));
          }
          output.accept(new org.spf4j.base.avro.jmx.MBeanOperationInfo(op.getName(), params,
                  op.getReturnType(), op.getDescription(), toImpact(op.getImpact()), descriptorMap));
        }
      }
    };
  }

  @POST
  @Path("/{mbeanName}/operations")
  @Produces({"application/json", "application/avro"})
  @Consumes({"application/json", "application/avro"})
  public Object invoke(
          @PathParam("mbeanName") final String mbeanName, final OperationInvocation invocation)
          throws MBeanException, ReflectionException, IOException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    List<Object> parameters = invocation.getParameters();
    List<String> sign = invocation.getSignature();
    try {
      return srv.invoke(mname, invocation.getName(),
              parameters.toArray(new Object[parameters.size()]), sign.toArray(new String[sign.size()]));
    } catch (InstanceNotFoundException ex) {
      throw new NotFoundException("bean not found " + mbeanName, ex);
    }
  }


  private static Map<String, Object> toDescriptorMap(final Descriptor descriptor) {
    String[] fields = descriptor.getFields();
    Object[] fieldValues = descriptor.getFieldValues(fields);
    Map<String, Object> descriptorMap = new HashMap<>(fields.length);
    for (int j = 0; j < fields.length; j++) {
      descriptorMap.put(fields[j], fieldValues[j]);
    }
    return descriptorMap;
  }

  private static OperationImpact toImpact(final int impactId) {
    switch (impactId) {
      case MBeanOperationInfo.ACTION:
        return OperationImpact.ACTION;
      case MBeanOperationInfo.ACTION_INFO:
        return OperationImpact.ACTION_INFO;
      case MBeanOperationInfo.INFO:
        return OperationImpact.INFO;
      default:
        return OperationImpact.UNKNOWN;
    }
  }

  private MBeanInfo getMBeanInfo(final String mbeanName) throws NotFoundException, RuntimeException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    MBeanInfo mBeanInfo;
    try {
      mBeanInfo = srv.getMBeanInfo(mname);
    } catch (IntrospectionException | ReflectionException | IOException ex) {
      throw new RuntimeException(ex);
    } catch (InstanceNotFoundException ex) {
      throw new NotFoundException("Mbean not found " + mbeanName, ex);
    }
    return mBeanInfo;
  }

  private static ObjectName getJmxObjName(final String mbeanName) throws NotFoundException {
    ObjectName mname;
    try {
      mname = new ObjectName(mbeanName);
    } catch (MalformedObjectNameException ex) {
      throw new NotFoundException("Mbean not found " + mbeanName, ex);
    }
    return mname;
  }

}
