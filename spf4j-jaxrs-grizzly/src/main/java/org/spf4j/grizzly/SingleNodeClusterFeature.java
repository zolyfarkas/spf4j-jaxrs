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
package org.spf4j.grizzly;

import com.google.common.collect.ImmutableSet;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.spf4j.service.avro.NetworkProtocol;
import org.spf4j.service.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.Service;
import org.spf4j.cluster.SingleNodeCluster;

/**
 * @author Zoltan Farkas
 */
public final class SingleNodeClusterFeature implements Feature {

  @Override
  public boolean configure(final FeatureContext fc) {
    Configuration cfg = fc.getConfiguration();
    String bindAddr = (String) cfg.getProperty("servlet.bindAddr");
    int port = (int) cfg.getProperty("servlet.port");
    try {
      SingleNodeCluster singleNodeCluster
              = new SingleNodeCluster(ImmutableSet.copyOf(InetAddress.getAllByName(bindAddr)),
                      Collections.singleton(new NetworkService("http",
                              port, NetworkProtocol.TCP)));
      fc.register(new BindCluster(singleNodeCluster));
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
    return true;
  }

  private static class BindCluster extends AbstractBinder {

    private final Cluster singleNodeCluster;

    BindCluster(final Cluster singleNodeCluster) {
      this.singleNodeCluster = singleNodeCluster;
    }

    @Override
    protected void configure() {
      bind(singleNodeCluster).to(Cluster.class);
      bind(singleNodeCluster).to(Service.class);
    }
  }

}
