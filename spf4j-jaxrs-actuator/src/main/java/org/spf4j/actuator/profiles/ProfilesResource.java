package org.spf4j.actuator.profiles;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import gnu.trove.set.hash.THashSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.actuator.logs.LogFilesResource;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author Zoltan Farkas
 */
@Path("profiles")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed(JaxRsSecurityContext.OPERATOR_ROLE)
@Singleton
public class ProfilesResource {

  private final LogsResource logsResource;

  private final LogFilesResource logFilesResource;

  private final FlameGraphTemplate visualizePage;

  private final Sampler sampler;

  private final String hostName;

  @Inject
  public ProfilesResource(final LogsResource logsResource,
          final LogFilesResource logFilesResource, final Sampler sampler,
          @ConfigProperty(name = "hostName", defaultValue = "127.0.0.1") final String hostName) throws IOException {
    this.logsResource = logsResource;
    this.logFilesResource = logFilesResource;
    this.sampler = sampler;
    Handlebars hb = new Handlebars(new ClassPathTemplateLoader("", ""));
    visualizePage = hb.compile("/org/spf4j/actuator/profiles/FlameGraph.html").as(FlameGraphTemplate.class);
    this.hostName = hostName;
  }

  public FlameGraphTemplate getVisualizePage() {
    return visualizePage;
  }

  @Path("local/traces/{trId}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  @Nullable
  public SampleNode getSamples(@PathParam("trId") final String traceId) throws IOException {
    StringBuilder sb = new StringBuilder(traceId.length());
    AppendableUtils.escapeJsonString(traceId, sb);
    List<LogRecord> logs = logsResource.getLocalLogs(10, "log.stackSamples.length != 0 and log.trId == \""
            + sb + "\"", Order.DESC, null);
    if (logs.isEmpty()) {
      return null;
    }
    SampleNode result = Converter.convert(logs.get(0).getStackSamples().iterator());
    for (int i = 1, l = logs.size(); i < l; i++) {
      List<StackSampleElement> stackSamples = logs.get(i).getStackSamples();
      if (stackSamples.isEmpty()) {
        continue;
      }
      result = SampleNode.aggregate(result, Converter.convert(stackSamples.iterator()));
    }
    return result;
  }

  @Path("local/groups")
  @GET
  @Produces({"application/json", "application/avro"})
  public Set<String> getSampleLabels() throws IOException {
    java.nio.file.Path base = logFilesResource.getFiles().getBase();
    Set<String> result = new THashSet<>();
    result.addAll(sampler.getStackCollections().keySet());
    try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(base)) {
      for (java.nio.file.Path elem : stream) {
        if (Files.isDirectory(elem)) { // will not recurse for now.
          continue;
        }
        java.nio.file.Path fname = elem.getFileName();
        if (fname == null) {
          continue;
        }
        String fileName = fname.toString();
        if (fileName.endsWith(".ssdump3") || fileName.endsWith(".ssdump3.gz")) {
          Converter.loadLabels(elem.toFile(), result::add);
        } else if (fileName.endsWith(".ssdump2") || fileName.endsWith(".ssdump2.gz")) {
          result.add(Converter.getLabelFromSsdump2FileName(fileName));
        }
      }
    }
    return result;
  }

  @Path("local/groups/{label}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  public SampleNode getLabeledSamples(@PathParam("label") final String label,
          @Nullable @QueryParam("from") final Instant from,
          @Nullable @QueryParam("to") final Instant to) throws IOException {
    if (from == null && to == null) { // return current in memory samples.
      SampleNode samples = sampler.getStackCollections().get(label);
      if (samples == null) {
        throw new NotFoundException("No currernt samples for: " + label);
      }
      return samples;
    } else {
      SampleNode samples = null;
      if (to == null) {
        samples = sampler.getStackCollections().get(label);
      }
      java.nio.file.Path base = logFilesResource.getFiles().getBase();
      try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(base)) {
        for (java.nio.file.Path elem : stream) {
          if (Files.isDirectory(elem)) { // will not recurse for now.
            continue;
          }
          java.nio.file.Path elemName = elem.getFileName();
          if (elemName == null) {
            continue;
          }
          String fileName = elemName.toString();
          if (fileName.endsWith(".ssdump3") || fileName.endsWith(".ssdump3.gz")) {
            if (inRange(elem, from, to)) {
              samples = SampleNode.aggregateNullableUnsafe(samples, Converter.loadLabeledDump(elem.toFile(), label));
            }
          } else if (fileName.endsWith(".ssdump2") || fileName.endsWith(".ssdump2.gz")) {
            String fileLabel = Converter.getLabelFromSsdump2FileName(fileName);
            if (label.equals(fileLabel) && inRange(elem, from, to)) {
              samples = SampleNode.aggregateNullableUnsafe(samples, Converter.load(elem.toFile()));
            }
          }
        }
      }
      if (samples == null) {
        throw new NotFoundException("No samples for: " + label + " in range [" + from + ", " + to + ']');
      }
      return samples;
    }
  }

  private static boolean inRange(final java.nio.file.Path file,
          @Nullable final Instant from, @Nullable final Instant to) throws IOException {
    Instant lastModifiedTime = Files.getLastModifiedTime(file).toInstant();
    if (from != null) {
      if (lastModifiedTime.isBefore(from)) {
        return false;
      }
    }
    if (to != null) {
      if (lastModifiedTime.isAfter(to)) {
        return false;
      }
    }
    return true;
  }

  @Path("local/visualize/traces/{trId}")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public StreamingOutput visualizeTraces(@PathParam("trId") final String traceId) throws IOException {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream os) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
          visualizePage.apply(new FlameGraphParams("Request profile for: " + traceId,
                  "/profiles/local/traces/" + UriComponent.encode(traceId, UriComponent.Type.PATH_SEGMENT)
                  + "?_Accept=application/stack.samples.d3%2Bjson"), bw);
        }
      }
    };
  }

  @Path("local/visualize/groups/{label}")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public StreamingOutput visualizeGroups(@PathParam("label") final String label,
          @Nullable @QueryParam("from") final Instant from,
          @Nullable @QueryParam("to") final Instant to) throws IOException {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream os) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
          StringBuilder url = new StringBuilder(64);
          url.append("/profiles/local/groups/").append(UriComponent.encode(label, UriComponent.Type.PATH_SEGMENT))
                  .append("?_Accept=application/stack.samples.d3%2Bjson");
          if (from != null) {
            url.append("&from=");
            url.append(UriComponent.encode(from.toString(), UriComponent.Type.QUERY_PARAM));
          }
          if (to != null) {
            url.append("&to=");
            url.append(UriComponent.encode(to.toString(), UriComponent.Type.QUERY_PARAM));
          }
          visualizePage.apply(new FlameGraphParams("Node profile for: " + hostName, url), bw);
        }
      }
    };

  }

  @Override
  public String toString() {
    return "ProfilesResource{" + "logsResource=" + logsResource + ", logFilesResource="
            + logFilesResource + ", visualizePage=" + visualizePage
            + ", sampler=" + sampler + ", hostName=" + hostName + '}';
  }



}
