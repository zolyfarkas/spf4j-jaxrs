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
package org.spf4j.actuator.cluster;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.spf4j.actuator.ActuatorFeature;
import org.spf4j.actuator.cluster.health.ClusterAllNodesCheck;
import org.spf4j.actuator.cluster.health.ClusterAllNodesRegistration;
import org.spf4j.actuator.cluster.info.InfoResourceCluster;
import org.spf4j.actuator.cluster.jmx.JmxClusterResource;
import org.spf4j.actuator.cluster.logs.LogFilesClusterResource;
import org.spf4j.actuator.cluster.logs.LogbackClusterResource;
import org.spf4j.actuator.cluster.logs.LogsClusterResource;
import org.spf4j.actuator.cluster.metrics.MetricsClusterResource;
import org.spf4j.actuator.cluster.profiles.ProfilesClusterResource;
import org.spf4j.actuator.health.HealthCheck;

/**
 *
 * @author Zoltan Farkas
 */
public final class ClusterActuatorFeature implements Feature {

  @Override
  public boolean configure(final FeatureContext fc) {
    fc.register(ActuatorFeature.class);
    fc.register(InfoResourceCluster.class);
    fc.register(JmxClusterResource.class);
    fc.register(LogFilesClusterResource.class);
    fc.register(LogsClusterResource.class);
    fc.register(LogbackClusterResource.class);
    fc.register(MetricsClusterResource.class);
    fc.register(ProfilesClusterResource.class);
    fc.register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(ClusterAllNodesCheck.class).to(HealthCheck.class).to(ClusterAllNodesCheck.class);
        bind(ClusterAllNodesRegistration.class).to(HealthCheck.Registration.class);
      }
    });
    return true;
  }

}
