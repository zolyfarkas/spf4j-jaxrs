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
package org.spf4j.api_browser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Properties;
import java.util.function.Function;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.spf4j.grizzly.JerseyService;
import org.spf4j.grizzly.JerseyServiceBuilder;
import org.spf4j.grizzly.JvmServices;
import org.spf4j.grizzly.JvmServicesBuilder;
import org.spf4j.grizzly.SingleNodeClusterFeature;
import org.spf4j.jaxrs.JaxRsSecurityContext;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.jaxrs.server.SecurityAuthenticator;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("CE_CLASS_ENVY")
public abstract class ServiceIntegrationBase {

  private static JvmServices jvm;
  private static JerseyService jerseyService;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static String localService;


  @BeforeClass
  public static void setUp() throws IOException, URISyntaxException {
    // start the server
    jvm = new JvmServicesBuilder()
            .withApplicationName("actuatorTest")
            .withLogFolder("./target")
            .build();
    jerseyService = new JerseyServiceBuilder(jvm)
            .withFeature(ApiBrowserFeature.class)
            .withFeature(SingleNodeClusterFeature.class)
            .withPort(9090)
            .withSecurityAuthenticator(new SecurityAuthenticator() {
              @Override
              public JaxRsSecurityContext authenticate(final Function<String, String> headers) {
                return new JaxRsSecurityContext() {
                  @Override
                  public Principal getUserPrincipal() {
                    return () -> "Test";
                  }

                  @Override
                  public boolean isUserInRole(final String role) {
                    return JaxRsSecurityContext.OPERATOR_ROLE.equals(role);
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
                  public boolean canAccess(final Properties resource,
                          final Properties action, final Properties env) {
                    return true;
                  }


                };
              }
            })
            .build();
    jerseyService.start();
    client = jerseyService.getApplication().getRestClient();
    localService = "http://127.0.0.1:9090";
    target = client.target(localService);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    jerseyService.close();
    jvm.close();
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
