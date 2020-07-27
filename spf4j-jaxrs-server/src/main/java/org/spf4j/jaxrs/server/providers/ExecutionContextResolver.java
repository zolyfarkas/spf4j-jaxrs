
package org.spf4j.jaxrs.server.providers;

import java.util.function.Supplier;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;

/**
 * A provider to allow injecting, ExecutionContexts.
 * @author Zoltan Farkas
 */
public final class ExecutionContextResolver implements Supplier<ExecutionContext> {


  @Override
  public ExecutionContext get() {
      return ExecutionContexts.current();
  }

}
