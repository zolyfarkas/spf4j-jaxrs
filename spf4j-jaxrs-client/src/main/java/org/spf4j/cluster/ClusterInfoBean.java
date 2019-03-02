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
import java.util.Set;
import org.spf4j.base.avro.NetworkService;

/**
 * @author Zoltan Farkas
 */
public class ClusterInfoBean implements ClusterInfo {

  private final Set<InetAddress> allAddresses;

  private final Set<InetAddress> localAddresses;

  private final Set<NetworkService> services;

  public ClusterInfoBean(final Set<InetAddress> allAddresses,
          final Set<InetAddress> localAddresses, final Set<NetworkService> services) {
    this.allAddresses = allAddresses;
    this.services = services;
    this.localAddresses = localAddresses;
  }

  @Override
  public final Set<InetAddress> getAddresses() {
    return allAddresses;
  }

  @Override
  public final Set<InetAddress> getLocalAddresses() {
    return localAddresses;
  }

  @Override
  public final Set<NetworkService> getServices() {
    return services;
  }

  /**
   * overwrite as needed.
   */
  @Override
  public String toString() {
    return "ClusterInfoBean{" + "allAddresses=" + allAddresses + ", localAddresses="
            + localAddresses + ", services=" + services + '}';
  }



}
