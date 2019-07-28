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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CharSequences;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@SuppressFBWarnings("JAXRS_ENDPOINT")
public class ClassPathResource {

  private static final Logger LOG = LoggerFactory.getLogger(ClassPathResource.class);

  private final String cpBase;

  private final ClassLoader classLoader;

  public ClassPathResource(final String cpBase) {
    this(cpBase, Thread.currentThread().getContextClassLoader());
  }

  public ClassPathResource(final String cpBase, final ClassLoader classLoader) {
    this.cpBase = cpBase;
    this.classLoader = classLoader;
  }

  public String getCpBase() {
    return cpBase;
  }

  @Override
  public String toString() {
    return "ClassPathResource{" + "cpBase=" + cpBase + '}';
  }

  @GET
  @Path("{path:.*}")
  public Response staticResources(@PathParam("path")  final String path) throws IOException {
    URL resource = classLoader.getResource(cpBase + '/' + CharSequences.validatedFileName(path));
    if (resource == null) {
      return Response.status(404).build();
    }
    URLConnection conn = resource.openConnection();
    LOG.debug("Connection of type {}", conn.getClass());
    conn.connect();
    final InputStream is = conn.getInputStream();
    return Response.ok().entity(is).type(getPathMediaType(path)).build();
  }


  private static MediaType getPathMediaType(final String path) {
      int dIdx = path.lastIndexOf('.');
      if (dIdx < 0) {
        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }
      int sIdx = path.lastIndexOf('/');
      if (sIdx > dIdx) {
        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }
      String ext = path.substring(dIdx + 1);
      MediaType mt = org.spf4j.jaxrs.server.MediaTypes.fromExtension(ext);
      if (mt == null) {
        return  MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }
      return mt;
  }

}
