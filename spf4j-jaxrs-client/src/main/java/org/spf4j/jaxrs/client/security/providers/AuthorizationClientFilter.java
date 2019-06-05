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
package org.spf4j.jaxrs.client.security.providers;

import java.util.function.Consumer;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

/**
 * A client filter that will set the authorization header with a Bearer token.
 * @author Zoltan Farkas
 */
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public class AuthorizationClientFilter implements ClientRequestFilter {

  private final Consumer<StringBuilder> tokenWriter;

  private final String method;

  public AuthorizationClientFilter(final AuthorizationMethod method, final Consumer<StringBuilder> tokenWriter) {
   this(method.toString(), tokenWriter);
  }

  public AuthorizationClientFilter(final String method, final Consumer<StringBuilder> tokenWriter) {
    this.tokenWriter = tokenWriter;
    this.method = method;
  }

  @Override
  public final void filter(final ClientRequestContext requestContext) {
    StringBuilder auth = new StringBuilder(128);
    auth.append(method).append(' ');
    tokenWriter.accept(auth);
    requestContext.getHeaders().addFirst(HttpHeaders.AUTHORIZATION, auth);
  }

  @Override
  public final String toString() {
    return "AuthorizationClientFilter{" + "tokenWriter=" + tokenWriter + ", method=" + method + '}';
  }

}
