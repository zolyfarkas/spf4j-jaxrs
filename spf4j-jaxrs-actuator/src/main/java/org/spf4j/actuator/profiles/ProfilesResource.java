package org.spf4j.actuator.profiles;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import java.io.IOException;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;

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

  @Inject
  public ProfilesResource(final LogsResource logsResource) {
    this.logsResource = logsResource;
  }

  @Path("local/{trId}")
  @GET
  @Produces(value = {"application/stack.samples+json", "application/stack.samples.d3+json"})
  public SampleNode getSamples(@PathParam("trId") final String traceId) throws IOException {
    StringBuilder sb = new StringBuilder(traceId.length());
    AppendableUtils.escapeJsonString(traceId, sb);
    List<LogRecord> logs = logsResource.getLocalLogs(0, 10, "log.stackSamples.length != 0 and log.trId == \""
            +  sb + "\"", Order.DESC);
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

  @Path("local/visualize/{trId}")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String visualize(@PathParam("trId") final String traceId) throws IOException {
    Handlebars hb = new Handlebars(new ClassPathTemplateLoader("", ""));
    Template t = hb.compile("/org/spf4j/actuator/profiles/FlameGraph.html");
    return t.apply(new Handlebars.SafeString(
            "/profiles/local/" + UriComponent.encode(traceId, UriComponent.Type.PATH_SEGMENT)
    + "?_Accept=application/stack.samples.d3%2Bjson"));
  }

}
