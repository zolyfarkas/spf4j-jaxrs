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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.spf4j.base.SuppressForbiden;
import org.spf4j.service.avro.FileEntry;
import org.spf4j.service.avro.FileType;
import org.spf4j.http.HttpRange;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.server.StreamedResponseContent;
import org.spf4j.jaxrs.server.security.SecuredInternaly;
import org.spf4j.security.AbacSecurityContext;

/**
 * A naive implementation of a file tree REST "browser"
 *
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@SuppressFBWarnings({ "JAXRS_ENDPOINT", "JXI_INVALID_CONTEXT_PARAMETER_TYPE" }) // will need to think about this...
@SecuredInternaly
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

  @GET
  public Response get(@HeaderParam("Range") @Nullable final HttpRange range,
          @Context final Request request, @Context final JaxRsSecurityContext secCtx)
          throws IOException {
    return get(Collections.emptyList(), range, request, secCtx);
  }

  @javax.ws.rs.Path("{path:.*}")
  @GET
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // try-resources gen code
  @SuppressForbiden // java.util.Date is my only choice.
  public Response get(@PathParam("path") final List<PathSegment> path,
          @HeaderParam("Range") @Nullable final HttpRange range,
          @Context final Request request, @Context final JaxRsSecurityContext secCtx)
          throws IOException {
    Path ltarget = resolveToPath(path);
    final Path target = ltarget;
    if (!secCtx
            .canAccess(AbacSecurityContext.resource("path", target.toString()),
                    AbacSecurityContext.action("read"), new Properties())) {
      return Response.status(403, "Denied access to: " + target).build();
    }
    if (Files.isDirectory(target)) {
      if (!this.listDirectoryContents) {
        return Response.status(403, "Directory listing not allowed for " + path).build();
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

  private Path resolveToPath(final List<PathSegment> path) {
    Path ltarget = base;
    for (PathSegment part : path) {
      String p = part.getPath();
      if ("..".equals(p)) {
        throw new ClientErrorException("Path " + path + " contains backreferences", 400);
      }
      ltarget = ltarget.resolve(p);
    }
    return ltarget;
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

  public Path getBase() {
    return base;
  }

  public boolean isListDirectoryContents() {
    return listDirectoryContents;
  }

  @Override
  public String toString() {
    return "FilesResource{" + "base=" + base + '}';
  }

}
