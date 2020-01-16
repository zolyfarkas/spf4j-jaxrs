
package org.spf4j.http;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.spf4j.base.ExecutionContext.SimpleTag;
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
  public static final Tag<List<Object>, Object> LOG_ATTRIBUTES = new Tag<List<Object>, Object>() {
    @Override
    public String toString() {
      return "LA";
    }

    @Override
    public List<Object> accumulate(final List<Object> existing, final List<Object> current) {
      if (existing == null) {
        return new ArrayList(current);
      } else {
        existing.addAll(current);
        return existing;
      }
    }

    @Override
    public List<Object> accumulateComponent(final List<Object> existing, final Object component) {
       List<Object> result;
      if (existing == null) {
        result = new ArrayList(2);
      } else {
        result = existing;
      }
      result.add(component);
      return result;
    }



    @Override
    public boolean pushOnClose() {
      return true;
    }
  };

  /**
   * Http warnings attached to current execution context.
   */
  public static final WarningsTag HTTP_WARNINGS =  new WarningsTag();

  /**
   * Upgrade the log level of the standard LOG entry for the context.
   */
  public static final SimpleTag<Level> LOG_LEVEL = new SimpleTag<Level>() {
    @Override
    public String toString() {
      return "LL";
    }

    @Override
    public Level accumulate(final Level existing, final Level current) {
      if (existing == null) {
        return current;
      }
      return (existing.ordinal() < current.ordinal()) ? current : existing;
    }

    @Override
    public boolean pushOnClose() {
      return true;
    }

  };


  public static final SimpleTag<CountingHttpServletRequest> HTTP_REQ = new SimpleTag<CountingHttpServletRequest>() {
    @Override
    public String toString() {
      return "HREQ";
    }
  };

  public static final SimpleTag<CountingHttpServletResponse> HTTP_RESP = new SimpleTag<CountingHttpServletResponse>() {
    @Override
    public String toString() {
      return "HRESP";
    }
  };

  public static final class WarningsTag implements Tag<Set<HttpWarning>, HttpWarning> {

    @Override
    public String toString() {
      return "HW";
    }

    @Override
    public Set<HttpWarning> accumulate(final Set<HttpWarning> existing, final Set<HttpWarning> current) {
      if (existing == null) {
        return new THashSet<>(current);
      } else {
        existing.addAll(current);
        return existing;
      }
    }

    @Override
    public Set<HttpWarning> accumulateComponent(final Set<HttpWarning> existing, final HttpWarning component) {
      Set<HttpWarning> result;
      if (existing == null) {
        result = new THashSet<>(2);
      } else {
        result = existing;
      }
      result.add(component);
      return result;
    }



    @Override
    public boolean pushOnClose() {
      return true;
    }
  }


}
