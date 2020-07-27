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
package org.spf4j.jaxrs.server;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.http.ContextTags;
import org.spf4j.jaxrs.JaxRsSecurityContext;


@Priority(Priorities.HEADER_DECORATOR)
@Provider
public final class DelegatingAuthenticationClientFilter implements ClientRequestFilter  {

  @Override
  public void filter(final ClientRequestContext reqCtx) {
    ExecutionContext current = ExecutionContexts.current();
    if (current != null) {
      JaxRsSecurityContext secCtx = current.get(ContextTags.SECURITY_CONTEXT);
      if (secCtx != null) {
        secCtx.initiateAuthentication(reqCtx.getHeaders()::add);
      }
    }
  }


}
