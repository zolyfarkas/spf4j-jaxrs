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
package org.spf4j.actuator.logs;

import java.nio.file.Paths;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.jaxrs.server.resources.FilesResource;

/**
 * @author Zoltan Farkas
 */
@Path("logFiles")
@RolesAllowed("operator")
@Singleton
public final class LogFilesResource {

  private final FilesResource files;

  @Inject
  public LogFilesResource(@ConfigProperty(name = "application.logFilesPath", defaultValue = "/var/log")
    final String basePath) {
    this.files = new FilesResource(Paths.get(basePath));
  }

  @Path("local")
  public FilesResource getFiles() {
    return files;
  }

  @Override
  public String toString() {
    return "LogFilesResource{" + "files=" + files + '}';
  }
  
}
