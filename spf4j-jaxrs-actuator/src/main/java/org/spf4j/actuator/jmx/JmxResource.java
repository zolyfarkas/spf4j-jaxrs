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
import javax.annotation.Nullable;
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
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenType;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ExtendedReflectData;
import org.glassfish.hk2.api.Immediate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Reflections;
import org.spf4j.base.avro.jmx.AttributeValue;
import org.spf4j.base.avro.jmx.OperationImpact;
import org.spf4j.base.avro.jmx.OperationInvocation;
import org.spf4j.http.ContextTags;
import org.spf4j.http.HttpWarning;
import org.spf4j.jaxrs.ArrayWriter;
import org.spf4j.jaxrs.StreamingArrayOutput;
import org.spf4j.log.ExecContextLogger;

/**
 *
 * @author Zoltan Farkas
 */
@Path("jmx")
@Immediate
@RolesAllowed("operator")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Beta
public class JmxResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(JmxResource.class));

  private static final String[] MBEAN_RESOURCES = {
    "attributes",
    "operations"
  };

  @GET
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
  @Path("{mbeanName}")
  public String[] get(@PathParam("mbeanName") final String mbeanName) {
    return MBEAN_RESOURCES;
  }

  private static Schema getSimpleTypeSchema(final String typeName) {
      try {
        return ExtendedReflectData.get().getSchema(Reflections.forName(typeName));
      } catch (ClassNotFoundException ex) {
        throw new RuntimeException(ex);
      }
  }

  @GET
  @Path("{mbeanName}/attributes")
  public StreamingArrayOutput<org.spf4j.base.avro.jmx.MBeanAttributeInfo> getMBeanAttributes(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanAttributeInfo[] attrs = getMBeanInfo(mbeanName).getAttributes();
    return new StreamingArrayOutput<org.spf4j.base.avro.jmx.MBeanAttributeInfo>() {
      @Override
      public void write(final ArrayWriter<org.spf4j.base.avro.jmx.MBeanAttributeInfo> output) throws IOException {
        for (MBeanAttributeInfo attr : attrs) {
          Map<String, Object> descriptorMap = toDescriptorMap(attr.getDescriptor());
          if (attr instanceof OpenMBeanAttributeInfoSupport) {
             OpenType openType = ((OpenMBeanAttributeInfoSupport) attr).getOpenType();
                         OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                    .getConverter(openType);
            output.accept(new org.spf4j.base.avro.jmx.MBeanAttributeInfo(attr.getName(), attr.getType(),
                  converter.getSchema(openType, OpenTypeConverterSupplier.INSTANCE),
                  attr.getDescription(), attr.isReadable(), attr.isWritable(), attr.isIs(), descriptorMap));
          } else {
            output.accept(new org.spf4j.base.avro.jmx.MBeanAttributeInfo(attr.getName(), attr.getType(),
                  getSimpleTypeSchema(attr.getType()),
                  attr.getDescription(), attr.isReadable(), attr.isWritable(), attr.isIs(), descriptorMap));
          }
        }
      }
    };
  }

  @GET
  @Path("{mbeanName}/attributes/values")
  public StreamingArrayOutput<AttributeValue> getMBeanAttributeValues(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    MBeanInfo mBeanInfo = getMBeanInfo(srv, mname);
    return new StreamingArrayOutput<AttributeValue>() {
      @Override
      public void write(final ArrayWriter<AttributeValue> output) throws IOException {
        for (MBeanAttributeInfo attr : mBeanInfo.getAttributes()) {
          if (!attr.isReadable()) {
            continue;
          }
          LOG.debug("writing attribute type {}", attr.getType(), attr);
          String name = attr.getName();
          if (attr instanceof OpenMBeanAttributeInfoSupport) {
            OpenType openType = ((OpenMBeanAttributeInfoSupport) attr).getOpenType();
            OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                    .getConverter(openType);
            try {
              output.accept(new AttributeValue(name, converter.fromOpenValue(openType,
                      srv.getAttribute(mname, name), OpenTypeConverterSupplier.INSTANCE)));
            } catch (MBeanException | ReflectionException ex) {
              throw new RuntimeException("Unable to convert attribute " + attr, ex);
            } catch (AttributeNotFoundException | InstanceNotFoundException ex) {
              throw new NotFoundException(ex);
            }
          } else {
            try {
              output.accept(new AttributeValue(name, srv.getAttribute(mname, name)));
            } catch (MBeanException | ReflectionException ex) {
              throw new RuntimeException("Unable to convert attribute " + attr, ex);
            } catch (AttributeNotFoundException | InstanceNotFoundException ex) {
              throw new NotFoundException(ex);
            } catch (RuntimeMBeanException ex) {
              ExecutionContexts.current().add(ContextTags.HTTP_WARNINGS, new HttpWarning(HttpWarning.MISCELLANEOUS,
                      "jmx", ex.getMessage()));
              LOG.warn("Unable to read value for {}", attr.getName(), attr, ex);
            }
          }
        }
      }
    };
  }

  @GET
  @Path("{mbeanName}/attributes/values/{attrName}")
  public Object getMBeanAttribute(@PathParam("mbeanName") final String mbeanName,
          @PathParam("attrName") final String attrName) throws MBeanException, ReflectionException, IOException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    MBeanInfo mBeanInfo = getMBeanInfo(srv, mname);
    MBeanAttributeInfo attr = findAttrInfo(mBeanInfo, attrName);
    if (attr == null) {
      throw new NotFoundException("Attribute " + attrName + " not found for " + mbeanName);
    }
    try {
      if (attr  instanceof OpenMBeanAttributeInfoSupport) {
        OpenType openType = ((OpenMBeanAttributeInfoSupport) attr).getOpenType();
        OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                    .getConverter(openType);
        return converter.fromOpenValue(openType, srv.getAttribute(mname, attrName), OpenTypeConverterSupplier.INSTANCE);
      } else {
        return srv.getAttribute(mname, attrName);
      }
    } catch (AttributeNotFoundException | InstanceNotFoundException ex) {
      throw new NotFoundException("Atttr not found: " + attrName + " for " + mbeanName, ex);
    } catch (UnsupportedOperationException ex) {
      throw new ClientErrorException("Attribute " + mbeanName + '/' + attrName + " not readable", 400);
    }
  }

  @Nullable
  private static MBeanAttributeInfo findAttrInfo(final MBeanInfo mBeanInfo, final String attrName) {
    for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {
      if (attribute.getName().equals(attrName)) {
        return attribute;
      }
    }
    return null;
  }

  @POST
  @Path("{mbeanName}/attributes/values/{attrName}")
  @Consumes(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
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
  @Path("{mbeanName}/operations")
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
            if (pi instanceof OpenMBeanParameterInfo) {
              OpenType<?> pOpenType = ((OpenMBeanParameterInfo) pi).getOpenType();
              OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                      .getConverter(pOpenType);
              params.add(new org.spf4j.base.avro.jmx.MBeanParameterInfo(pi.getName(),
                    pi.getType(), converter.getSchema(pOpenType, OpenTypeConverterSupplier.INSTANCE),
                      pi.getDescription(), toDescriptorMap(pi.getDescriptor())));
            } else {
              params.add(new org.spf4j.base.avro.jmx.MBeanParameterInfo(pi.getName(),
                    pi.getType(), getSimpleTypeSchema(op.getReturnType()),
                      pi.getDescription(), toDescriptorMap(pi.getDescriptor())));
            }
          }
          if (op instanceof OpenMBeanOperationInfoSupport) {
            OpenType<?> returnOpenType = ((OpenMBeanOperationInfoSupport) op).getReturnOpenType();
            OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                    .getConverter(returnOpenType);
            output.accept(new org.spf4j.base.avro.jmx.MBeanOperationInfo(op.getName(), params,
                  op.getReturnType(), converter.getSchema(returnOpenType, OpenTypeConverterSupplier.INSTANCE),
                  op.getDescription(), toImpact(op.getImpact()), descriptorMap));
          } else {
            output.accept(new org.spf4j.base.avro.jmx.MBeanOperationInfo(op.getName(), params,
                  op.getReturnType(), getSimpleTypeSchema(op.getReturnType()),
                  op.getDescription(), toImpact(op.getImpact()), descriptorMap));
          }
        }
      }
    };
  }

  @POST
  @Path("/{mbeanName}/operations")
  @Consumes(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
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

  private static MBeanInfo getMBeanInfo(final String mbeanName) throws NotFoundException, RuntimeException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    return getMBeanInfo(srv, mname);
  }

  private static MBeanInfo getMBeanInfo(final MBeanServerConnection srv, final ObjectName mname)
          throws RuntimeException {
    MBeanInfo mBeanInfo;
    try {
      mBeanInfo = srv.getMBeanInfo(mname);
    } catch (IntrospectionException | ReflectionException | IOException ex) {
      throw new RuntimeException(ex);
    } catch (InstanceNotFoundException ex) {
      throw new NotFoundException("Mbean not found " + mname, ex);
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
