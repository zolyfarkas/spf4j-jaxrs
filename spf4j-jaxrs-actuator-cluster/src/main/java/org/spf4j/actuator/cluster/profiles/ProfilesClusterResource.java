package org.spf4j.actuator.cluster.profiles;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.actuator.cluster.logs.LogsClusterResource;
import org.spf4j.base.AppendableUtils;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.jaxrs.server.AsyncResponseWrapper;
import org.spf4j.ssdump2.Converter;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author Zoltan Farkas
 */
@Path("profiles")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
public class ProfilesClusterResource {

  private final LogsClusterResource logsResource;

  @Inject
  public ProfilesClusterResource(final LogsClusterResource logsResource) {
    this.logsResource = logsResource;
  }

  @Path("cluster/{trId}")
  @GET
  @Produces(value = {"application/stack.samples+json", "application/stack.samples.d3+json"})
  public void getSamples(@PathParam("trId") final String traceId, @Suspended final AsyncResponse ar)
          throws IOException, URISyntaxException {
    StringBuilder sb = new StringBuilder(traceId.length());
    AppendableUtils.escapeJsonString(traceId, sb);
    logsResource.getClusterLogs(10, "log.stackSamples.length != 0 and log.trId == \""
            + sb + "\"", Order.DESC, new AsyncResponseWrapper(ar) {
      @Override
      public boolean resume(final Object response) {
        List<LogRecord> logs = (List<LogRecord>) response;
        if (logs.isEmpty()) {
          return super.resume(null);
        }
        SampleNode result = Converter.convert(logs.get(0).getStackSamples().iterator());
        for (int i = 1, l = logs.size(); i < l; i++) {
          List<StackSampleElement> stackSamples = logs.get(i).getStackSamples();
          if (stackSamples.isEmpty()) {
            continue;
          }
          result = SampleNode.aggregate(result, Converter.convert(stackSamples.iterator()));
        }
        return super.resume(result);
      }

    });
  }

  @Path("cluster/visualize/{trId}")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String visualize(@PathParam("trId") final String traceId) throws IOException {
    Handlebars hb =  new Handlebars(new ClassPathTemplateLoader("", ""));
    Template t = hb.compile("/org/spf4j/actuator/profiles/FlameGraph.html");
    return t.apply(
            new Handlebars.SafeString(
            "/profiles/cluster/" + UriComponent.encode(traceId, UriComponent.Type.PATH_SEGMENT)
    + "?_Accept=application/stack.samples.d3%2Bjson"));
  }

}
