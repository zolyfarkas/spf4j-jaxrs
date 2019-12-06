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
package org.spf4j.jaxrs.aql;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;

/**
 * Sys admin securirty context.
 * @author Zoltan Farkas
 */
final class SaSecurityContext implements SecurityContext {

  static final SecurityContext INSTANCE = new SaSecurityContext();

  private SaSecurityContext() { }

  @Override
  public Principal getUserPrincipal() {
    return () -> "sa";
  }

  @Override
  public boolean isUserInRole(final String role) {
    return "dba".equals(role) || "sa".equals(role);
  }

  @Override
  public boolean isSecure() {
    return true;
  }

  @Override
  public String getAuthenticationScheme() {
    return "INTERNAL";
  }

}
