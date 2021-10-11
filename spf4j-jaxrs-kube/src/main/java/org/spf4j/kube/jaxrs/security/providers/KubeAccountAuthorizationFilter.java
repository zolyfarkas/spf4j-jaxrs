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
package org.spf4j.kube.jaxrs.security.providers;

import com.google.common.base.Ascii;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.apache.avro.reflect.Nullable;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.kube.client.Client;
import org.spf4j.kube.client.TokenReview;

/**
 * @author Zoltan Farkas
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public final class KubeAccountAuthorizationFilter implements ContainerRequestFilter {

  private static final SecurityContext NOT_AUTH = new SecurityContextImpl(null, null, (x) -> Boolean.FALSE);

  private static final String AUTH_METHOD = "Bearer";

  private final Authenticate secResolver;

  private final KubeRoleMap roleMap;

  private final Client kubeClient;

  interface Authenticate {
    SecurityContext authenticate(String authStr, String scheme);
  }


  @Inject
  public KubeAccountAuthorizationFilter(final Client kubeClient,
          @ConfigProperty(name = "jaxrs.service.auth.tokenCacheTimeMillis", defaultValue = "1000")
          final long cacheMillis,
          @ConfigProperty(name = "jaxrs.service.auth.roleCacheTimeMillis", defaultValue = "10000")
          final long roleCacheMillis) {
    this.kubeClient = kubeClient;
    this.roleMap = new KubeRoleMap(kubeClient, roleCacheMillis);
    if (cacheMillis > 0) {
      LoadingCache<String, LoadingCache<String, SecurityContext>> cache
              = CacheBuilder.newBuilder()
                      .build(new CacheLoader<String, LoadingCache<String, SecurityContext>>() {
                        @Override
                        public LoadingCache<String, SecurityContext> load(final String scheme) {
                          return CacheBuilder.newBuilder()
                                  .expireAfterWrite(cacheMillis, TimeUnit.MILLISECONDS)
                                  .build(new CacheLoader<String, SecurityContext>() {
                                    @Override
                                    public SecurityContext load(final String authStr) {
                                      return authenticate(authStr, scheme);
                                    }
                                  });
                        }
                      });
      this.secResolver = (authStr, scheme) -> cache.getUnchecked(scheme).getUnchecked(authStr);
    } else {
      this.secResolver = this::authenticate;
    }
  }

  private SecurityContext authenticate(final String authStr, final String scheme) {
    TokenReview.Status status = kubeClient.tokenReview(authStr.substring(AUTH_METHOD.length() + 1));
    if (!status.isAuthenticated()) {
      return NOT_AUTH;
    }
    TokenReview.User user = status.getUser();
    return new SecurityContextImpl(user, scheme, roleMap.getRoles(user.getUsername())::contains);
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    SecurityContext securityContext = requestContext.getSecurityContext();
    if (securityContext != null) {
      return;
    }
    String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (auth == null) {
      return;
    }
    if (!auth.startsWith(AUTH_METHOD)) {
      return;
    }
    SecurityContext sc = secResolver.authenticate(auth, requestContext.getUriInfo().getRequestUri().getScheme());
    if (sc == NOT_AUTH) {
      return;
    }
    requestContext.setSecurityContext(sc);
  }


  @Override
  public String toString() {
    return "ServiceAccountAuthorizationFilter{" + "kubeClient=" + kubeClient + '}';
  }

  private static class SecurityContextImpl implements SecurityContext {

    private final TokenReview.User user;
    private final String scheme;
    private final Function<String, Boolean> roles;
    private final Principal principal;

    SecurityContextImpl(@Nullable final TokenReview.User user,
            @Nullable final String scheme, @Nonnull final Function<String, Boolean> roles) {
      this.user = user;
      this.scheme = scheme;
      this.roles = roles;
      this.principal = user == null ?  null : user::getUsername;
    }

    @Override
    public Principal getUserPrincipal() {
      return principal;
    }

    @Override
    public boolean isUserInRole(final String role) {
      boolean isGroup = user.getGroups().contains(role);
      if (isGroup) {
        return true;
      }
      return roles.apply(role);
    }

    @Override
    public boolean isSecure() {
      return Ascii.equalsIgnoreCase("https", scheme);
    }

    @Override
    public String getAuthenticationScheme() {
      return scheme;
    }
  }

}
