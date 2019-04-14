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
package org.spf4j.jaxrs.common.providers.avro;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.avro.SchemaClient;
import org.spf4j.demo.avro.DemoRecord;

/**
 *
 * @author Zoltan Farkas
 */
public class DefaultSchemaProtocolTest {


  @Test
  public void testSchemaProtocol() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    DefaultSchemaProtocol sprotocol = new DefaultSchemaProtocol(client);
    Map<String, String> headers = new HashMap<>();
    sprotocol.serialize(headers::put, Schema.createArray(DemoRecord.getClassSchema()));
    Assert.assertThat(headers, Matchers.hasEntry("content-schema",
            "{\"type\":\"array\",\"items\":{\"$ref\":\"org.spf4j.demo:jaxrs-spf4j-demo-schema:0.2:0\"}}"));
  }

  @Test
  public void testSchemaProtocol2() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    DefaultSchemaProtocol sprotocol = new DefaultSchemaProtocol(client);
    Map<String, String> headers = new HashMap<>();
    sprotocol.serialize(headers::put, Schema.createArray(Schema.create(Schema.Type.STRING)));
    Assert.assertThat(headers, Matchers.hasEntry("content-schema",
            "{\"type\":\"array\",\"items\":\"string\"}"));
  }

}
