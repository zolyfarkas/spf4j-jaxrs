package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ExtendedJsonEncoder;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/json;fmt=avro-x", "application/avro-x+json", "text/plain;fmt=avro-x+json"})
public final class XJsonAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  @Inject
  public XJsonAvroMessageBodyWriter(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Encoder getEncoder(final Schema writerSchema, final OutputStream os) throws IOException {
    return new ExtendedJsonEncoder(writerSchema, os);
  }

  @Override
  public void writeTo(final Object t, final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, "application/avro-x+json;charset=UTF-8");
    super.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
  }

}
