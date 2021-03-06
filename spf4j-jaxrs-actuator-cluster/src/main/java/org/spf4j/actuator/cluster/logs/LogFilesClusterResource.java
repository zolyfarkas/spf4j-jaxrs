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
package org.spf4j.actuator.cluster.logs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.service.avro.FileEntry;
import org.spf4j.service.avro.FileType;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.server.StreamedResponseContent;

/**
 * @author Zoltan Farkas
 */
@Path("logFiles/cluster")
@RolesAllowed(JaxRsSecurityContext.OPERATOR_ROLE)
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Singleton
public class LogFilesClusterResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final int port;

  private final String protocol;

  @Inject
  public LogFilesClusterResource(
          final Cluster cluster, final Spf4JClient httpClient,
          @ConfigProperty(name = "servlet.port") final int port,
          @ConfigProperty(name = "servlet.protocol") final String protocol) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.port = port;
    this.protocol = protocol;
  }

  @Produces({ "application/json", "application/octet-stream" })
  @GET
  public List<FileEntry> getClusterNodes() {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> addresses = clusterInfo.getAddresses();
    List<FileEntry> result = new ArrayList<>(addresses.size());
    for (InetAddress addr : addresses) {
      result.add(new FileEntry(FileType.DIRECTORY, addr.getHostAddress(), -1, null));
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

  @Operation(
          description = "Get cluster information.",
          responses = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(
                                    schema = @Schema(implementation = FileEntry.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/octet-stream"
                    )
            )
         }
  )
  @Produces({ "application/json", "application/octet-stream" })
  @Path("{nodePath:.*}")
  @GET
  public Response getNodesDetail(@PathParam("nodePath") final List<PathSegment> path) throws URISyntaxException {
    if (path == null || path.isEmpty()) {
      return Response.ok(getClusterNodes(), MediaType.APPLICATION_JSON).build();
    }
    String tPath = "/logFiles/local/";
    if (path.size() > 1) {
      String restpath = path.subList(1, path.size()).stream().map(x -> x.getPath())
              .collect(Collectors.joining("/"));
      tPath  = tPath + restpath;
    }
    String host = path.get(0).getPath();
    if (!isClusterMember(host)) {
      throw new NotFoundException("No such host: " + host);
    }
    URI uri = new URI(protocol, null, host, port, tPath, null, null);
    Response resp = httpClient.target(uri).request(MediaType.WILDCARD).get(Response.class);
    return Response.ok(new StreamedResponseContent(() -> resp.readEntity(InputStream.class)),
            resp.getHeaderString(HttpHeaders.CONTENT_TYPE)).build();

  }

}
