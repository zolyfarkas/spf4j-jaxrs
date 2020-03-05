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
package org.spf4j.jaxrs.server.features;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ImmediateErrorHandler;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.internal.inject.AbstractBinder;

/**
 * @author Zoltan Farkas
 */
public final class ImmediateFeature implements Feature {

  @Inject
  public ImmediateFeature(final ServiceLocator locator) {
    ServiceLocatorUtilities.enableImmediateScope(locator);
  }

  @Override
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public boolean configure(final FeatureContext context) {
    context.register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(new ImmediateErrorHandlerImpl()).to(ImmediateErrorHandler.class);
      }
    });
    return true;
  }

  public static final class ImmediateErrorHandlerImpl implements ImmediateErrorHandler {

    @Override
    public void postConstructFailed(final ActiveDescriptor<?> immediateService,
            final Throwable exception) {
      Logger.getLogger(ImmediateErrorHandler.class.getName())
              .log(Level.SEVERE, "Error while instantiating immediate service", exception);
    }

    @Override
    public void preDestroyFailed(final ActiveDescriptor<?> immediateService, final Throwable exception) {
      Logger.getLogger(ImmediateErrorHandler.class.getName())
              .log(Level.SEVERE, "Error while destroying immediate service", exception);
    }
  }

}
