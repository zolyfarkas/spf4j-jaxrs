/*
 * Copyright 2020 SPF4J.
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

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.http.ContextTags;

/**
 * This filter sets the security context.
 * You must have the security context attached to the execution context. (ContextTags.SECURITY_CONTEXT)
 * This is done automatically by the ExecutionContextFilter.
 * @author Zoltan Farkas
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public final class SecurityContextRequestFilter implements ContainerRequestFilter {

  @Override
  public void filter(final ContainerRequestContext rctx) {
    rctx.setSecurityContext(ExecutionContexts.current().get(ContextTags.SECURITY_CONTEXT));
  }

}
