
package org.spf4j.jaxrs.server;

import javax.annotation.Nullable;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;

/**
 * A provider to allow injecting, ExecutionContexts.
 * @author Zoltan Farkas
 */
@Provider
public final class ExecutionContextResolver implements ContextResolver<ExecutionContext> {

  @Override
  @Nullable
  public ExecutionContext getContext(final Class<?> type) {
    if (type == ExecutionContext.class) {
      return ExecutionContexts.current();
    }
    return null;
  }

}
