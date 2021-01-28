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
package org.spf4j.api_browser;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.spf4j.jaxrs.server.resources.ClassPathResource;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@Path("/")
@PermitAll
public class ApiBrowserResource {

  private final ClassPathResource res;

  public ApiBrowserResource() {
    res = new ClassPathResource("org/spf4j/actuator/apiBrowser");
  }

  @Path("apiBrowser/")
  public ClassPathResource getUI() {
    return res;
  }

  @Path("apiBrowser")
  @GET
  public Response getUIIndex(@Context final UriInfo request,
          @HeaderParam("X-Forwarded-Proto") @Nullable final String forwardedProto)
          throws URISyntaxException {
    URI uri = request.getRequestUri();
    URI redirect = uri.resolve(uri.getPath() + '/' + "index.html");
    if (forwardedProto != null) {
      redirect = new URI(forwardedProto, redirect.getUserInfo(), redirect.getHost(),
              redirect.getPort(), redirect.getPath(),
              redirect.getQuery(), redirect.getFragment());
    }
    return Response.temporaryRedirect(redirect).build();
  }

}
