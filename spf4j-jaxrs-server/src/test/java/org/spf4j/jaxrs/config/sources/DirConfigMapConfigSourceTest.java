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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.jaxrs.config.ConfigEvent;
import org.spf4j.jaxrs.config.PropertyWatcher;

/**
 *
 * @author Zoltan Farkas
 */
public class DirConfigMapConfigSourceTest {

  private static final Logger LOG = LoggerFactory.getLogger(DirConfigMapConfigSourceTest.class);

  @Test
  public void tesDirBasedConfigs() throws IOException, InterruptedException {
    Path testConfig = Files.createTempDirectory("temp");
    try (DirConfigMapConfigSource cfg = new DirConfigMapConfigSource(testConfig, StandardCharsets.UTF_8)) {
      Assert.assertNull(cfg.getValue("testProp"));
      Map<String, String> configs = new ConcurrentHashMap<>();
      BlockingQueue<ConfigEvent> queue = new LinkedBlockingDeque<>();
      cfg.addWatcher("testProp", new PropertyWatcher() {
        public void accept(final ConfigEvent event) {
          LOG.debug("Received event", event);
          switch (event) {
            case ADDED:
            case MODIFIED:
              configs.put("testProp", cfg.getValue("testProp"));
              break;
            case DELETED:
              configs.remove("testProp");
              break;
            default:
              throw new IllegalStateException();
          }
          if (!queue.offer(event)) {
            throw new IllegalStateException();
          }
        }

        public void unknownEvents() {
          throw new IllegalStateException();
        }
      });
      Path testProp = testConfig.resolve("testProp");
      Files.write(testProp, "bla bla".getBytes(StandardCharsets.UTF_8));
      Assert.assertNotNull(queue.poll(10, TimeUnit.SECONDS));
      Assert.assertEquals("bla bla", configs.get("testProp"));
      Files.write(testProp, "bla bla 2".getBytes(StandardCharsets.UTF_8));
      Assert.assertNotNull(queue.poll(10, TimeUnit.SECONDS));
      Assert.assertEquals("bla bla 2", configs.get("testProp"));
      Files.delete(testProp);
      Assert.assertNotNull(queue.poll(10, TimeUnit.SECONDS));
      Assert.assertNull(configs.get("testProp"));
    }

  }

}
