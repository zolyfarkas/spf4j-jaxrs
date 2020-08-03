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
package org.spf4j.jaxrs.server.providers;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.spf4j.base.ExecutionContext;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.server.security.providers.AnnotationAuthorizationFilter;
import org.spf4j.jaxrs.server.security.providers.SecurityContextRequestFilter;

/**
 * @author Zoltan Farkas
 */
public final class DefaultServerProvidersFeatures implements Feature {

  @Override
  public boolean configure(final FeatureContext fc) {
    fc.register(DataDeprecationsJaxRsFilter.class);
    fc.register(StdErrorResponseExceptionMapper.class);
    fc.register(ProjectionJaxRsFilter.class);
    fc.register(ServerCustomExecutorServiceProvider.class);
    fc.register(ServerCustomScheduledExecutionServiceProvider.class);
    fc.register(SecurityContextRequestFilter.class);
    fc.register(AnnotationAuthorizationFilter.class);
    fc.register(new XtraObjectsContextBinder());
    return true;
  }

  private static class XtraObjectsContextBinder extends AbstractBinder {

    @Override
    protected void configure() {
      bindFactory(ExecutionContextResolver.class).to(ExecutionContext.class)
              .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
      bindFactory(JaxRsAbacSecurityContextResolver.class).to(JaxRsSecurityContext.class)
              .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
    }
  }

}
