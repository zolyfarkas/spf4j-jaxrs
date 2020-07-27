/*
 * Copyright 2019 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.jaxrs.server.security.providers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.spf4j.jaxrs.server.security.SecuredInternaly;

/**
 * A filter that processes security annotations.
 *
 * @author Zoltan Farkas
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public final class AnnotationAuthorizationFilter implements ContainerRequestFilter {

  private static final Response ACCESS_UNAUTHENTICATED = Response.status(Response.Status.UNAUTHORIZED).build();

  private static final Response ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();

  private final Supplier<Consumer<ContainerRequestContext>> validator;

  @Inject
  public AnnotationAuthorizationFilter(@Context final javax.inject.Provider<ResourceInfo> resourceInfoSupp) {
    validator = () -> {
      ResourceInfo resourceInfo = resourceInfoSupp.get();
      Method method = resourceInfo.getResourceMethod();
      if (isAnnotationPresent(method, DenyAll.class)) {
        return (rCtx) -> rCtx.abortWith(ACCESS_FORBIDDEN);
      } else if (isAnnotationPresent(method, PermitAll.class, SecuredInternaly.class)) {
        return (rCtx) -> {
        };
      } else {
        RolesAllowed annotation = getAnnotation(method, RolesAllowed.class);
        if (annotation != null) {
          String[] roles = annotation.value();
          return (rCtx) -> {
            SecurityContext securityContext = rCtx.getSecurityContext();
            if (securityContext == null) {
              rCtx.abortWith(ACCESS_UNAUTHENTICATED);
            }
            boolean hasRole = false;
            for (String role : roles) {
              if (securityContext.isUserInRole(role)) {
                hasRole = true;
                break;
              }
            }
            if (!hasRole) {
              rCtx.abortWith(ACCESS_FORBIDDEN);
            }
          };
        } else {
            return (rCtx) -> rCtx.abortWith(ACCESS_FORBIDDEN);
        }
      }
    };
  }

  private static boolean isAnnotationPresent(final Method m,
          final Class<? extends Annotation> annotClasz) {
    return m.isAnnotationPresent(annotClasz) || m.getDeclaringClass().isAnnotationPresent(annotClasz);
  }

  private static boolean isAnnotationPresent(final Method m,
          final Class<? extends Annotation>... annotClasses) {
    for (Class<? extends Annotation> annotClasz : annotClasses) {
      boolean isa =  m.isAnnotationPresent(annotClasz) || m.getDeclaringClass().isAnnotationPresent(annotClasz);
      if (isa) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static <T extends Annotation> T getAnnotation(final Method m,
          final Class<T> annotClasz) {
    T annotation = m.getAnnotation(annotClasz);
    if (annotation != null) {
      return annotation;
    }
    return m.getDeclaringClass().getAnnotation(annotClasz);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    validator.get().accept(requestContext);
  }

  @Override
  public String toString() {
    return "AnnotationAuthorizationFilter{" + "validator=" + validator + '}';
  }

}
