package org.spf4j.jaxrs.common.providers.avro;

import java.io.OutputStream;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/octet-stream;fmt=avro", "application/avro"})
public final class BinaryAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  @Inject
  public BinaryAvroMessageBodyWriter(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Encoder getEncoder(final Schema writerSchema, final OutputStream os) {
    return EncoderFactory.get().binaryEncoder(os, null);
  }

}
