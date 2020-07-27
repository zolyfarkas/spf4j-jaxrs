
package org.spf4j.jaxrs.server.providers;

import java.util.function.Supplier;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.http.ContextTags;
import org.spf4j.jaxrs.JaxRsSecurityContext;

/**
 * A provider to allow injecting, JaxRsSecurityContext.
 * @author Zoltan Farkas
 */
public final class JaxRsAbacSecurityContextResolver implements Supplier<JaxRsSecurityContext> {

  @Override
  public JaxRsSecurityContext get() {
      ExecutionContext ec = ExecutionContexts.current();
      if (ec == null) {
        return null;
      }
      return ec.get(ContextTags.SECURITY_CONTEXT);
  }


}
