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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.api.Immediate;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.spf4j.actuator.jmx.JmxRestApi;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 * @author Zoltan Farkas
 */
@Path("jmx/cluster")
@Immediate
@RolesAllowed("operator")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Beta
public class JmxClusterResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  @Inject
  public JmxClusterResource(final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
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


  @Path("{node}")
  public JmxRestApi handleGet(@PathParam("node") final String node,
          @HeaderParam("Accept") @DefaultValue(MediaType.WILDCARD) final String accept) throws URISyntaxException {
    NetworkService httpService = cluster.getClusterInfo().getHttpService();
    try {
      return WebResourceFactory.newResource(JmxRestApi.class, httpClient.target(
            new URI(httpService.getName(), null, node, httpService.getPort(), "/jmx/local", null, null)));
    } catch (WebApplicationException ex) {
      Response response = ex.getResponse();
      ServiceError re;
      try {
        re = response.readEntity(ServiceError.class);
      } catch (RuntimeException ex2) {
        // not a standard Error.
        throw new WebApplicationException("Error while accessing node " + node,
              response.getStatus());
      }
      throw new WebApplicationException("Error while accessing node " + node,
              Response.status(response.getStatus()).entity(re).build());
    }
  }


}
