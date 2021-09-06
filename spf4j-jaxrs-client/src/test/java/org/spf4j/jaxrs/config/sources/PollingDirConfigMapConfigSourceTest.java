/*
 * Copyright 2021 SPF4J.
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
package org.spf4j.jaxrs.config.sources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.TimeSource;
import org.spf4j.jaxrs.config.ConfigEvent;
import org.spf4j.jaxrs.config.ConfigWatcher;
import static org.spf4j.jaxrs.config.sources.DirConfigMapConfigSourceTest.assertDirBasedConfigs;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("MDM_THREAD_YIELD")
public class PollingDirConfigMapConfigSourceTest {

  private static final Logger LOG = LoggerFactory.getLogger(PollingDirConfigMapConfigSourceTest.class);

  @Test
  public void tesDirBasedConfigs() throws IOException, InterruptedException {
    Path testConfig = Files.createTempDirectory("temp");
    assertDirBasedConfigs(new PollingDirConfigMapConfigSource(testConfig, StandardCharsets.UTF_8, 1));
  }

  @Test
  public void testDirBasedConfigsWithSymlinks() throws IOException, InterruptedException {
    Path testConfig = Files.createTempDirectory("temp");
    Path dataFolder = testConfig.resolve("..2021_09_01_16_54_44.890995881");
    Files.createDirectory(dataFolder);
    String testproperty = "test.property";
    Path targetFile = dataFolder.resolve(testproperty);
    Files.write(targetFile, "bla bla".getBytes(StandardCharsets.UTF_8));
    Files.createSymbolicLink(testConfig.resolve("..data"), Paths.get("..2021_09_01_16_54_44.890995881"));
    Files.createSymbolicLink(testConfig.resolve(testproperty), Paths.get("..data").resolve(testproperty));

    try (PollingDirConfigMapConfigSource dcfg
            = new PollingDirConfigMapConfigSource(testConfig, StandardCharsets.UTF_8, 1)) {
      Map<String, String> updateMap = updateableMap(dcfg);
      assertValue(updateMap, testproperty, "bla bla", 5000);
      Files.write(targetFile, "bla bla 2".getBytes(StandardCharsets.UTF_8));
      LOG.debug("Written bla bla 2");
      assertValue(updateMap, testproperty, "bla bla 2", 5000);
    }

  }

  private void assertValue(final Map<String, String> map, final String key,
          final String expected, final long timeoutMillis)
          throws InterruptedException {
    long deadline = TimeSource.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    String actual = null;
    do {
      actual = map.get(key);
      if (Objects.equals(expected, actual)) {
        return;
      }
      Thread.sleep(10);
    } while (TimeSource.nanoTime() < deadline);
    throw new AssertionError("Expected value did not arive: " + expected + " actual = " + actual
            + " Configs: " + map);
  }

  private static Map<String, String> updateableMap(final ObservableDirConfigMapConfigSource cfg) {
    Map<String, String> configs = new ConcurrentHashMap<>();
    cfg.addWatcher(new ConfigWatcher() {

      public void unknownEvents() {
        for (String name : cfg.getPropertyNames()) {
          String value = cfg.getValue(name);
          if (value == null) {
            LOG.debug("Null value for {}", name);
          } else {
            configs.put(name, value);
          }
        }
      }

      @Override
      public void accept(final String property, final ConfigEvent event) {
        LOG.debug("Received event", event);
        switch (event) {
          case ADDED:
          case MODIFIED:
            configs.put(property, cfg.getValue(property));
            break;
          case DELETED:
            configs.remove(property);
            break;
          default:
            throw new IllegalStateException();
        }
      }

      @Override
      public void close() {
      }
    });
    return configs;
  }

}
