
package org.spf4j.actuator.health;

import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import org.spf4j.service.avro.HealthCheckInfo;
import org.spf4j.service.avro.HealthRecord;

/**
 * @author Zoltan Farkas
 */
public class HealthResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(HealthResourceTest.class);

  @Test
  public void testHealthInfo() {
    HealthCheckInfo ai = getTarget().path("health/info")
            .request(MediaType.APPLICATION_JSON).get(HealthCheckInfo.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthInfoLocal() {
    HealthCheckInfo ai = getTarget().path("health/info/local")
            .request(MediaType.APPLICATION_JSON).get(HealthCheckInfo.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheck() {
    HealthRecord ai = getTarget().path("health/check")
            .request(MediaType.APPLICATION_JSON).get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheckLocal() {
    HealthRecord ai = getTarget().path("health/check/local")
            .request(MediaType.APPLICATION_JSON).get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheckSpecific() {
    HealthRecord ai = getTarget().path("health/check/local/deadlock")
            .request(MediaType.APPLICATION_JSON).get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

}
