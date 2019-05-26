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
package org.spf4j.actuator.apiBrowser;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import javax.inject.Inject;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.spf4j.jaxrs.RawSerialization;

@Path("/")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class OpenApiResource extends BaseOpenApiResource {

  private final ServletConfig config;

  private final Application app;

  @Inject
  public OpenApiResource(@Context final ServletConfig config, @Context final Application app) {
    this.config = config;
    this.app = app;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
  @Operation(hidden = true)
  @RawSerialization
  @Path("openapi.json")
  public Response getOpenApi(@Context final HttpHeaders headers,
          @Context final UriInfo uriInfo) throws Exception {
    return super.getOpenApi(headers, config, app, uriInfo, "json");
  }

  @GET
  @Produces("application/yaml")
  @Operation(hidden = true)
  @RawSerialization
  @Path("openapi.yaml")
  public Response getOpenApiYaml(@Context final HttpHeaders headers,
          @Context final UriInfo uriInfo) throws Exception {
    return super.getOpenApi(headers, config, app, uriInfo, "yamls");
  }

}
