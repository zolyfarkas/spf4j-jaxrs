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
package org.spf4j.kube;

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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientProperties;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.avro.SchemaProtocol;
import org.spf4j.jaxrs.common.avro.XJsonAvroMessageBodyReader;

/**
 * @author Zoltan Farkas
 */
public class Client {

  private final String kubernetesMaster;
  private final String apiToken;

  private final WebTarget apiTarget;

  public Client(String kubernetesMaster, String apiToken, String caCertificate) {
    this.kubernetesMaster = kubernetesMaster;
    this.apiToken = apiToken;
    apiTarget = new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslContext(buildSslContext(caCertificate))
            .register(new ExecutionContextClientFilter(DeadlineProtocol.NONE))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(new XJsonAvroMessageBodyReader(SchemaProtocol.NONE))
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build()).target("api/v1");
  }

  private Certificate generateCertificate(String caCertificate)
          throws IOException, CertificateException {
    try (InputStream caInput = new ByteArrayInputStream(caCertificate.getBytes(StandardCharsets.UTF_8))) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertificate(caInput);
    }
  }

  private SSLContext buildSslContext(String caCertificate) {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", generateCertificate(caCertificate));

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keyStore);

      SSLContext context = SSLContext.getInstance("TLSv1.2");
      context.init(null, tmf.getTrustManagers(), null);
      return context;
    } catch (KeyStoreException | KeyManagementException |IOException
            | NoSuchAlgorithmException | CertificateException ex) {
      throw new RuntimeException(ex);
    }
  }

}
