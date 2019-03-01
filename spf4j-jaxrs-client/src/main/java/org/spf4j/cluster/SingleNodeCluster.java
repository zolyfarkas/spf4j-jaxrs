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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.spf4j.base.avro.NetworkService;

/**
 * @author Zoltan Farkas
 */
public class SingleNodeCluster implements Cluster {

  private final Set<InetAddress> localAddresses;

  private final Set<NetworkService> services;

  public SingleNodeCluster(final Set<NetworkService> services) {
    this(Collections.unmodifiableSet(getLocalAddresses()), services);
  }

  public SingleNodeCluster(final Set<InetAddress> localAddresses, final Set<NetworkService> services) {
    this.localAddresses = localAddresses;
    this.services = services;
  }

  /**
   * Overwrite this for a multi-node cluster implementation.
   * @return
   */
  @Override
  public Set<InetAddress> getNodes() {
    return getLocalNodes();
  }

  @Override
  public final Set<InetAddress> getLocalNodes() {
    return localAddresses;
  }

  private static Set<InetAddress> getLocalAddresses() {
    Set<InetAddress> result = new HashSet<>(4);
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        // filters out 127.0.0.1 and inactive interfaces
        if (iface.isLoopback() || !iface.isUp()) {
          continue;
        }
        Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          result.add(addresses.nextElement());
        }
      }
      return result;
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public final Set<NetworkService> getServices() {
    return services;
  }

  /**
   * Extending classes will overwrite as necessary.
   * @return
   */
  @Override
  public String toString() {
    return "SingleNodeCluster{" + "localAddresses=" + localAddresses + ", services=" + services + '}';
  }


}
