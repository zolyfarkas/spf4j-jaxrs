package org.spf4j.jaxrs.common.providers.avro;

import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/octet-stream;fmt=avro", "application/avro"})
public final class BinaryAvroMessageBodyReader extends AvroMessageBodyReader {


  @Inject
  public BinaryAvroMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) {
    return DecoderFactory.get().binaryDecoder(is, null);
  }

}
