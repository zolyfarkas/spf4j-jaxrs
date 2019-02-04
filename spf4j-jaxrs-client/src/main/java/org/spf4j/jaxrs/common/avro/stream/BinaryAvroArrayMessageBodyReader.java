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
package org.spf4j.jaxrs.common.avro.stream;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.jaxrs.common.avro.SchemaProtocol;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/octet-stream;fmt=avro", "application/avro"})
public final class BinaryAvroArrayMessageBodyReader extends AvroArrayMessageBodyReader {

  public BinaryAvroArrayMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is)  {
    return DecoderFactory.get().binaryDecoder(is, null);
  }

  public  InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8);
  }


}
