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

import java.lang.reflect.Method;
import java.util.function.Consumer;
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

/**
 *
 * @author Zoltan Farkas
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public final class AnnotationAuthorizationFilter implements ContainerRequestFilter {

  private static final Response ACCESS_UNAUTHENTICATED = Response.status(Response.Status.UNAUTHORIZED).build();

  private static final Response ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();

  private final Consumer<ContainerRequestContext> validator;

  @Inject
  public AnnotationAuthorizationFilter(@Context final ResourceInfo resourceInfo) {
    Method method = resourceInfo.getResourceMethod();
    if (method.isAnnotationPresent(DenyAll.class)) {
      validator = (rCtx) -> rCtx.abortWith(ACCESS_FORBIDDEN);
    } else if (method.isAnnotationPresent(PermitAll.class)) {
      validator = (rCtx) -> { };
    } else {
      RolesAllowed annotation = method.getAnnotation(RolesAllowed.class);
      if (annotation != null) {
        String[] roles = annotation.value();
        validator = (rCtx) -> {
          SecurityContext securityContext = rCtx.getSecurityContext();
          if (securityContext == null) {
            rCtx.abortWith(ACCESS_UNAUTHENTICATED);
          }
          boolean hasRole = false;
          for (String role : roles) {
            if (!securityContext.isUserInRole(role)) {
              hasRole = true;
              break;
            }
          }
          if (!hasRole) {
            rCtx.abortWith(ACCESS_FORBIDDEN);
          }
        };
      } else {
        if (resourceInfo.getResourceClass().isAnnotationPresent(PermitAll.class)) {
          validator = (rCtx) -> { };
        } else {
          validator = (rCtx) -> rCtx.abortWith(ACCESS_FORBIDDEN);
        }
      }
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
     validator.accept(requestContext);
  }

  @Override
  public String toString() {
    return "AnnotationAuthorizationFilter{" + "validator=" + validator + '}';
  }

}
