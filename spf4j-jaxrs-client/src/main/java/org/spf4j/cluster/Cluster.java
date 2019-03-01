
package org.spf4j.cluster;

import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.util.Set;
import org.spf4j.base.avro.NetworkService;

/**
 * @author Zoltan Farkas
 */
public interface Cluster {

  Set<InetAddress> getNodes();

  Set<InetAddress> getLocalNodes();

  default Set<InetAddress> getOtherNodes() {
    return Sets.difference(getNodes(), getLocalNodes());
  }

  Set<NetworkService> getServices();

}
