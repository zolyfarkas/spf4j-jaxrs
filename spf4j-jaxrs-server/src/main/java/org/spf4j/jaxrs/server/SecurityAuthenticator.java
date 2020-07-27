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

import org.spf4j.jaxrs.JaxRsSecurityContext;
import java.security.Principal;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * A security authenticator/authorizator.
 * @author Zoltan Farkas
 */
public interface SecurityAuthenticator {

  SecurityAuthenticator NOAUTH = new SecurityAuthenticator() {

    private final JaxRsSecurityContext unauth = new JaxRsSecurityContext() {
        @Override
        public Principal getUserPrincipal() {
          return null;
        }

        @Override
        public boolean isUserInRole(final String role) {
          return false;
        }

        @Override
        public boolean isSecure() {
          return false;
        }

        @Override
        public String getAuthenticationScheme() {
          return "NONE";
        }
      };

    @Override
    public JaxRsSecurityContext authenticate(final Function<String, String> headers) {
      return unauth;
    }
  };

  /**
   * authenticate the user hased on the headers.
   * @param headers the HTTP headers
   * @return the security context.
   */
  @Nonnull
  JaxRsSecurityContext authenticate(Function<String, String> headers);


}
