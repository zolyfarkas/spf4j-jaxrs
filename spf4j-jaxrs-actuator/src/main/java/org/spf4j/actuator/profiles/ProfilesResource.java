package org.spf4j.actuator.profiles;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import gnu.trove.set.hash.THashSet;
import java.io.IOException;
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
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.actuator.logs.LogFilesResource;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author Zoltan Farkas
 */
@Path("profiles")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
@Singleton
public class ProfilesResource {

  private final LogsResource logsResource;

  private final LogFilesResource logFilesResource;

  private final Template visualizePage;

  private final Sampler sampler;

  @Inject
  public ProfilesResource(final LogsResource logsResource,
          final LogFilesResource logFilesResource, final Sampler sampler) throws IOException {
    this.logsResource = logsResource;
    this.logFilesResource = logFilesResource;
    this.sampler = sampler;
    Handlebars hb = new Handlebars(new ClassPathTemplateLoader("", ""));
    visualizePage = hb.compile("/org/spf4j/actuator/profiles/FlameGraph.html");
  }

  @Path("local/traces/{trId}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  public SampleNode getSamples(@PathParam("trId") final String traceId) throws IOException {
    StringBuilder sb = new StringBuilder(traceId.length());
    AppendableUtils.escapeJsonString(traceId, sb);
    List<LogRecord> logs = logsResource.getLocalLogs(0, 10, "log.stackSamples.length != 0 and log.trId == \""
            + sb + "\"", Order.DESC);
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


  @Path("local/labels/{label}")
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
        String fileName = elem.getFileName().toString();
        if (fileName.endsWith(".ssdump3") || fileName.endsWith(".ssdump3.gz")) {
          Converter.loadLabels(elem.toFile(), result::add);
        } else if (fileName.endsWith(".ssdump2")  || fileName.endsWith(".ssdump2.gz")) {
          result.add(Converter.getLabelFromSsdump2FileName(fileName));
        }
      }
    }
    return result;
  }

  @Path("local/labels/{label}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  public SampleNode getLabeledSamples(@PathParam("label") final String label,
          @Nullable @QueryParam("from") Instant from,
          @Nullable @QueryParam("to") Instant to) throws IOException {
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
          String fileName = elem.getFileName().toString();
          if (fileName.endsWith(".ssdump3") || fileName.endsWith(".ssdump3.gz")) {
            if (inRange(elem, from, to)) {
              samples = SampleNode.aggregateNullable(samples, Converter.loadLabeledDump(elem.toFile(), label));
            }
          } else if (fileName.endsWith(".ssdump2")  || fileName.endsWith(".ssdump2.gz")) {
            String fileLabel = Converter.getLabelFromSsdump2FileName(fileName);
            if (label.equals(fileLabel) && inRange(elem, from, to)) {
              samples = SampleNode.aggregateNullable(samples, Converter.load(elem.toFile()));
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
  public String visualize(@PathParam("trId") final String traceId) throws IOException {
    return visualizePage.apply(new Handlebars.SafeString(
            "/profiles/local/" + UriComponent.encode(traceId, UriComponent.Type.PATH_SEGMENT)
            + "?_Accept=application/stack.samples.d3%2Bjson"));
  }

}
