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

import com.google.common.annotations.Beta;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.function.Consumer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.spf4j.base.Base64;

/**
 *
 * @author Zoltan Farkas
 */
public final class SSLUtils {

  private SSLUtils() {
  }

  public static X509Certificate generateCertificate(final Path caCertificate)
          throws IOException, CertificateException {
    try (InputStream caInput = new BufferedInputStream(Files.newInputStream(caCertificate))) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return (X509Certificate) cf.generateCertificate(caInput);
    }
  }

  public static RSAPrivateKey loadRSAPrivateKey(final Path keyFileName)
          throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
    StringBuilder buff = new StringBuilder(2048);
    try (BufferedReader reader = Files.newBufferedReader(keyFileName, StandardCharsets.US_ASCII)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.endsWith("PRIVATE KEY-----")) {
          continue;
        }
        buff.append(line);
      }
    }
    byte[] keyBin = Base64.decodeBase64(buff);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(keyBin);
    return (RSAPrivateKey) kf.generatePrivate(ks);
  }

  public static Certificate generateCertificate(final byte[] caCertificate)
          throws IOException, CertificateException {
    try (InputStream caInput = new ByteArrayInputStream(caCertificate)) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      return cf.generateCertificate(caInput);
    }
  }

  /**
   * utility method to create a ssl context where a non ca certificate is added.
   */
  @Beta
  public static SSLContext buildTrustManagerSslContext(final Consumer<KeyStore> keyStoreSetup) {
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

  /**
   * create a SslContent with the keystore specified.
   */
  @Beta
  public static SSLContext buildKeyManagerSslContext(final Consumer<KeyStore> keyStoreSetup)
          throws UnrecoverableKeyException {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStoreSetup.accept(keyStore);
      KeyManagerFactory kf = KeyManagerFactory.getInstance("SunX509");
      kf.init(keyStore, new char[] {}); // no password
      SSLContext context = SSLContext.getInstance("TLSv1.2");
      context.init(kf.getKeyManagers(), null, null);
      return context;
    } catch (KeyStoreException | KeyManagementException | IOException
            | NoSuchAlgorithmException | CertificateException ex) {
      throw new RuntimeException(ex);
    }
  }

}
