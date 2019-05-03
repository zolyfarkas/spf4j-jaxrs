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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.api.Immediate;

/**
 *
 * @author Zoltan Farkas
 */
@Path("jmx")
@Immediate
@RolesAllowed("operator")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class JmxResource {

  @GET
  @Path("")
  @Produces({"application/json", "application/avro"})
  public List<String> getMbeans() {
    MBeanServer srv = ManagementFactory.getPlatformMBeanServer();
    Set<ObjectInstance> beans;
    try {
      beans = srv.queryMBeans(new ObjectName("*:*"), null);
    } catch (MalformedObjectNameException ex) {
      throw new RuntimeException(ex);
    }
    List<String> res = new ArrayList<>(beans.size());
    for (ObjectInstance bean : beans) {
      res.add(bean.getObjectName().getCanonicalName());
    }
    return res;
  }

  @GET
  @Path("/{path:.*}")
  @Produces({"application/json", "application/avro"})
  public Response get(@PathParam("path") final List<PathSegment> path) {
    MBeanServer srv = ManagementFactory.getPlatformMBeanServer();
    PathSegment mbeanName = path.get(0);
    ObjectName mname;
    try {
      mname = new ObjectName(mbeanName.getPath());
    } catch (MalformedObjectNameException ex) {
      throw new NotFoundException("Mbean not found " + mbeanName, ex);
    }
    MBeanInfo mBeanInfo;
    try {
      mBeanInfo = srv.getMBeanInfo(mname);
    } catch (IntrospectionException | ReflectionException ex) {
      throw new RuntimeException(ex);
    } catch (InstanceNotFoundException ex) {
      throw new NotFoundException("Mbean not found " + mbeanName, ex);
    }
    if (path.size() == 1) {
      return Response.ok(Arrays.asList("attributes", "operations")).build();
    } else {
      PathSegment attOrOp = path.get(1);
      switch (attOrOp.getPath()) {
        case "attributes":
          if (path.size() == 2) {
            MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
            List<String> res = new ArrayList<>(attributes.length);
            for (MBeanAttributeInfo attr : attributes) {
              res.add(attr.getName());
            }
            return Response.ok(res).build();
          } else {
            throw new UnsupportedOperationException();
          }
        case "operations":
          if (path.size() == 2) {
            MBeanOperationInfo[] operations = mBeanInfo.getOperations();
            List<String> res = new ArrayList<>(operations.length);
            for (MBeanOperationInfo op : operations) {
              res.add(op.getName());
            }
            return Response.ok(res).build();
          } else {
            throw new UnsupportedOperationException();
          }
        default:
          throw new NotFoundException("Bean subresource not found " + attOrOp.getPath());
      }
    }

  }

}
