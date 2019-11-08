
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
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;
import org.spf4j.jaxrs.StreamingArrayContent;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroStreamingMessageBodyWriter implements MessageBodyWriter<StreamingArrayContent> {

  private final SchemaProtocol protocol;

  @Inject
  public AvroStreamingMessageBodyWriter(final SchemaProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType) {
    return StreamingArrayContent.class.isAssignableFrom(type);
  }

  public abstract Encoder getEncoder(Schema writerSchema, OutputStream os)
          throws IOException;

  /**
   * @inheritdoc
   */
  @Override
  public void writeTo(final StreamingArrayContent t, final Class<?> type,
          final Type pgenericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    ParameterizedType genericType = MessageBodyRWUtils.toParameterizedType(pgenericType);
    Type elType = genericType.getActualTypeArguments()[0];
    Schema schema;
    Schema elemSchema = t.getElementSchema();
    if (elemSchema == null) {
      elemSchema = MessageBodyRWUtils.getAvroSchemaFromType(elType, annotations);
      schema = Schema.createArray(elemSchema);
    } else {
      schema = Schema.createArray(elemSchema);
    }
    protocol.serialize(httpHeaders::add, schema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(elemSchema);
      Encoder encoder = getEncoder(schema, entityStream);
      AvroArrayWriter arrWriter = new AvroArrayWriter(encoder, writer,
              TypeToken.of(elType).getRawType(), t.getElementBufferSize());
      t.write(arrWriter);
      arrWriter.close();
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + schema.getName(), e);
    }
  }

}