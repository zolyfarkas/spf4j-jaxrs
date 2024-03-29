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

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientProperties;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.jaxrs.client.SSLUtils;
import org.spf4j.jaxrs.client.Spf4jClientBuilder;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.jaxrs.client.security.providers.BearerAuthClientFilter;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;
import org.spf4j.jaxrs.common.providers.avro.XJsonAvroMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.XJsonAvroMessageBodyWriter;

/**
 * A mini kubernetes client that implements "discovery" and is meant to be used within a kubernetes pod.
 *
 * example invocation: curl --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt -H "Authorization: Bearer
 * $(cat /var/run/secrets/kubernetes.io/serviceaccount/token)"
 * https://kubernetes.default.svc/api/v1/namespaces/default/endpoints/jaxrs-spf4j-demo
 *
 * @see https://kubernetes.io/docs/tasks/access-application-cluster/access-cluster/#accessing-the-api-from-a-pod
 *
 * @author Zoltan Farkas
 */
public final class Client {

  private final WebTarget apiTarget;

  private final WebTarget tokenReviewTarget;

  private final WebTarget clusterRoleBindingsTarget;

  private final WebTarget roleBindingsTarget;

  public Client(@Nullable final TokenProvider apiToken,
          @Nullable final byte[] caCertificate) {
    this("kubernetes.default.svc", apiToken, caCertificate);
  }

  public Client(final String kubernetesMaster,
          @Nullable final TokenProvider apiToken,
          @Nullable final byte[] caCertificate) {
    Spf4jClientBuilder clBuilder = new Spf4jClientBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS);
    if (caCertificate != null) {
      clBuilder = clBuilder.sslContext(buildSslContext(caCertificate));
    }
    if (apiToken != null) {
      clBuilder = clBuilder.register(
              new BearerAuthClientFilter((hv) -> apiToken.access((t, o, l) -> hv.append(t, o, l))));
    }
    Spf4jWebTarget rootTarget = clBuilder
            .register(new ExecutionContextClientFilter(DeadlineProtocol.NONE, true))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(new XJsonAvroMessageBodyReader(SchemaProtocol.NONE))
            .register(new XJsonAvroMessageBodyWriter(SchemaProtocol.NONE))
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build().target((caCertificate == null ? "http://" : "https://")
                    + kubernetesMaster);
    apiTarget = rootTarget.path("api/v1");
    tokenReviewTarget = rootTarget.path("apis/authentication.k8s.io/v1/tokenreviews");
    clusterRoleBindingsTarget = rootTarget.path("apis/rbac.authorization.k8s.io/v1/clusterrolebindings");
    roleBindingsTarget = rootTarget.path("apis/rbac.authorization.k8s.io/v1/rolebindings");
  }

  public TokenReview.Status tokenReview(final String token) {
    return tokenReviewTarget.request(MediaType.APPLICATION_JSON_TYPE).post(
            Entity.entity(new TokenReview(token), MediaType.APPLICATION_JSON), TokenReview.class).getStatus();
  }

  public RoleBindings getClusterRoleBindings() {
    return clusterRoleBindingsTarget.request(MediaType.APPLICATION_JSON).get(RoleBindings.class);
  }

  public RoleBindings getRoleBindings() {
    return roleBindingsTarget.request(MediaType.APPLICATION_JSON).get(RoleBindings.class);
  }

  public Endpoints getEndpoints(final String namesSpace, final String endpointName) {
    return apiTarget.path("namespaces/{namespace}/endpoints/{endpointName}")
            .resolveTemplate("namespace", namesSpace)
            .resolveTemplate("endpointName", endpointName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(Endpoints.class);
  }

  private static SSLContext buildSslContext(final byte[] caCertificate) {
    return SSLUtils.buildTrustManagerSslContext((keyStore) -> {
      try {
        keyStore.setCertificateEntry("ca", SSLUtils.generateCertificate(caCertificate));
      } catch (IOException | CertificateException | KeyStoreException ex) {
        throw new RuntimeException(ex);
      }
    });
  }

  @Override
  public String toString() {
    return "Client{" + "apiTarget=" + apiTarget + '}';
  }

}
