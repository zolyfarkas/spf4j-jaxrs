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
package org.spf4j.actuator.info;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import org.glassfish.hk2.api.Immediate;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.ProcessInfo;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 *
 * @author Zoltan Farkas
 */
@Path("info")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@Immediate
@RolesAllowed("operator")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class InfoResourceCluster {

  private final Spf4JClient httpClient;

  private final InfoResource resource;

  private final Cluster cluster;

  @Inject
  public InfoResourceCluster(final Cluster cluster,
          final Spf4JClient httpClient, final InfoResource resource) {
    this.httpClient = httpClient;
    this.resource = resource;
    this.cluster = cluster;
  }

  @Path("cluster")
  @GET
  public void getClusterInfo(@Suspended final AsyncResponse ar) throws URISyntaxException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    List<ProcessInfo> result = Collections.synchronizedList(new ArrayList(peerAddresses.size() + 1));
    result.add(resource.getProcessInfo(clusterInfo));
    CompletableFuture<List<ProcessInfo>> cf = ContextPropagatingCompletableFuture.completedFuture(result);
    NetworkService service = clusterInfo.getHttpService();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(service.getName(), null,
              addr.getHostAddress(), service.getPort(), "/info/local", null, null);
      cf = cf.thenCombine(httpClient.target(uri)
              .request("application/avro").rx().get(ProcessInfo.class),
              (res, info) -> {
                res.add(info);
                return res;
              });
    }
    cf.whenComplete((res, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(new org.spf4j.base.avro.ClusterInfo(resource.getApplicationInfo(), res));
      }
    });
  }
}
