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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.spf4j.base.CharSequences;
import org.spf4j.io.Streams;

/**
 * A naive implementation of a file tree "browser"
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@SuppressFBWarnings("JAXRS_ENDPOINT") // will need to think about this...
public class FilesResource {

  private final Path base;

  public FilesResource(final Path local) {
    this.base = local;
  }

  @javax.ws.rs.Path("{path:.*}")
  @GET
  @Produces({"application/json", "application/octet-stream"})
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // try-resources gen code
  public Response get(@PathParam("path") final List<String> path) throws IOException {
    Path target = base;
    for (String part : path) {
      CharSequences.validatedFileName(part);
      target = target.resolve(part);
    }
    if (Files.isDirectory(target)) {
      List<String> result = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
        for (Path elem : stream) {
          Path fileName = elem.getFileName();
          if (fileName == null) {
            throw new IllegalStateException("Dir entry should not be empty " + elem);
          }
          result.add(fileName.toString());
        }
      }
      return Response.ok(result, MediaType.APPLICATION_JSON).build();
    } else {
      return Response.ok(new StreamedFile(target), MediaType.APPLICATION_OCTET_STREAM)
              .header("Content-Disposition", "attachment; filename=\"" + target.getFileName() + "\"")
              .build();
    }
  }

  @Override
  public String toString() {
    return "FilesResource{" + "base=" + base + '}';
  }

  private static class StreamedFile implements StreamingOutput {

    private final Path ft;

    StreamedFile(final Path ft) {
      this.ft = ft;
    }

    @Override
    public void write(final OutputStream output) throws IOException {
      try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(ft))) {
        Streams.copy(bis, output);
      }
    }
  }

}
