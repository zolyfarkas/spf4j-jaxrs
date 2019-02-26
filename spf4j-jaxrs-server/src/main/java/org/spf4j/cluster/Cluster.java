
package org.spf4j.cluster;

import com.google.common.net.HostAndPort;

/**
 * @author Zoltan Farkas
 */
public interface Cluster {

  Iterable<HostAndPort> getEndpoints();
  
}
