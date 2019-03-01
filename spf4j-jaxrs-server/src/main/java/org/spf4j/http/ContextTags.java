
package org.spf4j.http;

import java.util.ArrayList;
import java.util.List;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContext.Tag;
import org.spf4j.log.Level;
import org.spf4j.servlet.CountingHttpServletRequest;
import org.spf4j.servlet.CountingHttpServletResponse;

/**
 * Custom
 * @author Zoltan Farkas
 */
public final class ContextTags {

  private ContextTags() { }

  /**
   * Additional log attributes (objects) that will be logged in the standard log entry for the context.
   */
  public static final Tag<List<Object>> LOG_ATTRIBUTES = new Tag<List<Object>>() {
    @Override
    public String toString() {
      return "LA";
    }

    @Override
    public List<Object> combine(List<Object> existing, List<Object> current) {
      if (existing == null) {
        return current;
      } else {
        List<Object> result = new ArrayList<>(existing.size() + current.size());
        result.addAll(existing);
        result.addAll(current);
        return result;
      }
    }

    @Override
    public boolean pushOnClose(ExecutionContext.Relation relation) {
      return relation == ExecutionContext.Relation.CHILD_OF;
    }
  };

  /**
   * Http warnings attached to current execution context.
   */
  public static final Tag<List<HttpWarning>> HTTP_WARNINGS =  new Tag<List<HttpWarning>>() {
    @Override
    public String toString() {
      return "HW";
    }

    @Override
    public List<HttpWarning> combine(List<HttpWarning> existing, List<HttpWarning> current) {
      if (existing == null) {
        return current;
      } else {
        List<HttpWarning> result = new ArrayList<>(existing.size() + current.size());
        result.addAll(existing);
        result.addAll(current);
        return result;
      }
    }

    @Override
    public boolean pushOnClose(ExecutionContext.Relation relation) {
      return relation == ExecutionContext.Relation.CHILD_OF;
    }


  };

  /**
   * Upgrade the log level of the standard LOG entry for the context.
   */
  public static final Tag<Level> LOG_LEVEL = new Tag<Level>() {
    @Override
    public String toString() {
      return "LL";
    }

    @Override
    public Level combine(Level existing, Level current) {
      if (existing == null) {
        return current;
      }
      return (existing.ordinal() < current.ordinal()) ? current : existing;
    }

    @Override
    public boolean pushOnClose(ExecutionContext.Relation relation) {
      return relation == ExecutionContext.Relation.CHILD_OF;
    }

  };


  public static final Tag<CountingHttpServletRequest> HTTP_REQ = new Tag<CountingHttpServletRequest>() {
    @Override
    public String toString() {
      return "HREQ";
    }
  };

  public static final Tag<CountingHttpServletResponse> HTTP_RESP = new Tag<CountingHttpServletResponse>() {
    @Override
    public String toString() {
      return "HRESP";
    }
  };


}
