
package org.spf4j.cluster;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Zoltan Farkas
 */
public interface Cluster {

  ClusterInfo getClusterInfo();

  static Set<InetAddress> getLocalAddresses() {
    Set<InetAddress> result = new HashSet<>(4);
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        // filters out 127.0.0.1 and inactive interfaces
        if (iface.isLoopback() || !iface.isUp()  ||  iface.isVirtual() || iface.isPointToPoint()) {
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

  default String getLocalHostName() {
    ClusterInfo clusterInfo = getClusterInfo();
    Set<InetAddress> localAddresses = clusterInfo.getLocalAddresses();
    Set<InetAddress> clusterAddresses = clusterInfo.getAddresses();
    UnmodifiableIterator<InetAddress> iterator
            = Sets.intersection(localAddresses, clusterAddresses)
                    .iterator();
    if (iterator.hasNext()) {
      InetAddress next = iterator.next();
      if (iterator.hasNext()) {
        throw new IllegalStateException("Multiple local adresses " + localAddresses
              + "within cluster addresses " + clusterAddresses);
      }
      return next.getHostName();
    } else {
      throw new IllegalStateException("local adresses " + localAddresses
              + "not within cluster addresses " + clusterAddresses);
    }
  }

}
