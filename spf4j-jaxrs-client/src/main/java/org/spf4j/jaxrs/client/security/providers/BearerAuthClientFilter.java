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
import javax.ws.rs.ext.Provider;

/**
 * A client filter that will set the authorization header with a Bearer token.
 * @author Zoltan Farkas
 */
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public final class BearerAuthClientFilter extends  AuthorizationClientFilter {


  public BearerAuthClientFilter(final Consumer<StringBuilder> tokenWriter) {
    super(AuthorizationMethod.Bearer, tokenWriter);
  }

}
