package org.spf4j.actuator.profiles;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.hash.THashSet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
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
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ThreadLocalContextAttacher;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.AvroStackSampleSupplier;
import org.spf4j.stackmonitor.ProfilingTLAttacher;
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

  private final LogFilesResource logFilesResource;

  private final FlameGraphTemplate visualizePage;

  private final Sampler sampler;

  private final String hostName;

  @Inject
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ProfilesResource(final LogFilesResource logFilesResource, final Sampler sampler,
          @ConfigProperty(name = "hostName", defaultValue = "127.0.0.1") final String hostName) throws IOException {
    this.logFilesResource = logFilesResource;
    this.sampler = sampler;
    Handlebars hb = new Handlebars(new ClassPathTemplateLoader("", ""));
    visualizePage = hb.compile("/org/spf4j/actuator/profiles/FlameGraph.html").as(FlameGraphTemplate.class);
    this.hostName = hostName;
  }

  public FlameGraphTemplate getVisualizePage() {
    return visualizePage;
  }


  @Path("local/traces")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @ProjectionSupport
  public Map<String, List<DebugDetail>> getActiveRequests() {
    ThreadLocalContextAttacher threadLocalAttacher = ExecutionContexts.threadLocalAttacher();
    if (!(threadLocalAttacher instanceof ProfilingTLAttacher)) {
      throw new ClientErrorException("Request Profiling (ProfilingTLAttacher) not active, "
              + threadLocalAttacher.getClass() + " is running", 400);
    }
    ProfilingTLAttacher ptla = (ProfilingTLAttacher) threadLocalAttacher;
    Map<String, List<DebugDetail>> result = new HashMap<>();
    for (Map.Entry<Thread, ExecutionContext> entry : ptla.getCurrentThreadContexts()) {
      ExecutionContext ec = entry.getValue();
      String id = ec.getId().toString();
      List<DebugDetail> dd = result.get(id);
      if (dd == null) {
        dd = new ArrayList<>(2);
        result.put(id, dd);
      }
      dd.add(ec.getDebugDetail(hostName, null, true));
    }
    return result;
  }


  @Path("local/traces/{trId}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  public SampleNode getSamples(@PathParam("trId") final String traceId) throws IOException {
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
        if (fileName.endsWith("ssp.avro")) {
          AvroStackSampleSupplier ss = new AvroStackSampleSupplier(elem);
          SampleNode samples = ss.getSamples(null, traceId, Instant.MIN, Instant.MAX);
          if (samples != null) {
            return samples;
          }
        }
      }
    }
    throw new NotFoundException("No samples for trId = " + traceId);
  }

  @Path("local/groups")
  @GET
  @Produces({"application/json", "application/avro"})
  public Set<String> getSampleLabels(
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto) throws IOException {
    Instant now = Instant.now();
    final Instant from = pfrom == null ? now.minus(Duration.ofHours(1)) : pfrom;
    final Instant to = pto == null ? now : pfrom;
    java.nio.file.Path base = logFilesResource.getFiles().getBase();
    Set<String> result = new THashSet<>();
    result.addAll(sampler.getStackCollections().keySet());
    String filePrefix = sampler.getFilePrefix();
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
        if (!fname.startsWith(filePrefix)) {
          continue;
        }
        if (fileName.endsWith(".ssdump3") || fileName.endsWith(".ssdump3.gz")) {
          Converter.loadLabels(elem.toFile(), result::add);
        } else if (fileName.endsWith(".ssdump2") || fileName.endsWith(".ssdump2.gz")) {
          result.add(Converter.getLabelFromSsdump2FileName(fileName));
        } else if (fileName.endsWith("ssp.avro")) {
          AvroStackSampleSupplier ss = new AvroStackSampleSupplier(elem);
          result.addAll(ss.getMetaData(from, to).getContexts());
        }
      }
    }
    return result;
  }

  @Path("local/groups/{label}")
  @GET
  @Produces({"application/stack.samples+json", "application/stack.samples.d3+json"})
  public SampleNode getLabeledSamples(@PathParam("label") final String label,
          @Nullable @QueryParam("tag") final String tag,
          @Nullable @QueryParam("from") final Instant pfrom,
          @Nullable @QueryParam("to") final Instant pto) throws IOException {
    Instant from;
    if (pfrom == null) {
      from = Instant.now().minus(Duration.ofHours(1));
    } else {
      from = pfrom;
    }
    Instant to;
    if (pto == null) {
      to = Instant.now();
    } else {
      to = pto;
    }
    SampleNode samples = null;
    if (to == null || to.isAfter(sampler.getLastDumpInstant())) {
      samples = sampler.getStackCollections().get(label);
    }
    sampler.flushPersister();
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
        } else if (fileName.endsWith("ssp.avro")) {
          AvroStackSampleSupplier ss = new AvroStackSampleSupplier(elem);
          samples = SampleNode.aggregateNullableUnsafe(samples, ss.getSamples(label, tag, from, to));
        }
      }
    }
    if (samples == null) {
      throw new NotFoundException("No samples for: " + label + " in range [" + from + ", " + to + ']');
    }
    return samples;
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
    return "ProfilesResource{logFilesResource="
            + logFilesResource + ", visualizePage=" + visualizePage
            + ", sampler=" + sampler + ", hostName=" + hostName + '}';
  }



}
