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

import com.google.common.annotations.Beta;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.spf4j.actuator.jmx.JmxRestApi;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 * @author Zoltan Farkas
 */
@Path("jmx/cluster")
@Immediate
@RolesAllowed(JaxRsSecurityContext.OPERATOR_ROLE)
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Beta
public class JmxClusterResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final int port;

  private final String protocol;

  @Inject
  public JmxClusterResource(final Cluster cluster, final Spf4JClient httpClient,
          @ConfigProperty(name = "servlet.port") final int port,
          @ConfigProperty(name = "servlet.protocol") final String protocol) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.port = port;
    this.protocol = protocol;
  }

  @GET
  public List<String> getClusterNodes() {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> addresses = clusterInfo.getAddresses();
    List<String> result = new ArrayList<>(addresses.size());
    for (InetAddress addr : addresses) {
      result.add(addr.getHostAddress());
    }
    return result;
  }

  private boolean isClusterMember(final String host) {
    for (InetAddress addr : cluster.getClusterInfo().getAddresses()) {
      if (host.equals(addr.getHostAddress())) {
        return true;
      }
    }
    return false;
  }

  @Path("{node}")
  public JmxRestApi handleGet(@PathParam("node") final String node,
          @HeaderParam("Accept") @DefaultValue(MediaType.WILDCARD) final String accept) throws URISyntaxException {
    if (!isClusterMember(node)) {
      throw new NotFoundException("No such host: " + node);
    }
    return WebResourceFactory.newResource(JmxRestApi.class, httpClient.target(
            new URI(protocol, null, node, port, "/jmx/local", null, null)));

  }

}
