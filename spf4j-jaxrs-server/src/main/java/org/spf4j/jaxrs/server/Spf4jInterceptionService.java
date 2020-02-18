package org.spf4j.jaxrs.server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InterceptionService;
import org.jvnet.hk2.internal.SystemDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ContextValue;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.http.ContextTags;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;
import org.spf4j.jaxrs.Timeout;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.log.Level;
import org.spf4j.servlet.CountingHttpServletResponse;

/**
 * Based on  https://blog.dejavu.sk/intercepting-jersey-resource-method-calls/
 *
 * <p>Implements the following:</p>
 *
 * <li>Handles @Deprecated annotations, to return a HTTP warning to the client</li>
 * <li>Handles @Timeout annotations to set/overwrite the context deadline</li>
 * <li>Detects uses of AsyncResponse and sets the context timeout</li>
 *
 * @author Zoltan Farkas
 */
public final class Spf4jInterceptionService implements InterceptionService {

  @Override
  public Filter getDescriptorFilter() {
    return new ResourceFilter();
  }

  @Override
  @Nullable
  public List<MethodInterceptor> getMethodInterceptors(final Method method) {
    boolean hasJaxRs = false;
    Annotation[] annotations = method.getAnnotations();
    for (Annotation ann : annotations) {
      if (ann.annotationType().getName().startsWith("javax.ws.rs")) {
        hasJaxRs = true;
        break;
      }
    }
    if (!hasJaxRs) {
      return null;
    }
    int i  = 0;
    Deprecated dannot = method.getAnnotation(Deprecated.class);
    List<HttpWarning> warnings = null;
    if (dannot != null) {
      warnings = new ArrayList<>(2);
      warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent",
              "Deprecated-Operation: " + method));
    }
    List<MethodInterceptor> extra = null;
    for (Parameter param : method.getParameters()) {
      if (AsyncResponse.class.isAssignableFrom(param.getType())) {
        if (extra == null) {
          extra = new ArrayList<>(2);
        }
        if (param.getAnnotation(Suspended.class) != null) {
          extra.add(new AsycResponseTimeoutSetterInterceptor(i));
        }
        continue;
      }
      Deprecated dp = param.getAnnotation(Deprecated.class);
      if (dp != null) {
        if (warnings == null) {
          warnings = new ArrayList<>(2);
        }
        QueryParam qp = param.getAnnotation(QueryParam.class);
        if (qp != null) {
          warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent",
                  "Deprecated-Query-Param: " + qp.value()));
        } else {
          PathParam pp = param.getAnnotation(PathParam.class);
          if (pp != null) {
            warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent",
                  "Deprecated-Path-Param: " + pp.value()));
          } else {
            warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent",
                     "Deprecated-Param: " + param.getName()));
          }
        }
      }
      i++;
    }
    if (warnings != null) {
      if (extra == null) {
        extra = new ArrayList<>(2);
      }
      extra.add(new WarningsInterceptor(warnings));
    }
    Timeout tannot = method.getAnnotation(Timeout.class);
    if (tannot != null) {
      if (extra == null) {
        extra = new ArrayList<>(2);
      }
      extra.add(0, new ContextTimeoutSetterInterceptor(tannot.unit().toNanos(tannot.value())));
    }
    if (extra == null) {
      return  Collections.singletonList(new LoggingInterceptor(method));
    } else {
      extra.add(0, new LoggingInterceptor(method));
      return extra;
    }
  }

  @Override
  @Nullable
  public List<ConstructorInterceptor> getConstructorInterceptors(final Constructor<?> constructor) {
    return null;
  }

  private static class LoggingInterceptor implements MethodInterceptor {

    private final Logger log;

    LoggingInterceptor(final Method m) {
      this.log = new ExecContextLogger(LoggerFactory.getLogger(m.getDeclaringClass().getName() + "->" + m.getName()));
    }


    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      log.debug("invoking", invocation.getArguments());
      Object result = invocation.proceed();
      log.debug("returning", result);
      return result;
    }

  }

  private static class ContextTimeoutSetterInterceptor implements MethodInterceptor {

    private final long  timeoutNanos;

    ContextTimeoutSetterInterceptor(final long timeoutNanos) {
      this.timeoutNanos = timeoutNanos;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      try (ExecutionContext ec = ExecutionContexts.start(invocation.getMethod().getName(),
              timeoutNanos, TimeUnit.NANOSECONDS)) {
        return invocation.proceed();
      }
    }
  }



  private static class AsycResponseTimeoutSetterInterceptor implements MethodInterceptor {

    private final int loc;

    AsycResponseTimeoutSetterInterceptor(final int loc) {
      this.loc = loc;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      AsyncResponse ar = (AsyncResponse) invocation.getArguments()[loc];
      if (!ar.setTimeout(ExecutionContexts.getTimeToDeadline(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS)) {
        throw new IllegalStateException("Async Response must be suspended " + ar);
      }
      return invocation.proceed();
    }
  }

  private static class WarningsInterceptor implements MethodInterceptor {

    private final List<HttpWarning> warnings;

    WarningsInterceptor(final List<HttpWarning> warnings) {
      this.warnings = warnings;
    }

    @Override
    @SuppressFBWarnings("HTTP_RESPONSE_SPLITTING") // Warning text message is validated for cr/lf
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      ExecutionContext current = ExecutionContexts.current();
      ContextValue<CountingHttpServletResponse> contextAndValue = current.getContextAndValue(ContextTags.HTTP_RESP);
      ExecutionContext ctxt = contextAndValue.getContext();
      CountingHttpServletResponse value = contextAndValue.getValue();
      for (HttpWarning warning: warnings) {
        ctxt.accumulateComponent(ContextTags.HTTP_WARNINGS, warning);
        ctxt.accumulate(ContextTags.LOG_LEVEL, Level.WARN);
        value.addHeader(Headers.WARNING, warning.toString());
      }
      return invocation.proceed();
    }

  }

  private static class ResourceFilter implements Filter {

    @Override
    public boolean matches(final Descriptor d) {
      if (d instanceof SystemDescriptor) {
        return ((SystemDescriptor) d).getImplementationClass().getAnnotation(Path.class) != null;
      }
      return false;
    }
  }

}
