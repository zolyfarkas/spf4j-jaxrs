package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
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
@Produces({"application/avro+json"})
public final class JsonAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  @Inject
  public JsonAvroMessageBodyWriter(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Encoder getEncoder(final Schema writerSchema, final OutputStream os) throws IOException {
    return EncoderFactory.get().jsonEncoder(writerSchema, os);
  }

}
