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
package org.spf4j.hk2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import org.apache.avro.SchemaResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.spf4j.avro.SchemaClient;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.server.DebugDetailEntitlement;
import org.spf4j.jaxrs.server.Spf4jInterceptionService;

public final class Spf4jBinder extends AbstractBinder {

  private final SchemaClient schemaClient;

  private final Spf4JClient restClient;

  private final DebugDetailEntitlement ddEnt;

  public Spf4jBinder(final SchemaClient schemaClient,
          final Spf4JClient restClient, final DebugDetailEntitlement ddEnt) {
    this.schemaClient = schemaClient;
    this.restClient = restClient;
    this.ddEnt = ddEnt;
  }

  @Override
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  protected void configure() {
    bind(schemaClient).to(SchemaResolver.class);
    bind(restClient).to(Client.class);
    bind(ddEnt).to(DebugDetailEntitlement.class);
    bind(Spf4jInterceptionService.class)
            .to(org.glassfish.hk2.api.InterceptionService.class)
            .in(Singleton.class);
    bind(ConfigurationInjector.class)
            .to(new GenericType<InjectionResolver<ConfigProperty>>() { })
            .in(Singleton.class);
  }

  @Override
  public String toString() {
    return "Spf4jBinder{" + "schemaClient=" + schemaClient + ", restClient=" + restClient + '}';
  }

}
