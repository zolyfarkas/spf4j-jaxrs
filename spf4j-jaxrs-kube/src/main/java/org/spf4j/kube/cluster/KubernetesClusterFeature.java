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
package org.spf4j.kube.cluster;

import com.google.common.base.Suppliers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.spf4j.base.Env;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.Service;
import org.spf4j.kube.client.Client;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class KubernetesClusterFeature implements Feature {

  private static final String CA_CRT_FILE
          = Env.getValue("CA_CRT_FILE", "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");

  private static final String SVC_TOKEN_FILE = Env.getValue("SVC_TOKEN_FILE",
          "/var/run/secrets/kubernetes.io/serviceaccount/token");

  private final String kubeNameSpace;

  private final String kubeAppName;

  @Inject
  public KubernetesClusterFeature(@ConfigProperty(name = "KUBE_NAME_SPACE") final String kubeNameSpace,
          @ConfigProperty(name = "KUBE_APP_NAME") final String kubeAppName) {
    this.kubeNameSpace = kubeNameSpace;
    this.kubeAppName = kubeAppName;
  }

  @Override
  public boolean configure(final FeatureContext fc) {
    Path certPath = Paths.get(CA_CRT_FILE);
    byte[] caCert;
    try {
      if (Files.isReadable(certPath)) {
        caCert = Files.readAllBytes(certPath);
      } else {
        caCert = null;
      }
      KubeCluster kubeCluster = new KubeCluster(new Client(Suppliers.memoizeWithExpiration(
              () -> {
                try {
                  return new String(Files.readAllBytes(Paths.get(SVC_TOKEN_FILE)),
                          StandardCharsets.UTF_8);
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
              }, 10, TimeUnit.MINUTES), caCert), kubeNameSpace, kubeAppName);
      fc.register(new ClusterBinder(kubeCluster));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return true;
  }

  @Override
  public String toString() {
    return "KubernetesClusterFeature{" + "kubeNameSpace=" + kubeNameSpace + ", kubeAppName=" + kubeAppName + '}';
  }

  private static class ClusterBinder extends AbstractBinder {

    private final KubeCluster kubeCluster;

    ClusterBinder(final KubeCluster kubeCluster) {
      this.kubeCluster = kubeCluster;
    }

    @Override
    protected void configure() {
      bind(kubeCluster).to(Cluster.class);
      bind(kubeCluster).to(Service.class);
    }
  }

}
