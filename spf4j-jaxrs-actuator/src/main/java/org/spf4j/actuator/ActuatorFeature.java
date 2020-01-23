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
package org.spf4j.actuator;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.spf4j.actuator.health.HealthResource;
import org.spf4j.actuator.health.checks.DefaultHealthChecksBinder;
import org.spf4j.actuator.info.InfoResource;
import org.spf4j.actuator.jmx.JmxResource;
import org.spf4j.actuator.logs.LogFilesResource;
import org.spf4j.actuator.logs.LogsResource;
import org.spf4j.actuator.metrics.MetricsResource;
import org.spf4j.actuator.openApi.OpenApiResource;
import org.spf4j.actuator.profiles.ProfilesResource;

/**
 *
 * @author Zoltan Farkas
 */
public final  class ActuatorFeature implements Feature {

  @Override
  public boolean configure(final FeatureContext fc) {
    fc.register(InfoResource.class);
    fc.register(HealthResource.class);
    fc.register(JmxResource.class);
    fc.register(LogsResource.class);
    fc.register(LogFilesResource.class);
    fc.register(MetricsResource.class);
    fc.register(OpenApiResource.class);
    fc.register(ProfilesResource.class);
    fc.register(new DefaultHealthChecksBinder());
    return true;
  }

}
