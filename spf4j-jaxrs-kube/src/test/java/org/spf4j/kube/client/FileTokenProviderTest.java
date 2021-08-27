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
package org.spf4j.kube.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class FileTokenProviderTest {

  @Test
  public void testFileTokeSupplier() throws IOException {
    Path path = Files.createTempFile("token", ".tmp");
    String testStr = "Test string";
    Files.write(path, testStr.getBytes(StandardCharsets.UTF_8));
    FileTokenProvider tp = new FileTokenProvider(path);
    StringBuilder sb = new StringBuilder();
    tp.access((c, o, l) ->  sb.append(c, o, l));
    Assert.assertEquals(testStr, sb.toString());
  }

}
