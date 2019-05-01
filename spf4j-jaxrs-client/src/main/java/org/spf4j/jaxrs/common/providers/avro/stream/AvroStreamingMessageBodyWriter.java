
package org.spf4j.jaxrs.common.providers.avro.stream;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ExtendedReflectData;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.spf4j.avro.AvroArrayWriter;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;
import org.spf4j.jaxrs.StreamingArrayOutput;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroStreamingMessageBodyWriter implements MessageBodyWriter<StreamingArrayOutput> {

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
    return StreamingArrayOutput.class.isAssignableFrom(type);
  }

  public abstract Encoder getEncoder(Schema writerSchema, OutputStream os)
          throws IOException;

  /**
   * @inheritdoc
   */
  @Override
  public void writeTo(final StreamingArrayOutput t, final Class<?> type,
          final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    ExtendedReflectData reflector = ExtendedReflectData.get();
    if (!(genericType instanceof ParameterizedType)) {
      throw new IllegalStateException("ArrayStreamingOutput type parameters must be known " + genericType);
    }
    Type elType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
    Schema elemSchema = reflector.getSchema(elType);
    if (elemSchema == null) {
      elemSchema = reflector.createSchema(elType, t, new HashMap<>());
    }
    Schema schema = Schema.createArray(elemSchema);
    protocol.serialize(httpHeaders::add, schema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(elemSchema);
      Encoder encoder = getEncoder(schema, entityStream);
      AvroArrayWriter arrWriter = new AvroArrayWriter(encoder, writer,
              TypeToken.of(elType).getRawType(), 128);
      t.write(arrWriter);
      arrWriter.close();
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + schema.getName(), e);
    }
  }

}