
package org.spf4j.actuator.cluster.info;

import java.net.MalformedURLException;
import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import org.spf4j.service.avro.ClusterInfo;

/**
 * @author Zoltan Farkas
 */
public class ClusterInfoResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterInfoResourceTest.class);

  @Test(timeout = 10000)
  public void testClusterInfo() throws MalformedURLException {
    ClusterInfo ai = getTarget().path("info/cluster").request(MediaType.APPLICATION_JSON).get(ClusterInfo.class);
    LOG.debug("cluster info", ai);
    Assert.assertNotNull(ai);
  }

}
