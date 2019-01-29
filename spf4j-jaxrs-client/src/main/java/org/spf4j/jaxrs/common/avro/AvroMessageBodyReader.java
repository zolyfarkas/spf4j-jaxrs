package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.reflect.ExtendedReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.spf4j.http.Headers;
import org.spf4j.io.MemorizingBufferedInputStream;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroMessageBodyReader implements MessageBodyReader<Object> {

  private final SchemaResolver client;

  @Inject
  public AvroMessageBodyReader(final SchemaResolver client) {
    this.client = client;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return type != void.class &&  type != Void.class;
  }

  public abstract Decoder getDecoder(Schema writerSchema, InputStream is)
          throws IOException;

  /**
   * Wrap the input stream. Overwrite to change the default wrapping.
   * @param pentityStream
   * @return
   */
  public  InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream);
  }

  /**
   * @inheritdoc
   */
  @Override
  public Object readFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
          final InputStream pentityStream)
          throws IOException {
    String schemaStr = httpHeaders.getFirst(Headers.CONTENT_SCHEMA);
    Schema readerSchema = null;
    Schema schema = ExtendedReflectData.get().getSchema(genericType != null ? genericType : type);
    if (schema != null) {
      readerSchema = schema;
    }
    Schema writerSchema;
    if (schemaStr != null) {
      writerSchema = new Schema.Parser(new AvroNamesRefResolver(client)).parse(schemaStr);
      if (readerSchema == null) {
        readerSchema = writerSchema;
      }
    } else if (readerSchema != null) { //no writer schema, will try to recode with the reader schema.
      writerSchema = readerSchema;
    } else {
      throw new UnsupportedOperationException("Unable to deserialize " + type);
    }
    DatumReader reader = new ReflectDatumReader(writerSchema, readerSchema);
    InputStream entityStream = wrapInputStream(pentityStream);
    try {
      Decoder decoder = getDecoder(writerSchema, entityStream);
      return reader.read(null, decoder);
    } catch (IOException | RuntimeException ex) {
      throw new RuntimeException(this + " parsing failed for " + writerSchema + ", from " + entityStream, ex);
    }
  }


}
