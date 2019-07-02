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
package org.spf4j.jaxrs.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public final class SSLUtils {

  private SSLUtils() { }

  public static Certificate generateCertificate(final Path caCertificate)
          throws IOException, CertificateException {
    try (InputStream caInput = new BufferedInputStream(Files.newInputStream(caCertificate))) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertificate(caInput);
    }
  }

  
  public static Certificate generateCertificate(final byte[] caCertificate)
          throws IOException, CertificateException {
    try (InputStream caInput = new ByteArrayInputStream(caCertificate)) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertificate(caInput);
    }
  }

  public static SSLContext buildSslContext(final Consumer<KeyStore>  keyStoreSetup) {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStoreSetup.accept(keyStore);
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

}
