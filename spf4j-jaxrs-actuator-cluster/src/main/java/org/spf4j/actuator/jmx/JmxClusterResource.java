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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.api.Immediate;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.http.Headers;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.server.StreamedResponseContent;

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

  private URI buildURI(final List<PathSegment> path) throws URISyntaxException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    NetworkService service = clusterInfo.getHttpService();
    String tPath = "/jmx/local";
    if (path.size() > 1) {
      String restpath = path.subList(1, path.size()).stream().map(x -> x.getPath())
              .collect(Collectors.joining("/"));
      tPath  = tPath + '/' + restpath;
    }
    URI uri = new URI(service.getName(), null,  path.get(0).getPath(), service.getPort(), tPath, null, null);
    return uri;
  }

  @Path("{nodePath:.*}")
  @GET
  public Response handleGet(@PathParam("nodePath") final List<PathSegment> path,
          @HeaderParam("Accept") @DefaultValue(MediaType.WILDCARD) final String accept) throws URISyntaxException {
    URI uri = buildURI(path);
    Response resp = httpClient.target(uri).request(accept).get(Response.class);
    return Response.ok(new StreamedResponseContent(() -> resp.readEntity(InputStream.class)),
            resp.getHeaderString("Content-Type"))
            .header(Headers.CONTENT_SCHEMA, resp.getHeaderString(Headers.CONTENT_SCHEMA))
            .build();
  }

  @Path("{nodePath:.*}")
  @POST
  public Response handlePostPut(@PathParam("nodePath") final List<PathSegment> path,
          @HeaderParam("Accept") @DefaultValue(MediaType.WILDCARD) final String accept,
          @HeaderParam("Content-Type") final String contentType,
          @HeaderParam(Headers.CONTENT_SCHEMA) final String schema,
          final InputStream is) throws URISyntaxException {
    URI uri = buildURI(path);
    final Response resp = httpClient.target(uri)
            .request(accept).header(Headers.CONTENT_SCHEMA, schema)
            .post(Entity.entity(is, contentType));
    return Response.ok(new StreamedResponseContent(() -> resp.readEntity(InputStream.class)),
            resp.getHeaderString("Content-Type"))
            .header(Headers.CONTENT_SCHEMA, resp.getHeaderString(Headers.CONTENT_SCHEMA))
            .build();
  }



}
