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
package org.spf4j.jaxrs.server.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.spf4j.base.CharSequences;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@SuppressFBWarnings("JAXRS_ENDPOINT")
public class ClassPathResource {

  private final String cpBase;

  public ClassPathResource(final String cpBase) {
    this.cpBase = cpBase;
  }

  @Override
  public String toString() {
    return "ClassPathResource{" + "cpBase=" + cpBase + '}';
  }

  @GET
  @Path("{path:.*}")
  public Response staticResources(@PathParam("path") final String path) {
    final InputStream resource = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(cpBase + '/' + CharSequences.validatedFileName(path));
    return null == resource
        ? Response.status(404).build()
        : Response.ok().entity(resource).build();
  }

}
