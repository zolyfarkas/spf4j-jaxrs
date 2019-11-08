
package org.spf4j.jaxrs.common.providers.avro.stream;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.apache.avro.AvroArrayWriter;
import org.spf4j.jaxrs.AvroContainer;
import org.spf4j.jaxrs.Buffered;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroIterableMessageBodyWriter implements MessageBodyWriter<Iterable> {

  private final SchemaProtocol protocol;

  @Inject
  public AvroIterableMessageBodyWriter(final SchemaProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType) {
    return Iterable.class.isAssignableFrom(type);
  }

  public abstract Encoder getEncoder(Schema writerSchema, OutputStream os)
          throws IOException;

  /**
   * @inheritdoc
   */
  @Override
  public void writeTo(final Iterable t, final Class<?> type,
          final Type pgenericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    Schema schema;
    Schema elemSchema = t instanceof AvroContainer ? ((AvroContainer) t).getElementSchema() : null;
    Type elType;
    if (elemSchema == null) {
      ParameterizedType genericType = MessageBodyRWUtils.toParameterizedType(pgenericType);
      elType = genericType.getActualTypeArguments()[0];
      elemSchema = MessageBodyRWUtils.getAvroSchemaFromType(elType, annotations);
      schema = Schema.createArray(elemSchema);
    } else {
      schema = Schema.createArray(elemSchema);
      elType = Object.class;
    }
    protocol.serialize(httpHeaders::add, schema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(elemSchema);
      Encoder encoder = getEncoder(schema, entityStream);
      int bufferSize;
      if (t instanceof Buffered) {
        bufferSize = ((Buffered) t).getElementBufferSize();
      } else {
        bufferSize = 64;
      }

      AvroArrayWriter arrWriter = new AvroArrayWriter(encoder, writer,
              TypeToken.of(elType).getRawType(), bufferSize);
      for (Object o : t) {
        arrWriter.write(o);
      }
      arrWriter.close();
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + schema.getName(), e);
    } finally {
      if (t instanceof AutoCloseable) {
        try {
          ((AutoCloseable) t).close();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

}