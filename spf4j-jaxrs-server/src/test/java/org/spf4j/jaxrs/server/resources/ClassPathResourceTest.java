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
package org.spf4j.jaxrs.server.resources;

import java.io.IOException;
import java.util.Arrays;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class ClassPathResourceTest {

  @Test
  public void testExtDetect() {
    MediaType pathMediaType = ClassPathResource.getPathMediaType("index.html");
    Assert.assertEquals(MediaType.TEXT_HTML_TYPE, pathMediaType);
  }

  @Test
  public void testPathValidation() throws IOException {
    ClassPathResource res = new ClassPathResource("static", Arrays.asList("index.html", "index.htm"));
    Response resp = res.staticResources("/");
    Assert.assertNotNull(resp.getEntity());
    try {
      res.staticResources("../stuff.txt");
      Assert.fail();
    } catch (ForbiddenException ex) {

    }
    try {
      Response staticResources = res.staticResources("%2e%2e%2fstuff.txt");
      Assert.assertEquals(404, staticResources.getStatus());
    } catch (ForbiddenException ex) {

    }
  }

}
