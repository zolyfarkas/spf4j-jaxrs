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
package org.spf4j.actuator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.spf4j.grizzly.JerseyService;
import org.spf4j.grizzly.JerseyServiceBuilder;
import org.spf4j.grizzly.JvmServices;
import org.spf4j.grizzly.JvmServicesBuilder;
import org.spf4j.grizzly.SingleNodeClusterFeature;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("CE_CLASS_ENVY")
public abstract class ServiceIntegrationBase {

  private static final JvmServices JVM = new JvmServicesBuilder()
            .withApplicationName("actuatorTest")
            .withLogFolder("./target")
            .build().closeOnShutdown();
  private static JerseyService jerseyService;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static String localService;


  @BeforeClass
  public static void setUp() throws IOException, URISyntaxException {
    // start the server
    jerseyService = new JerseyServiceBuilder(JVM)
            .withFeature(ActuatorFeature.class)
            .withFeature(SingleNodeClusterFeature.class)
            .withPort(9090)
            .build();
    jerseyService.start();
    client = jerseyService.getApplication().getRestClient();
    localService = "http://127.0.0.1:9090";
    target = client.target(localService);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    jerseyService.close();
  }

  public static Spf4jWebTarget getTarget() {
    return target;
  }

  public static Spf4JClient getClient() {
    return client;
  }

  public static String getLocalService() {
    return localService;
  }

}
