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
package org.spf4j.kube.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Arrays;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;
import org.spf4j.jaxrs.common.providers.avro.XJsonAvroMessageBodyReader;
import org.spf4j.kube.cluster.KubeCluster;
import javax.ws.rs.core.MultivaluedHashMap;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ExtendedReflectData;
import org.junit.Ignore;

/**
 *
 * @author Zoltan Farkas
 */
@Ignore
public class ClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(ClientTest.class);

  @Test
  public void testKubeClient() {
    Client kubeCl = new Client("127.0.0.1:32768", null, null);
    Endpoints endpoints = kubeCl.getEndpoints("default", "jaxrs-spf4j-demo");
    LOG.debug("Endpoints: {} ", endpoints);
    Assert.assertNotNull(endpoints);
    KubeCluster cluster = new KubeCluster(kubeCl, "default", "jaxrs-spf4j-demo");
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    LOG.debug("Endpoints: {} ", clusterInfo);
    Assert.assertNotNull(clusterInfo);
  }

  private static final String RESP = "{\"kind\":\"Endpoints\",\"apiVersion\":\"v1\","
          + "\"metadata\":{\"name\":\"jaxrs-spf4j-demo\",\"namespace\":\"default\","
          + "\"selfLink\":\"/api/v1/namespaces/default/endpoints/jaxrs-spf4j-demo\","
          + "\"uid\":\"65c1183f-4408-11e9-8cbf-9e4ed3d0f183\",\"resourceVersion\":\"852976\","
          + "\"creationTimestamp\":\"2019-03-11T14:17:26Z\",\"labels\":"
          + "{\"app\":\"jaxrs-spf4j-demo\",\"version\":\"0.5-SNAPSHOT\"}},"
          + "\"subsets\":[{\"addresses\":[{\"ip\":\"10.244.2.67\","
          + "\"nodeName\":\"kube-node-1\",\"targetRef\":{\"kind\":\"Pod\",\"namespace\":\"default\","
          + "\"name\":\"jaxrs-spf4j-demo-65c5c6874b-px6lc\",\"uid\":\"f5c4db82-4e35-11e9-8cbf-9e4ed3d0f183\","
          + "\"resourceVersion\":\"852975\"}}],"
          + "\"notReadyAddresses\":[{\"ip\":\"10.244.2.66\","
          + "\"nodeName\":\"kube-node-1\",\"targetRef\":{\"kind\":\"Pod\","
          + "\"namespace\":\"default\",\"name\":\"jaxrs-spf4j-demo-65c5c6874b-55qdh\","
          + "\"uid\":\"f5c5730f-4e35-11e9-8cbf-9e4ed3d0f183\",\"resourceVersion\":\"852962\"}}],"
          + "\"ports\":[{\"name\":\"http\",\"port\":8080,\"protocol\":\"TCP\"}]}]}\n";

  @Test
  public void testParse() throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(RESP.getBytes(StandardCharsets.UTF_8));
    XJsonAvroMessageBodyReader reader = new XJsonAvroMessageBodyReader(SchemaProtocol.NONE);
    Endpoints eps = (Endpoints) reader.readFrom((Class) Endpoints.class,
            Endpoints.class, Arrays.EMPTY_ANNOT_ARRAY,
            null, new MultivaluedHashMap(), bis);
    Assert.assertNotNull(eps);
  }

  private static final String RESP2 = "{\"kind\":\"Endpoints\",\"apiVersion\":\"v1\","
          + "\"metadata\":{\"name\":\"jaxrs-spf4j-demo\",\"namespace\":\"default\","
          + "\"selfLink\":\"/api/v1/namespaces/default/endpoints/jaxrs-spf4j-demo\","
          + "\"uid\":\"65c1183f-4408-11e9-8cbf-9e4ed3d0f183\",\"resourceVersion\":\"852976\","
          + "\"creationTimestamp\":\"2019-03-11T14:17:26Z\",\"labels\":"
          + "{\"app\":\"jaxrs-spf4j-demo\",\"version\":\"0.5-SNAPSHOT\"}},"
          + "\"subsets\":[{\"addresses\":[{\"ip\":\"10.244.2.67\","
          + "\"nodeName\":\"kube-node-1\",\"targetRef\":{\"kind\":\"Pod\",\"namespace\":\"default\","
          + "\"name\":\"jaxrs-spf4j-demo-65c5c6874b-px6lc\",\"uid\":\"f5c4db82-4e35-11e9-8cbf-9e4ed3d0f183\","
          + "\"resourceVersion\":\"852975\"}}],"
          + "\"ports\":[{\"name\":\"http\",\"port\":8080,\"protocol\":\"TCP\"}]}]}\n";

  @Test
  public void testParse2() throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(RESP2.getBytes(StandardCharsets.UTF_8));
    XJsonAvroMessageBodyReader reader = new XJsonAvroMessageBodyReader(SchemaProtocol.NONE);
    Endpoints eps = (Endpoints) reader.readFrom((Class) Endpoints.class,
            Endpoints.class, Arrays.EMPTY_ANNOT_ARRAY,
            null, new MultivaluedHashMap(), bis);
    Assert.assertNotNull(eps);
  }

  private static final String RESP3 = "{\n"
          + "\"kind\": \"Endpoints\",\n"
          + "\"apiVersion\": \"v1\",\n"
          + "\"metadata\": {\n"
          + "\"name\": \"jaxrs-spf4j-demo\",\n"
          + "\"namespace\": \"default\",\n"
          + "\"selfLink\": \"/api/v1/namespaces/default/endpoints/jaxrs-spf4j-demo\",\n"
          + "\"uid\": \"fb193e2e-5ea7-11e9-9d14-1269dff1685e\",\n"
          + "\"resourceVersion\": \"3793\",\n"
          + "\"creationTimestamp\": \"2019-04-14T11:25:17Z\",\n"
          + "\"labels\": {\n"
          + "\"app\": \"jaxrs-spf4j-demo\",\n"
          + "\"version\": \"0.6-SNAPSHOT\"\n"
          + "}\n"
          + "}\n"
          + "}";

  @Test
  public void testParse3() throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(RESP3.getBytes(StandardCharsets.UTF_8));
    XJsonAvroMessageBodyReader reader = new XJsonAvroMessageBodyReader(SchemaProtocol.NONE);
    Endpoints eps = (Endpoints) reader.readFrom((Class) Endpoints.class,
            Endpoints.class, Arrays.EMPTY_ANNOT_ARRAY,
            null, new MultivaluedHashMap(), bis);
    Assert.assertNotNull(eps);
  }


  @Test
  public void testTokenReview() {
    Client kubeCl = new Client("127.0.0.1:32768", null, null);
    TokenReview.Status tokenReview = kubeCl.tokenReview(System.getProperty("kube.token"));
    Assert.assertTrue(tokenReview.isAuthenticated());
  }

  @Test
  public void testTokenReviewFail() {
    Client kubeCl = new Client("127.0.0.1:32768", null, null);
    TokenReview.Status tokenReview = kubeCl.tokenReview("abc");
    Assert.assertFalse(tokenReview.isAuthenticated());
  }


  @Test
  public void testRoleBindings() {
    Schema schema = ExtendedReflectData.get().getSchema(RoleBindings.class);
    Assert.assertNotNull(schema);
    LOG.debug("schema", schema);
    Client kubeCl = new Client("127.0.0.1:32768", null, null);
    RoleBindings rb = kubeCl.getRoleBindings();
    Assert.assertEquals("RoleBindingList", rb.getKind());
    RoleBindings rb2 = kubeCl.getClusterRoleBindings();
    Assert.assertEquals("ClusterRoleBindingList", rb2.getKind());
  }

}
