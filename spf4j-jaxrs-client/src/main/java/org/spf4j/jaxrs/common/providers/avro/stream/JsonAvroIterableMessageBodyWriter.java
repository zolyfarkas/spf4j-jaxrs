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
package org.spf4j.jaxrs.common.providers.avro.stream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.spf4j.jaxrs.common.providers.ProviderUtils;
import org.spf4j.jaxrs.common.providers.avro.JsonAvroMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;

/**
 *
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/avro+json"})
public final class JsonAvroIterableMessageBodyWriter  extends AvroIterableMessageBodyWriter {

  public JsonAvroIterableMessageBodyWriter(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Encoder getEncoder(final MediaType mediaType,
          final Schema writerSchema, final OutputStream os) throws IOException {
    Charset cs = ProviderUtils.getCharset(mediaType);
    if (cs == StandardCharsets.UTF_8) {
      return JsonAvroMessageBodyWriter.ENC.jsonEncoder(writerSchema, os);
    } else {
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, cs));
      return JsonAvroMessageBodyWriter.ENC.jsonEncoder(writerSchema, bw);
    }
  }
}
