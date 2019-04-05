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
package org.spf4j.kube.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.TimeSource;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.cluster.ClusterInfoBean;
import org.spf4j.kube.client.Client;
import org.spf4j.kube.client.Endpoints;

/**
 *
 * @author Zoltan Farkas
 */
public final class KubeCluster implements Cluster {

  /** A node should not bother the master more the 1/s by default */
  private static final long MAX_AGE_NANOS
          = TimeUnit.MILLISECONDS.toNanos(Long.getLong("spf4j.kube.cluster.maxAgeMillis", 1000));

  private final Client client;

  private final Set<InetAddress> localAddresses;

  private final String nameSpace;
  private final String endpointName;

  private volatile ClusterState state;


  public KubeCluster(final Client client, final String nameSpace, final String endpointName) {
    this(Cluster.getLocalAddresses(), client, nameSpace, endpointName);
  }


  public KubeCluster(final Set<InetAddress> localAddresses,
          final Client client, final String nameSpace, final String endpointName) {
    this.client = client;
    this.localAddresses = localAddresses;
    this.nameSpace = nameSpace;
    this.endpointName = endpointName;
    this.state = null;
  }

  @Override
  public ClusterInfo getClusterInfo() {
    long nanoTime = TimeSource.nanoTime();
    ClusterState lState = state;
    if (lState == null) {
      synchronized (client) {
        lState = state;
        if (lState == null) {
          ClusterInfo clusterInfoNow = getClusterInfoNow();
          state = new ClusterState(nanoTime, clusterInfoNow);
          return clusterInfoNow;
        }
      }
    }
    if (nanoTime - lState.getAsOfnanos() > MAX_AGE_NANOS) {
      synchronized (client) {
        lState = state;
        if (nanoTime - lState.getAsOfnanos() > MAX_AGE_NANOS) {
          ClusterInfo clusterInfoNow = getClusterInfoNow();
          state = new ClusterState(nanoTime, clusterInfoNow);
          return clusterInfoNow;
        } else {
          return lState.getInfo();
        }
      }
    } else {
      return lState.getInfo();
    }

  }


  public ClusterInfo getClusterInfoNow() {
    Set<InetAddress> addrs = new HashSet<>();
    Set<NetworkService> svcs = new HashSet<>(4);
    for (Endpoints.SubSet ss : client.getEndpoints(nameSpace, endpointName).getSubsets()) {
      List<Endpoints.Address> addresses = ss.getAddresses();
      if (addresses != null) { // during setup addresses are listed as not ready.
        for (Endpoints.Address adr : addresses) {
          try {
            addrs.add(InetAddress.getByName(adr.getIp()));
          } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
      for (Endpoints.Port port : ss.getPorts()) {
        svcs.add(new NetworkService(port.getName(), port.getPort(), port.getProtocol()));
      }
    }
    return new ClusterInfoBean(addrs, localAddresses, svcs);
  }

  @Override
  public String toString() {
    return "KubeCluster{" + "client=" + client + ", localAddresses=" + localAddresses
            + ", nameSpace=" + nameSpace + ", endpointName=" + endpointName + ", state=" + state + '}';
  }



}
