
package org.spf4j.jaxrs.server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import org.spf4j.base.Wrapper;

/**
 * A wrapper that by default delegates to the wrapped AsyncResponse.
 * this is a useful utility for simply extendings this and intercept certain calls.
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class AsyncResponseWrapper implements AsyncResponse, Wrapper<AsyncResponse> {

  private final AsyncResponse resp;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AsyncResponseWrapper(final AsyncResponse resp) {
    this.resp = resp;
  }

  @Override
  public boolean resume(final Object response) {
    return resp.resume(response);
  }

  @Override
  public boolean resume(final Throwable response) {
    return resp.resume(response);
  }

  @Override
  public boolean cancel() {
    return resp.cancel();
  }

  @Override
  public boolean cancel(final int retryAfter) {
    return resp.cancel(retryAfter);
  }

  @Override
  public boolean cancel(final Date retryAfter) {
    return resp.cancel(retryAfter);

  }

  @Override
  public boolean isSuspended() {
    return resp.isSuspended();
  }

  @Override
  public boolean isCancelled() {
    return resp.isCancelled();
  }

  @Override
  public boolean isDone() {
    return resp.isDone();
  }

  @Override
  public boolean setTimeout(final long time, final TimeUnit unit) {
    return resp.setTimeout(time, unit);
  }

  @Override
  public void setTimeoutHandler(final TimeoutHandler handler) {
    resp.setTimeoutHandler(handler);
  }

  @Override
  public Collection<Class<?>> register(final Class<?> callback) {
    return resp.register(callback);
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(final Class<?> callback, final Class<?>... callbacks) {
    return resp.register(callback, callbacks);
  }

  @Override
  public Collection<Class<?>> register(final Object callback) {
    return resp.register(callback);
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(final Object callback, final Object... callbacks) {
    return resp.register(callback, callbacks);
  }

  @Override
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public final AsyncResponse getWrapped() {
    return resp;
  }

  @Override
  public String toString() {
    return "AsyncResponseWrapper{" + "resp=" + resp + '}';
  }

}
