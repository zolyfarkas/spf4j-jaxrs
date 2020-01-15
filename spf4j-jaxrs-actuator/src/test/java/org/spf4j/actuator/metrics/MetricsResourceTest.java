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
package org.spf4j.actuator.metrics;

import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.avro.generic.GenericRecord;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.ServiceIntegrationBase;
import static org.spf4j.actuator.ServiceIntegrationBase.getTarget;
import org.spf4j.base.CloseableIterable;
import org.spf4j.perf.impl.MeasurementsInfoImpl;
import org.spf4j.perf.impl.RecorderFactory;
import org.spf4j.tsdb2.avro.MeasurementType;

/**
 *
 * @author Zoltan Farkas
 */
public class MetricsResourceTest  extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(MetricsResourceTest.class);

  @Test
  public void testMetrics() throws IOException {
    long mid = RecorderFactory.MEASUREMENT_STORE.alocateMeasurements(
            new MeasurementsInfoImpl("test", "test measurement",
            new String[] {"a", "b"}, new String[] {"ms", "ms"}, MeasurementType.GAUGE), 0);
    RecorderFactory.MEASUREMENT_STORE.saveMeasurements(mid, System.currentTimeMillis(), 1, 2);
     List<String> metrics = getTarget().path("metrics/local")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
     LOG.debug("Metrics: {}", metrics);
     Assert.assertFalse(metrics.isEmpty());
     CloseableIterable<GenericRecord> measurements = getTarget().path("metrics/local/test/data")
            .request("application/avro").get(new GenericType<CloseableIterable<GenericRecord>>() { });
    Assert.assertNotNull(measurements);
    for (GenericRecord data : measurements) {
      LOG.debug("data", data);
      Assert.assertEquals(1L, data.get(1));
      Assert.assertEquals(2L, data.get(2));
    }
  }

  @Test
  public void testPrometheusMetrics() throws IOException {
    long mid = RecorderFactory.MEASUREMENT_STORE.alocateMeasurements(
            new MeasurementsInfoImpl("test", "test measurement",
            new String[] {"a", "b"}, new String[] {"ms", "ms"}, MeasurementType.GAUGE), 0);
    RecorderFactory.MEASUREMENT_STORE.saveMeasurements(mid, System.currentTimeMillis(), 1, 2);
     List<String> metrics = getTarget().path("metrics/local")
            .request(MediaType.APPLICATION_JSON).get(new GenericType<List<String>>() { });
     LOG.debug("Metrics: {}", metrics);
     Assert.assertFalse(metrics.isEmpty());
     CharSequence measurements = getTarget().path("metrics/local/test/data")
            .request(TextFormat.CONTENT_TYPE_004).get(CharSequence.class);
    Assert.assertNotNull(measurements);
    LOG.debug("Metrics data: {}", measurements);
    CharSequence allMeasurements = getTarget().path("metrics/local")
            .request(TextFormat.CONTENT_TYPE_004).get(CharSequence.class);
    Assert.assertNotNull(measurements);
    LOG.debug("All Metrics data: {}", allMeasurements);

  }

}
