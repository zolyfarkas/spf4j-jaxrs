
package org.spf4j.cluster;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.spf4j.os.OperatingSystem;

/**
 * @author Zoltan Farkas
 */
public interface Cluster  extends Service {

  ClusterInfo getClusterInfo();

  @Override
  default ServiceInfo getServiceInfo() {
    return getClusterInfo();
  }

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
    return OperatingSystem.getHostName();
  }

}
