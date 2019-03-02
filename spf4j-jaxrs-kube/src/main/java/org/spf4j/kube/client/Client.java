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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientProperties;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.providers.BearerAuthClientFilter;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.avro.SchemaProtocol;
import org.spf4j.jaxrs.common.avro.XJsonAvroMessageBodyReader;

/**
 * @author Zoltan Farkas
 */
public final class Client {

  private final WebTarget apiTarget;

  public Client(final String kubernetesMaster,
          @Nullable final String apiToken,
          @Nullable final byte[] caCertificate) {
    ClientBuilder clBuilder = ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS);
    if (caCertificate != null) {
      clBuilder = clBuilder.sslContext(buildSslContext(caCertificate));
    }
    if (apiToken != null) {
      clBuilder = clBuilder.register(new BearerAuthClientFilter((hv) -> hv.append(apiToken)));
    }
    apiTarget = new Spf4JClient(clBuilder
            .register(new ExecutionContextClientFilter(DeadlineProtocol.NONE))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
//            .register(new JsonAvroMessageBodyReader2(SchemaProtocol.NONE))
            .register(new XJsonAvroMessageBodyReader(SchemaProtocol.NONE))
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build()).target(kubernetesMaster).path("api/v1");
  }


  public Endpoints getEndpoints(final String namesSpace, final String endpointName) {
    return apiTarget.path("namespaces/{namespace}/endpoints/{endpointName}")
            .resolveTemplate("namespace", namesSpace)
            .resolveTemplate("endpointName", endpointName)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(Endpoints.class);
  }

  private Certificate generateCertificate(final byte[] caCertificate)
          throws IOException, CertificateException {
    try (InputStream caInput = new ByteArrayInputStream(caCertificate)) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertificate(caInput);
    }
  }

  private SSLContext buildSslContext(final byte[] caCertificate) {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", generateCertificate(caCertificate));

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keyStore);

      SSLContext context = SSLContext.getInstance("TLSv1.2");
      context.init(null, tmf.getTrustManagers(), null);
      return context;
    } catch (KeyStoreException | KeyManagementException | IOException
            | NoSuchAlgorithmException | CertificateException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public String toString() {
    return "Client{" + "apiTarget=" + apiTarget + '}';
  }

}
