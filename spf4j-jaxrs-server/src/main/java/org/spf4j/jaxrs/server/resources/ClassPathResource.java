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

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.server.security.SecuredInternaly;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.security.AbacSecurityContext;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@SuppressFBWarnings({ "JAXRS_ENDPOINT", "JXI_INVALID_CONTEXT_PARAMETER_TYPE" })
@SecuredInternaly
public class ClassPathResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(ClassPathResource.class));

  private final String cpBase;

  private final ClassLoader classLoader;

  private final List<String> welcomeFiles;

  public ClassPathResource(final String cpBase) {
    this(cpBase, Thread.currentThread().getContextClassLoader(), Collections.EMPTY_LIST);
  }


  public ClassPathResource(final String cpBase, final List<String> welcomeFiles) {
    this(cpBase, Thread.currentThread().getContextClassLoader(), welcomeFiles);
  }

  public ClassPathResource(final String cpBase, final ClassLoader classLoader, final List<String> welcomeFiles) {
    this.cpBase = cpBase;
    this.classLoader = classLoader;
    this.welcomeFiles = welcomeFiles;
  }

  public String getCpBase() {
    return cpBase;
  }

  public List<String> getWelcomeFiles() {
    return welcomeFiles;
  }

  @Override
  public String toString() {
    return "ClassPathResource{" + "cpBase=" + cpBase + '}';
  }

  static final String validateNoBackRef(final String path) {
    if (path.contains("/../") || path.startsWith("../") || path.endsWith("/..") || "..".equals(path)) {
      throw new ForbiddenException("Path " + path + " contains backreferences");
    }
    return path;
  }


  @GET
  @Path("{path:.*}")
  public Response staticResources(@PathParam("path") final String path,
          @Context final JaxRsSecurityContext secCtx) throws IOException {
    String urlStr;
    if (path.startsWith("/")) {
      urlStr = cpBase + validateNoBackRef(path);
    } else {
      urlStr = cpBase + '/' + validateNoBackRef(path);
    }
    if (!secCtx.canAccess(AbacSecurityContext.resource("url", urlStr),
            AbacSecurityContext.action("read"), new Properties())) {
      return Response.status(403, "Cannot access " + urlStr).build();
    }
    URL resource = null;
    if (urlStr.endsWith("/")) {
      if (welcomeFiles.isEmpty()) {
        throw new ForbiddenException("Directory listing not allowed for " + path);
      }
      for (String welcome : welcomeFiles) {
         resource = classLoader.getResource(urlStr + welcome);
         if (resource != null) {
           break;
         }
      }
    } else {
      resource = classLoader.getResource(urlStr);
    }
    if (resource == null) {
      return Response.status(404).build();
    }
    LOG.debug("Getting resource {}", resource);
    URLConnection conn = resource.openConnection();
    LOG.debug("Connection of type {}", conn.getClass());
    conn.connect();
    final InputStream is = conn.getInputStream();
    return Response.ok().entity(is).type(getPathMediaType(resource.getPath())).build();
  }


  @VisibleForTesting
  static MediaType getPathMediaType(final String path) {
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
