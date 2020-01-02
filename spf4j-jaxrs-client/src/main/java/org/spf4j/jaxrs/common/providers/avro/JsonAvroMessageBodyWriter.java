package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
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

  @Override
  public void writeTo(final Object t, final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    super.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
  }

}
