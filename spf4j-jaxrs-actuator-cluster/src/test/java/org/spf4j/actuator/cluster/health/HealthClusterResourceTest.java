
package org.spf4j.actuator.cluster.health;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import org.spf4j.service.avro.HealthRecord;
import org.spf4j.io.Streams;

/**
 * @author Zoltan Farkas
 */
public class HealthClusterResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(HealthClusterResourceTest.class);

  @Test
  public void testHealthCheckCluster() {
    HealthRecord ai = getTarget().path("health/check/cluster")
            .request("application/avro").get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }


  @Test
  public void testHealthCheckCluster2() {
    HealthRecord ai = getTarget().path("health/check/cluster")
            .queryParam("debug", "true")
            .request("application/json").get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheckCluster2Capture() throws IOException {
    InputStream ai = getTarget().path("health/check/cluster")
            .queryParam("debug", "true")
            .request("application/json").get(InputStream.class);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Streams.copy(ai, bos);
    LOG.debug(new String(bos.toByteArray(), StandardCharsets.UTF_8));
  }

}
