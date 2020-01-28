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
package org.spf4j.grizzly;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.ResourceConfig;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 * @author Zoltan Farkas
 */
public class Spf4jJaxrsApplication extends ResourceConfig {


  private final ServiceLocator locator;

  @Inject
  public Spf4jJaxrsApplication(@Context final ServletContext srvContext,
          final ServiceLocator locator) {
    ServiceLocatorUtilities.enableImmediateScope(locator);
    this.locator = locator;
  }

  public final Spf4JClient getRestClient() {
    return locator.getService(Spf4JClient.class);
  }

  /**
   * overwrite as needed.
   */
  @Override
  public String toString() {
    return "Spf4jJaxrsApplication{" + "locator=" + locator + '}';
  }

}
