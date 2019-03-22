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
package org.spf4j.cluster;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;
import org.spf4j.base.avro.NetworkService;

/**
 * @author Zoltan Farkas
 */
public final class SingleNodeCluster implements Cluster {

  private final ClusterInfoBean clusterInfo;

  public SingleNodeCluster(final Set<NetworkService> services) {
    this(Collections.unmodifiableSet(Cluster.getLocalAddresses()), services);
  }

  public SingleNodeCluster(final Set<InetAddress> localAddresses, final Set<NetworkService> services) {
    this.clusterInfo = new ClusterInfoBean(localAddresses, localAddresses, services);
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return this.clusterInfo;
  }

  @Override
  public String toString() {
    return "SingleNodeCluster{" + "clusterInfo=" + clusterInfo + '}';
  }

}
