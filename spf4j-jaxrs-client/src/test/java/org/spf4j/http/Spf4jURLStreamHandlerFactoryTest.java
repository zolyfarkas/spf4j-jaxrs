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
package org.spf4j.http;

import org.spf4j.http.multi.MultiURLs;
import org.spf4j.http.multi.Spf4jURLStreamHandlerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.io.Streams;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;

/**
 *
 * @author Zoltan Farkas
 */
public class Spf4jURLStreamHandlerFactoryTest {

  static {
    URL.setURLStreamHandlerFactory(new Spf4jURLStreamHandlerFactory());
  }


  @Test
  public void testCustomProtocol() throws IOException, URISyntaxException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    URL url =  MultiURLs.newURL(MultiURLs.Protocol.mhttp, "http://bla.nowhere", "http://www.google.com");
    try (InputStream openStream = url.openStream()) {
      Streams.copy(openStream, bos);
    }
    Assert.assertThat(new String(bos.toByteArray(), StandardCharsets.UTF_8), Matchers.containsString("google"));
  }

  @Test
  public void testCustomProtocolJaxrs() throws IOException, URISyntaxException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    URL url =  MultiURLs.newURL(MultiURLs.Protocol.mhttp, "http://bla.nowhere", "http://www.google.com");
    Spf4JClient cl = Spf4JClient.create(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .register(new ExecutionContextClientFilter(DeadlineProtocol.NONE, false))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());
    try (InputStream is = cl.target(url.toString()).request().get(InputStream.class)) {
      Streams.copy(is, bos);
    }
    Assert.assertThat(new String(bos.toByteArray(), StandardCharsets.UTF_8), Matchers.containsString("google"));
  }

}
