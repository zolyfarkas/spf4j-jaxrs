package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.spf4j.io.Csv;
import org.spf4j.avro.csv.CsvDecoder;
import org.spf4j.avro.DecodedSchema;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.io.csv.CsvReader;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"text/csv;fmt=avro"})
public final class CsvAvroMessageBodyReader extends AvroMessageBodyReader {

  @Inject
  public CsvAvroMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public DecodedSchema tryDecodeSchema(final Schema readerSchema, final InputStream is, final Annotation[] annotations)
          throws IOException {
    return CsvDecoder.tryDecodeSchema(is, readerSchema);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    CsvReader reader = Csv.CSV.reader(new InputStreamReader(is, StandardCharsets.UTF_8));
    try {
      reader.skipRow(); // skip headers
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
    return new CsvDecoder(reader, writerSchema);
  }

  public InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8);
  }

}
