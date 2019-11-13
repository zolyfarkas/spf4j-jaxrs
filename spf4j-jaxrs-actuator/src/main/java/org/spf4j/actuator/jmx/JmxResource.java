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
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
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
import org.spf4j.base.ArrayWriter;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.jaxrs.StreamingArrayContent;

/**
 *
 * @author Zoltan Farkas
 */
@Path("jmx/local")
@Immediate
@RolesAllowed("operator")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Beta
public class JmxResource implements JmxRestApi {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(JmxResource.class));

  private static final String[] MBEAN_RESOURCES = {
    "attributes",
    "operations"
  };

  @GET
  @Override
  public StreamingArrayContent<String> getMBeans() {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectInstance> beans;
    try {
      beans = srv.queryMBeans(new ObjectName("*:*"), null);
    } catch (MalformedObjectNameException | IOException ex) {
      throw new RuntimeException(ex);
    }
    return new StreamingArrayContent<String>() {
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
  @Override
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
  @Override
  public StreamingArrayContent<org.spf4j.base.avro.jmx.MBeanAttributeInfo> getMBeanAttributes(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanAttributeInfo[] attrs = getMBeanInfo(mbeanName).getAttributes();
    return new StreamingArrayContent<org.spf4j.base.avro.jmx.MBeanAttributeInfo>() {
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
  @Override
  public StreamingArrayContent<AttributeValue> getMBeanAttributeValues(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    MBeanInfo mBeanInfo = getMBeanInfo(srv, mname);
    return new StreamingArrayContent<AttributeValue>() {
      @Override
      public void write(final ArrayWriter<AttributeValue> output) throws IOException {
        for (MBeanAttributeInfo attr : mBeanInfo.getAttributes()) {
          if (!attr.isReadable()) {
            continue;
          }
          LOG.debug("writing attribute type {}", attr.getType(), attr);
          String name = attr.getName();
          OpenType openType = getOpenType(attr);
          OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE.getConverter(openType);
          try {
            output.accept(new AttributeValue(name, converter.fromOpenValue(openType,
                    srv.getAttribute(mname, name), OpenTypeConverterSupplier.INSTANCE)));
          } catch (MBeanException | ReflectionException ex) {
            throw new RuntimeException("Unable to convert attribute " + attr, ex);
          } catch (AttributeNotFoundException | InstanceNotFoundException ex) {
            throw new NotFoundException(ex);
          } catch (RuntimeMBeanException ex) {
            ContextTags.HTTP_WARNINGS.addToContext(ExecutionContexts.current(),
                    new HttpWarning(HttpWarning.MISCELLANEOUS,
                    "jmx", ex.getMessage()));
            LOG.warn("Unable to read value for {}", attr.getName(), attr, ex);
          }
        }
      }

    };
  }

  private static OpenType getOpenType(final MBeanAttributeInfo attr) {
    OpenType openType;
    if (attr instanceof OpenMBeanAttributeInfoSupport) {
      openType = ((OpenMBeanAttributeInfoSupport) attr).getOpenType();
    } else {
      Object ot = attr.getDescriptor().getFieldValue("openType");
      if (ot instanceof OpenType) {
        return (OpenType) ot;
      }
      openType = SimpleTypes.getOpenType(attr.getType());
    }
    return openType;
  }

  private static OpenType getOpenType(final MBeanParameterInfo pi) {
    OpenType openType;
    if (pi instanceof OpenMBeanParameterInfoSupport) {
      openType = ((OpenMBeanParameterInfoSupport) pi).getOpenType();
    } else {
      Object ot = pi.getDescriptor().getFieldValue("openType");
      if (ot instanceof OpenType) {
        return (OpenType) ot;
      }
      openType = SimpleTypes.getOpenType(pi.getType());
    }
    return openType;
  }

  private static OpenType getOpenType(final MBeanOperationInfo op) {
    OpenType openType;
    if (op instanceof OpenMBeanOperationInfoSupport) {
      openType = ((OpenMBeanOperationInfoSupport) op).getReturnOpenType();
    } else {
      Object ot = op.getDescriptor().getFieldValue("openType");
      if (ot instanceof OpenType) {
        return (OpenType) ot;
      }
      openType = SimpleTypes.getOpenType(op.getReturnType());
    }
    return openType;
  }

  @GET
  @Path("{mbeanName}/attributes/values/{attrName}")
  @Override
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
      OpenType openType = getOpenType(attr);
      OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE.getConverter(openType);
      return converter.fromOpenValue(openType, srv.getAttribute(mname, attrName), OpenTypeConverterSupplier.INSTANCE);
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
  @Override
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
  @Override
  public StreamingArrayContent<org.spf4j.base.avro.jmx.MBeanOperationInfo> getMBeanOperations(
          @PathParam("mbeanName") final String mbeanName) {
    MBeanOperationInfo[] operations = getMBeanInfo(mbeanName).getOperations();
    return new StreamingArrayContent<org.spf4j.base.avro.jmx.MBeanOperationInfo>() {
      @Override
      public void write(final ArrayWriter<org.spf4j.base.avro.jmx.MBeanOperationInfo> output) throws IOException {
        for (MBeanOperationInfo op : operations) {
          Map<String, Object> descriptorMap = toDescriptorMap(op.getDescriptor());
          MBeanParameterInfo[] signature = op.getSignature();
          List<org.spf4j.base.avro.jmx.MBeanParameterInfo> params = new ArrayList<>(signature.length);
          for (MBeanParameterInfo pi : signature) {
            OpenType<?> pOpenType = getOpenType(pi);
            OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                    .getConverter(pOpenType);
            params.add(new org.spf4j.base.avro.jmx.MBeanParameterInfo(pi.getName(),
                    pi.getType(), converter.getSchema(pOpenType, OpenTypeConverterSupplier.INSTANCE),
                    pi.getDescription(), toDescriptorMap(pi.getDescriptor())));
          }
          OpenType<?> returnOpenType = getOpenType(op);
          OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                  .getConverter(returnOpenType);
          output.accept(new org.spf4j.base.avro.jmx.MBeanOperationInfo(op.getName(), params,
                  op.getReturnType(), converter.getSchema(returnOpenType, OpenTypeConverterSupplier.INSTANCE),
                  op.getDescription(), toImpact(op.getImpact()), descriptorMap));
        }
      }
    };
  }

  @POST
  @Path("/{mbeanName}/operations")
  @Consumes({"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @Override
  public Object invoke(
          @PathParam("mbeanName") final String mbeanName, final OperationInvocation invocation)
          throws MBeanException, ReflectionException, IOException {
    MBeanServerConnection srv = ManagementFactory.getPlatformMBeanServer();
    ObjectName mname = getJmxObjName(mbeanName);
    MBeanInfo mBeanInfo = getMBeanInfo(srv, mname);
    String opName = invocation.getName();
    List<Object> parameters = invocation.getParameters();
    List<String> sign = invocation.getSignature();
    MBeanOperationInfo[] operations = mBeanInfo.getOperations();
    MBeanOperationInfo moi = null;
    for (MBeanOperationInfo oi : operations) {
      if (oi.getName().equals(opName)) {
        boolean match = true;
        for (MBeanParameterInfo param : oi.getSignature()) {
          if (!sign.contains(param.getType())) {
            match = false;
            break;
          }
        }
        if (match) {
          moi = oi;
          break;
        }
      }
    }
    if (moi == null) {
      throw new NotFoundException("Mbean " + mbeanName  + "  operation not found: " + opName);
    }

    Object[] cParams = convertInputParams(parameters, moi);


    OpenType retOpenType = getOpenType(moi);
    OpenTypeAvroConverter converter = OpenTypeConverterSupplier.INSTANCE
                    .getConverter(retOpenType);
    try {
      return converter.fromOpenValue(retOpenType,
              srv.invoke(mname, invocation.getName(), cParams, sign.toArray(new String[sign.size()])),
              OpenTypeConverterSupplier.INSTANCE);
    } catch (InstanceNotFoundException ex) {
      throw new NotFoundException("bean not found " + mbeanName, ex);
    }
  }

  private static Object[] convertInputParams(final List<Object> parameters, final MBeanOperationInfo moi) {
    try {
      Object[] cParams = new Object[parameters.size()];
      MBeanParameterInfo[] signature = moi.getSignature();
      for (int i = 0; i < cParams.length; i++) {
        MBeanParameterInfo pinfo = signature[i];
        OpenType openType = getOpenType(pinfo);
        OpenTypeAvroConverter pconv = OpenTypeConverterSupplier.INSTANCE.getConverter(openType);
        cParams[i] = pconv.toOpenValue(openType, parameters.get(i),
                org.spf4j.actuator.jmx.OpenTypeConverterSupplier.INSTANCE);
      }
      return cParams;
    } catch (RuntimeException ex) {
      throw new ClientErrorException("Invalid parameters " + parameters + " for " + moi, 400, ex);
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
