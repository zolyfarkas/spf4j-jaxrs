/*
 * Copyright 2021 SPF4J.
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
package org.spf4j.jaxrs.client;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.function.Predicate;
import javax.annotation.concurrent.NotThreadSafe;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.avro.SchemaResolver;
import org.spf4j.base.Either;
import org.spf4j.jaxrs.config.ConfigImpl;
import org.spf4j.jaxrs.config.JerseyMicroprofileConfigurationModel;
import org.spf4j.jaxrs.config.MemoryConfig;
import org.spf4j.log.Level;
import org.spf4j.service.avro.HttpExecutionPolicy;
import org.spf4j.test.log.annotations.ExpectLog;

/**
 * tests of this class should not be executed in parallel threads (even with other tests)
 */
@NotThreadSafe
public class EndpointPoliciesConfigTest {


  @BeforeClass
  public static void init() {
    MemoryConfig.init();
  }

  @AfterClass
  public static void dispose() {
    MemoryConfig.resetToDefault();
  }

  @Before
  public  void initTest() {
    MemoryConfig.clear();
  }

  @Test
  public void testNoEndpointConfig() {
    // prepare the test config
    EndpointPoliciesConfig config = new EndpointPoliciesConfig(new JerseyMicroprofileConfigurationModel(
                (ConfigImpl) ConfigProvider.getConfig()), SchemaResolver.NONE);
    // No config at all.
    Assert.assertNull(
    config.getHttpExecutionPolicy("service", -1, "/endpoint", "GET", Collections.EMPTY_MAP, Collections.EMPTY_MAP));
  }

  @Test
  @ExpectLog(level = Level.ERROR,
          messageRegexp = "Referenced Value: http.exec.policy.default does not exist, ignoring it")
  public void testEndpointConfigSomeMissing() throws IOException {
    // prepare the test config
    EndpointPoliciesConfig config = new EndpointPoliciesConfig(new JerseyMicroprofileConfigurationModel(
                (ConfigImpl) ConfigProvider.getConfig()), SchemaResolver.NONE);
    MemoryConfig.put(EndpointPoliciesConfig.CONFIG_NAME, Resources.toString(
            Resources.getResource(EndpointPoliciesConfigTest.class, "endpointPolicies.json"), StandardCharsets.UTF_8));
    // have endpoint policies, but missing all references.
    Assert.assertNull(
    config.getHttpExecutionPolicy("service", -1, "/endpoint", "GET", Collections.EMPTY_MAP, Collections.EMPTY_MAP));
  }

  @Test
  public void testEndpointConfig() throws IOException {
    // prepare the test config
    EndpointPoliciesConfig config = new EndpointPoliciesConfig(new JerseyMicroprofileConfigurationModel(
                (ConfigImpl) ConfigProvider.getConfig()), SchemaResolver.NONE);
    MemoryConfig.put(EndpointPoliciesConfig.CONFIG_NAME,
            Resources.toString(
            Resources.getResource(EndpointPoliciesConfigTest.class, "endpointPolicies.json"),
            StandardCharsets.UTF_8));
    MemoryConfig.put("http.exec.policy.default",
            Resources.toString(
            Resources.getResource(EndpointPoliciesConfigTest.class, "http.exec.policy.default.json"),
            StandardCharsets.UTF_8));
    MemoryConfig.put("http.exec.policy.my-service-local",
            Resources.toString(
            Resources.getResource(EndpointPoliciesConfigTest.class, "http.exec.policy.my-service-local.json"),
            StandardCharsets.UTF_8));
    MemoryConfig.put("http.exec.policy.my-service-read",
            Resources.toString(
            Resources.getResource(EndpointPoliciesConfigTest.class, "http.exec.policy.my-service-read.json"),
            StandardCharsets.UTF_8));
    MemoryConfig.put(EndpointPoliciesConfig.RESULT_MATCHERS,
            Resources.toString(
            Resources.getResource(EndpointPoliciesConfigTest.class, "http.jaxrs.operationResultPatterns.json"),
            StandardCharsets.UTF_8));
    HttpExecutionPolicy httpExecutionPolicy = config.getHttpExecutionPolicy(
                    "service", -1, "/endpoint", "GET", Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    Assert.assertNotNull(httpExecutionPolicy);
    Assert.assertEquals(Duration.ofSeconds(1), httpExecutionPolicy.getConnectTimeout());
    HttpExecutionPolicy localhttpExecutionPolicy = config.getHttpExecutionPolicy(
                    "my-service.prod.svc.cluster.local", -1, "/endpoint", "GET",
            Collections.EMPTY_MAP, Collections.EMPTY_MAP);
    Assert.assertEquals(Duration.ofMillis(200), localhttpExecutionPolicy.getConnectTimeout());
    Either<Predicate<Throwable>, Predicate<Object>> predicate = config.toResultMatcherSupplier().apply("io_errror");
    Assert.assertNotNull(predicate);
    Predicate<Throwable> left = predicate.getLeft();
    Assert.assertTrue(left.test(new IOException("test")));
    Assert.assertFalse(left.test(new Exception("test")));
  }

}
