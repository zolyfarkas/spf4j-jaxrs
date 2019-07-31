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

import com.google.common.collect.Range;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.spf4j.base.SuppressForbiden;
import org.spf4j.base.avro.FileEntry;
import org.spf4j.base.avro.FileType;
import org.spf4j.http.HttpRange;
import org.spf4j.jaxrs.server.StreamedResponseContent;

/**
 * A naive implementation of a file tree REST "browser"
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@SuppressFBWarnings("JAXRS_ENDPOINT") // will need to think about this...
public class FilesResource {

  private final Path base;

  private final boolean listDirectoryContents;

  public FilesResource(final Path local) {
    this(local, true);
  }

  public FilesResource(final Path local, final boolean listDirectoryContents) {
    this.base = local;
    this.listDirectoryContents = listDirectoryContents;
  }

  @javax.ws.rs.Path("{path:.*}")
  @GET
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // try-resources gen code
  @SuppressForbiden // java.util.Date is my only choice.
  public Response get(@PathParam("path") final List<PathSegment> path,
          @HeaderParam("Range") final HttpRange range, @Context final Request request) throws IOException {
    Path ltarget = base;
    for (PathSegment part : path) {
      String p = part.getPath();
      if ("..".equals(p)) {
        throw new ClientErrorException("Path " + path + " contains backreferences", 400);
      }
      ltarget = ltarget.resolve(p);
    }
    final Path target = ltarget;
    if (Files.isDirectory(target)) {
      if (!this.listDirectoryContents) {
        throw new ForbiddenException("Directory listing not allowed for " + path);
      }
      List<FileEntry> result = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
        for (Path elem : stream) {
          Path fileName = elem.getFileName();
          if (fileName == null) {
            throw new IllegalStateException("Dir entry should not be empty " + elem);
          }
          result.add(new FileEntry(Files.isDirectory(elem) ? FileType.DIRECTORY : FileType.REGULAR,
                  fileName.toString(), Files.size(elem), Files.getLastModifiedTime(elem).toInstant()));
        }
      }
      return Response.ok(result, MediaType.APPLICATION_JSON).build();
    } else {
        Date lastModifiedTime = new Date(Files.getLastModifiedTime(target).toMillis());
        Response.ResponseBuilder rb = request.evaluatePreconditions(lastModifiedTime);
        if (rb != null) {
          return rb.build();
        }
        MediaType fileMediaType = getFileMediaType(target);
        if (range != null && range.isByteRange()) {
        List<Range<Long>> ranges = range.getRanges();
        if (ranges.size() == 1) {
          final Range<Long> r = ranges.get(0);
          return Response.status(206).entity(new StreamedResponseContent(
                  () -> new BufferedInputStream(Files.newInputStream(target)), r.lowerEndpoint(), r.upperEndpoint()))
                  .type(fileMediaType)
                  .lastModified(lastModifiedTime)
                  .header("Accept-Ranges", "bytes")
                  .header("Content-Range", "bytes " + r.lowerEndpoint() + '-' + r.upperEndpoint() + "/*")
                  .header("Content-Disposition", "attachment; filename=\"" + target.getFileName() + "\"")
                  .build();
        }
      }
      return Response.ok(new StreamedResponseContent(() -> new BufferedInputStream(Files.newInputStream(target))),
              fileMediaType)
              .lastModified(lastModifiedTime)
              .header("Accept-Ranges", "bytes")
              .header("Content-Disposition", "attachment; filename=\"" + target.getFileName() + "\"")
              .build();
    }
  }

  private static MediaType getFileMediaType(final Path filePath) {
      Path fName = filePath.getFileName();
      if (fName == null) {
        throw new IllegalArgumentException("Invalid file " + filePath);
      }
      String fileName = fName.toString();
      int dIdx = fileName.lastIndexOf('.');
      if (dIdx < 0) {
        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }
      String ext = fileName.substring(dIdx + 1);
      MediaType mt = org.spf4j.jaxrs.server.MediaTypes.fromExtension(ext);
      if (mt == null) {
        return  MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }
      return mt;
  }

  @Override
  public String toString() {
    return "FilesResource{" + "base=" + base + '}';
  }

}
