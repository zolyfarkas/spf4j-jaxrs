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

import io.swagger.v3.core.filter.OpenAPISpecFilter;
import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import static io.swagger.v3.jaxrs2.integration.ServletConfigContextUtils.getContextIdFromServletConfig;
import static io.swagger.v3.jaxrs2.integration.ServletConfigContextUtils.resolveResourcePackages;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.jaxrs.RawSerialization;

@Path("/")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class OpenApiResource extends BaseOpenApiResource {

  private static final Logger LOG = LoggerFactory.getLogger(OpenApiResource.class);

  private final ServletConfig config;

  private final Application app;

  @Inject
  public OpenApiResource(@Context final ServletConfig config, @Context final Application app) {
    this.config = config;
    this.app = app;
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(hidden = true)
  @RawSerialization
  @Path("openapi.json")
  public Response getOpenApi(@Context final HttpHeaders headers,
          @Context final UriInfo uriInfo) throws Exception {
    return getOpenApi(headers, config, app, uriInfo, "json");
  }

  @GET
  @Produces("application/yaml")
  @Operation(hidden = true)
  @RawSerialization
  @Path("openapi.yaml")
  public Response getOpenApiYaml(@Context final HttpHeaders headers,
          @Context final UriInfo uriInfo) throws Exception {
    return getOpenApi(headers, config, app, uriInfo, "yaml");
  }

  @Override
  protected Response getOpenApi(final HttpHeaders headers,
          final ServletConfig servletConfig,
          final Application application,
          final UriInfo uriInfo,
          final String type) throws Exception {
    String ctxId = getContextIdFromServletConfig(servletConfig);
    if (resourcePackages == null) {
      resourcePackages = resolveResourcePackages(servletConfig);
    }
    if (openApiConfiguration == null) {
      SwaggerConfiguration cfg = new SwaggerConfiguration()
              .resourcePackages(resourcePackages)
              .readerClass(CustomReader.class.getName());
      cfg.setId(ctxId);
      openApiConfiguration = cfg;
    }
    OpenApiContext ctx = new JaxrsOpenApiContextBuilder()
            .servletConfig(servletConfig)
            .application(application)
            .resourcePackages(resourcePackages)
            .configLocation(configLocation)
            .openApiConfiguration(openApiConfiguration)
            .ctxId(ctxId)
            .buildContext(true);
    OpenAPI oas = ctx.read();
    final boolean pretty;
    if (ctx.getOpenApiConfiguration() != null && Boolean.TRUE.equals(ctx.getOpenApiConfiguration().isPrettyPrint())) {
      pretty = true;
    } else {
      pretty = false;
    }

    if (oas != null) {
      if (ctx.getOpenApiConfiguration() != null && ctx.getOpenApiConfiguration().getFilterClass() != null) {
        try {
          OpenAPISpecFilter filterImpl = (OpenAPISpecFilter) Class.forName(
                  ctx.getOpenApiConfiguration().getFilterClass()).newInstance();
          SpecFilter f = new SpecFilter();
          oas = f.filter(oas, filterImpl, getQueryParams(uriInfo.getQueryParameters()), getCookies(headers),
                  getHeaders(headers));
        } catch (Exception e) {
          LOG.error("failed to load filter", e);
        }
      }
    }
    SpecFilter f = new SpecFilter();
    oas = f.filter(oas, new DefaultAspectsApiFilter(),
            getQueryParams(uriInfo.getQueryParameters()), getCookies(headers),
                  getHeaders(headers));
    if (oas == null) {
      return Response.status(404).build();
    }
    final OpenAPI result = oas;
    if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
      return Response.status(Response.Status.OK)
              .entity(new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                  if (pretty) {
                    Yaml.pretty().writeValue(os, result);
                  } else {
                    Yaml.mapper().writeValue(os, result);
                  }
                }
              })
              .type("application/yaml")
              .build();
    } else {
      return Response.status(Response.Status.OK)
              .entity(new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                  if (pretty) {
                    Json.pretty().writeValue(os, result);
                  } else {
                    Json.mapper().writeValue(os, result);
                  }
                }
              })
              .type(MediaType.APPLICATION_JSON_TYPE)
              .build();
    }
  }

  private static Map<String, List<String>> getQueryParams(final MultivaluedMap<String, String> params) {
    Map<String, List<String>> output = new HashMap<String, List<String>>();
    if (params != null) {
      for (String key : params.keySet()) {
        List<String> values = params.get(key);
        output.put(key, values);
      }
    }
    return output;
  }

  private static Map<String, String> getCookies(final HttpHeaders headers) {
    Map<String, String> output = new HashMap<String, String>();
    if (headers != null) {
      for (String key : headers.getCookies().keySet()) {
        Cookie cookie = headers.getCookies().get(key);
        output.put(key, cookie.getValue());
      }
    }
    return output;
  }

  private static Map<String, List<String>> getHeaders(final HttpHeaders headers) {
    Map<String, List<String>> output = new HashMap<String, List<String>>();
    if (headers != null) {
      for (String key : headers.getRequestHeaders().keySet()) {
        List<String> values = headers.getRequestHeaders().get(key);
        output.put(key, values);
      }
    }
    return output;
  }

}
