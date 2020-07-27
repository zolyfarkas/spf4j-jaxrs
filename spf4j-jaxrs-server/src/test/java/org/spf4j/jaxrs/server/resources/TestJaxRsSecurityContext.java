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
package org.spf4j.jaxrs.server.resources;

import java.security.Principal;
import java.util.Properties;
import javax.annotation.Nullable;
import org.spf4j.jaxrs.JaxRsSecurityContext;

/**
 *
 * @author Zoltan Farkas
 */
public final class TestJaxRsSecurityContext implements JaxRsSecurityContext {

  @Override
  @Nullable
  public Principal getUserPrincipal() {
    return null;
  }

  @Override
  public boolean isUserInRole(final String arg0) {
    return true;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public String getAuthenticationScheme() {
    return "TEST";
  }

  @Override
  public boolean canAccess(final Properties resource, final Properties action, final Properties env) {
    return true;
  }



}
