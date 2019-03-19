package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.ExtendedJsonDecoder;
import org.spf4j.io.MemorizingBufferedInputStream;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/json;fmt=avro-x", "application/avro-x+json", "text/plain;fmt=avro-x"})
public final class XJsonAvroMessageBodyReader extends AvroMessageBodyReader {


  @Inject
  public XJsonAvroMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    return new ExtendedJsonDecoder(writerSchema, is);
  }

  public  InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8);
  }

}
