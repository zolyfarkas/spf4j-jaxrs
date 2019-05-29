
package org.glassfish.grizzly.servlet;

import javax.servlet.Servlet;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * @author Zoltan Farkas
 */
public final class FixedWebappContext extends WebappContext {

  public FixedWebappContext(final String displayName) {
    super(displayName);
  }

  public FixedWebappContext(final String displayName, final String contextPath) {
    super(displayName, contextPath);
  }

  public FixedWebappContext(final String displayName, final String contextPath, final String basePath) {
    super(displayName, contextPath, basePath);
  }

  @Override
  protected Servlet createServletInstance(final ServletRegistration registration) throws Exception {
    ServletContainer srvlet = (ServletContainer) super.createServletInstance(registration);
    registration.servlet = srvlet;
    return srvlet;
  }


}
