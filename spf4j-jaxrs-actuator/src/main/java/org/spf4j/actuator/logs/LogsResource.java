package org.spf4j.actuator.logs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.Order;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.log.AvroDataFileAppender;
import org.spf4j.log.LogbackUtils;
import org.spf4j.os.OperatingSystem;
import org.spf4j.zel.vm.CompileException;
import org.spf4j.zel.vm.Program;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logs/local")
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
@RolesAllowed("operator")
@Singleton
public class LogsResource {

  private final String hostName;

  @Inject
  public LogsResource(@ConfigProperty(name = "hostName", defaultValue = "") final String hostName) {
    this.hostName = hostName.isEmpty() ? OperatingSystem.getHostName() : hostName;
  }

  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @ProjectionSupport
  public List<LogRecord> getLocalLogs(
          @QueryParam("limit") @DefaultValue("25") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @QueryParam("order") @DefaultValue("DESC") final Order resOrder,
          @QueryParam("sort") @Nullable final String sort) throws IOException {
    return getLocalLogs(limit, filter, resOrder, sort, "default");
  }

  @Path("{appenderName}")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  @ProjectionSupport
  public List<LogRecord> getLocalLogs(
          @QueryParam("limit") @DefaultValue("25") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @QueryParam("order") @DefaultValue("DESC") final Order resOrder,
          @QueryParam("sort") @Nullable final String sort,
          @PathParam("appenderName") final String appenderName) throws IOException {
    if (limit == 0) {
      return Collections.emptyList();
    }
    if (limit < 0) {
      throw new ClientErrorException("limit parameter must be positive: " + limit, 400);
    }
    Map<String, AvroDataFileAppender> appenders = LogbackUtils.getConfiguredFileAppenders();
    AvroDataFileAppender fa = appenders.get(appenderName);
    if (fa == null) {
      throw new NotFoundException("Resource not available: " + appenderName);
    }
    TopAccumulator acc;
    if (sort != null) {
       acc = new TopAccumulator(sort, resOrder, limit);
    } else {
       acc = new TopAccumulator(limit, LogRecord::getTs, resOrder);
    }
    if (filter != null) {
      try {
        fa.getFilteredLogs(hostName, 0, Long.MAX_VALUE, Program.compilePredicate(filter, "log"), acc);
      } catch (CompileException ex) {
        throw new ClientErrorException("Invalid filter " + filter + ", " + ex.getMessage(), 400, ex);
      }
    } else {
      fa.getLogs(hostName, 0, Long.MAX_VALUE, acc);
    }
    return acc.getRecords();
  }

  public interface LogAccumulator extends Consumer<LogRecord>, Supplier<List<LogRecord>> {
    default void acceptAll(final Iterable<LogRecord> t) {
      for (LogRecord r : t) {
        this.accept(r);
      }
    }
  }

  public static Function<LogRecord, Comparable> getFieldExtractor(final String topField) {
      Program program;
      try {
        program = Program.compile(topField, "log");
      } catch (CompileException ex) {
        throw new IllegalArgumentException("Invalid top expression: " + topField, ex);
      }
      return (log) -> {
        try {
          return (Comparable) program.execute(log);
        } catch (ExecutionException ex) {
          throw new IllegalArgumentException("Problematic top expression: " + topField, ex);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        }
      };
    }

  public static class TopAccumulator implements LogAccumulator {

    private final int topNumber;
    private final Function<LogRecord, Comparable> topField;
    private final Order order;
    private final PriorityQueue<LogRecord> queue;
    private final Comparator<LogRecord> desiredOrder;

    public TopAccumulator(final String sortExpression, final Order order, final int limit) {
      this(limit, getFieldExtractor(sortExpression), order);
    }

    public TopAccumulator(final int topNumber, final Function<LogRecord, Comparable> topField,
            final Order order) {
      this.topNumber = topNumber;
      this.topField = topField;
      this.order = order;
      Comparator<LogRecord> comparator = (LogRecord a, LogRecord b) -> {
        return topField.apply(a).compareTo(topField.apply(b));
      };
      if (order == Order.ASC) {
        queue = new PriorityQueue<>(topNumber, comparator.reversed()); // always remove largest
        desiredOrder = comparator;
      } else { // DESC
        queue = new PriorityQueue<>(topNumber, comparator); // remove smallest
        desiredOrder = comparator.reversed();
      }
    }

    @Override
    public void accept(final LogRecord t) {
      if (topField.apply(t) == null) {
        return;
      }
      while (queue.size() >= topNumber) {
        queue.remove();
      }
      queue.add(t);
    }

    public Order getOrder() {
      return order;
    }

    public List<LogRecord> getRecords() {
      List<LogRecord> result = new ArrayList<>(queue);
      result.sort(desiredOrder);
      return result;
    }

    @Override
    public List<LogRecord> get() {
      return getRecords();
    }

  }

  @Override
  public String toString() {
    return "LogsResource{" + "hostName=" + hostName + '}';
  }

}
