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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.spf4j.avro.csv.CsvDecoder;
import org.spf4j.io.Csv;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.jaxrs.common.avro.CsvAvroMessageBodyReader;
import org.spf4j.jaxrs.common.avro.DecodedSchema;
import org.spf4j.jaxrs.common.avro.SchemaProtocol;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes("text/csv")
public final class CsvAvroArrayMessageBodyReader extends AvroArrayMessageBodyReader {

  public CsvAvroArrayMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public DecodedSchema tryDecodeSchema(final Schema readerSchema, final InputStream is, final Annotation[] annotations)
          throws IOException {
    return CsvAvroMessageBodyReader.tryDecodeSchema(is, readerSchema);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    CsvDecoder decoder = new CsvDecoder(Csv.CSV.readerILEL(new InputStreamReader(is, StandardCharsets.UTF_8)),
            writerSchema);
    try {
      decoder.skipHeader();
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
    return decoder;
  }

  public  InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8);
  }


}
