package org.spf4j.jaxrs.common.providers.avro;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.spf4j.jaxrs.common.providers.ProviderUtils;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/avro+json"})
public final class JsonAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  public static final EncoderFactory ENC = EncoderFactory.get();

  @Inject
  public JsonAvroMessageBodyWriter(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Encoder getEncoder(final MediaType mediaType,
          final Schema writerSchema, final OutputStream os) throws IOException {
    Charset cs = ProviderUtils.getCharset(mediaType);
    if (cs == StandardCharsets.UTF_8) {
      return ENC.jsonEncoder(writerSchema, os);
    } else {
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, cs));
      return ENC.jsonEncoder(writerSchema, bw);
    }
  }

}
