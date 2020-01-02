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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.avro.Schema;
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
    sprotocol.serialize(MediaType.valueOf("application/avro"),
            headers::put, Schema.createArray(DemoRecord.getClassSchema()));
    Assert.assertThat(headers, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE,
            "application/avro;avsc=\""
                    + "{\\\"type\\\":\\\"array\\\",\\\"items\\\":"
                    + "{\\\"$ref\\\":\\\"org.spf4j.demo:jaxrs-spf4j-demo-schema:0.3:0\\\"}}\""));
  }

  @Test
  public void testSchemaProtocolRT() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    DefaultSchemaProtocol sprotocol = new DefaultSchemaProtocol(client);
    Map<String, String> headers = new HashMap<>();
    Schema schema = Schema.createArray(DemoRecord.getClassSchema());
    sprotocol.serialize(MediaType.valueOf("application/avro"), headers::put, schema);
    Assert.assertThat(headers, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE,
            "application/avro;avsc=\""
                    + "{\\\"type\\\":\\\"array\\\",\\\"items\\\":"
                    + "{\\\"$ref\\\":\\\"org.spf4j.demo:jaxrs-spf4j-demo-schema:0.3:0\\\"}}\""));
    Schema deserialize = sprotocol.deserialize(MediaType.valueOf(headers.get(HttpHeaders.CONTENT_TYPE)), headers::get,
            new DemoRecord[] {}.getClass(), new DemoRecord[] {}.getClass());
    Assert.assertEquals(schema, deserialize);
  }


  @Test
  public void testSchemaProtocol2() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    DefaultSchemaProtocol sprotocol = new DefaultSchemaProtocol(client);
    Map<String, String> headers = new HashMap<>();
    sprotocol.serialize(MediaType.valueOf("application/avro"),
            headers::put, Schema.createArray(Schema.create(Schema.Type.STRING)));
    Assert.assertThat(headers, Matchers.hasEntry(HttpHeaders.CONTENT_TYPE,
            "application/avro;avsc=\"{\\\"type\\\":\\\"array\\\",\\\"items\\\":\\\"string\\\"}\""));
  }

}
