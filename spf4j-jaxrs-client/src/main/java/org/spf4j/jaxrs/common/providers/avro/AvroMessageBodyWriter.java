
package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.spf4j.avro.schema.Schemas;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroMessageBodyWriter implements MessageBodyWriter<Object> {

  private final SchemaProtocol protocol;

  @Inject
  public AvroMessageBodyWriter(final SchemaProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType) {
    return IndexedRecord.class.isAssignableFrom(type) || (type != void.class &&  type != Void.class
            && !OutputStream.class.isAssignableFrom(type)
            && !Writer.class.isAssignableFrom(type)  && !Iterable.class.isAssignableFrom(type));
  }

  public abstract Encoder getEncoder(MediaType mediaType, Schema writerSchema, OutputStream os)
          throws IOException;


  /**
   * @inheritdoc
   */
  @Override
  public void writeTo(final Object t, final Class<?> type,
          final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    Schema acceptedSchema = protocol.getAcceptableSchema(mediaType);
    Schema actualSchema = MessageBodyRWUtils.getAvroSchemaFromType(type, genericType, t, annotations);
    Schema responseSchema;
    Object resp;
    if (acceptedSchema == null) {
      responseSchema = actualSchema;
      resp =  t;
    } else {
      responseSchema = acceptedSchema;
      try {
        resp = Schemas.project(actualSchema, responseSchema, t);
      } catch (RuntimeException ex) {
        throw new ClientErrorException("Requested schema cannot be served: " + acceptedSchema, 400, ex);
      }
    }
    protocol.serialize(mediaType, httpHeaders::putSingle, responseSchema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(responseSchema);
      Encoder encoder = getEncoder(mediaType, responseSchema, entityStream);
      writer.write(resp, encoder);
      encoder.flush();
    } catch (IOException | RuntimeException e) {
      String name = responseSchema.getName();
      throw new RuntimeException("Serialization failed for " + (name != null
              ? name : responseSchema), e);
    }
  }

}