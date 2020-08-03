
package org.spf4j.servlet;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.SecurityContext;
import org.spf4j.base.Wrapper;

/**
 * @author Zoltan Farkas
 */
public final class CountingHttpServletRequest extends HttpServletRequestWrapper
        implements Wrapper<HttpServletRequest> {

  private CountingServletInputStream is;

  private final SecurityContext secCtx;

  public CountingHttpServletRequest(final HttpServletRequest request, final SecurityContext secCtx) {
    super(request);
    this.secCtx = secCtx;
  }

  @Override
  public synchronized ServletInputStream getInputStream() throws IOException {
    if (is == null) {
      is = new CountingServletInputStream(super.getInputStream());
    }
    return is;
  }

  public synchronized long getBytesRead() {
    return is == null ? 0 : is.getCount();
  }

  @Override
  public Principal getUserPrincipal() {
    return secCtx.getUserPrincipal();
  }

  @Override
  public boolean isUserInRole(final String role) {
    return secCtx.isUserInRole(role);
  }

  @Override
  public String getAuthType() {
    return secCtx.getAuthenticationScheme();
  }

  @Override
  public boolean isSecure() {
    return secCtx.isSecure();
  }

  @Override
  public String toString() {
    return "CountingHttpServletRequest{" + "is=" + is + '}';
  }

  @Override
  public HttpServletRequest getWrapped() {
    return (HttpServletRequest) super.getRequest();
  }



}
