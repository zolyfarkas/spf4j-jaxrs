
package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;

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

  public abstract Encoder getEncoder(Schema writerSchema, OutputStream os)
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
    Schema schema = MessageBodyRWUtils.getAvroSchemaFromType(type, genericType, t, annotations);
    protocol.serialize(mediaType, httpHeaders::add, schema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(schema);
      Encoder encoder = getEncoder(schema, entityStream);
      writer.write(t, encoder);
      encoder.flush();
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + schema.getName(), e);
    }
  }

}