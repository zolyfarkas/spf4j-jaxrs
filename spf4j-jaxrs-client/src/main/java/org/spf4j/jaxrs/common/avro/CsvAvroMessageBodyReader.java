package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.Decoder;
import org.spf4j.io.Csv;
import org.spf4j.avro.csv.CsvDecoder;
import org.spf4j.io.MemorizingBufferedInputStream;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"text/csv;fmt=avro"})
public final class CsvAvroMessageBodyReader extends AvroMessageBodyReader {

  @Inject
  public CsvAvroMessageBodyReader(final SchemaResolver client) {
    super(client);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    CsvDecoder decoder = new CsvDecoder(Csv.CSV.reader(new InputStreamReader(is, StandardCharsets.UTF_8)),
            writerSchema);
    decoder.skipHeader();
    return decoder;
  }

  public  InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8);
  }

}
