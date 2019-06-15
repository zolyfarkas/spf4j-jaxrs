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
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.spf4j.base.avro.FileEntry;
import org.spf4j.base.avro.FileType;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.server.StreamedResponseContent;

/**
 * @author Zoltan Farkas
 */
@Path("logFiles/cluster")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class LogFilesClusterResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  @Inject
  public LogFilesClusterResource(
          final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
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
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    NetworkService service = clusterInfo.getHttpService();
    String tPath = "/logFiles/local/";
    if (path.size() > 1) {
      String restpath = path.subList(1, path.size()).stream().map(x -> x.getPath())
              .collect(Collectors.joining("/"));
      tPath  = tPath + restpath;
    }
    URI uri = new URI(service.getName(), null,  path.get(0).getPath(), service.getPort(), tPath, null, null);
    Response resp = httpClient.target(uri).request(MediaType.WILDCARD).get(Response.class);
    return Response.ok(new StreamedResponseContent(() -> resp.readEntity(InputStream.class)),
            resp.getHeaderString("Content-Type")).build();

  }

}
