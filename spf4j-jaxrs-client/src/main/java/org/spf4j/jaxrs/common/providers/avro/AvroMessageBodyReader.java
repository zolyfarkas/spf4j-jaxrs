package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.reflect.ReflectDatumReader;
import org.spf4j.avro.DecodedSchema;
import org.spf4j.io.MemorizingBufferedInputStream;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroMessageBodyReader implements MessageBodyReader<Object> {

  private final SchemaProtocol protocol;

  @Inject
  public AvroMessageBodyReader(final SchemaProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return IndexedRecord.class.isAssignableFrom(type)
            || (type != void.class &&  type != Void.class
            && Iterable.class != type
            && !InputStream.class.isAssignableFrom(type)
            && !Reader.class.isAssignableFrom(type));
  }

  /** Overwrite for decoders that are capable of decoding schema from stream  */
  @Nullable
  public DecodedSchema tryDecodeSchema(@Nullable final Schema readerSchema,
          final InputStream is, final Annotation[] annotations)
          throws IOException {
    return null;
  }


  public abstract Decoder getDecoder(Schema writerSchema, InputStream is)
          throws IOException;

  /**
   * Wrap the input stream. Overwrite to change the default wrapping.
   * @param pentityStream
   * @return
   */
  public  InputStream wrapInputStream(final InputStream pentityStream) throws IOException {
    return new MemorizingBufferedInputStream(pentityStream);
  }

  /**
   * @inheritdoc
   */
  @Override
  public Object readFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
          final InputStream ientityStream)
          throws IOException {
    final PushbackInputStream pentityStream = new PushbackInputStream(ientityStream);
    int read = pentityStream.read();
    if (read < 0) {
      // Jersey considers null as 204 No content.
      return null;
    }
    pentityStream.unread(read);
    Schema writerSchema = protocol.deserialize(mediaType, httpHeaders::getFirst, type, genericType);
    Schema readerSchema = MessageBodyRWUtils.getAvroSchemaFromType(type, genericType, annotations);
    InputStream entityStream = wrapInputStream(pentityStream);
    Decoder decoder = null;
    if (writerSchema == null) {
      DecodedSchema tryDecodeSchema = tryDecodeSchema(readerSchema, entityStream, annotations);
      if (tryDecodeSchema != null) {
        decoder = tryDecodeSchema.getDecoder();
        writerSchema = tryDecodeSchema.getSchema();
      }
    }
    if (writerSchema  == null && readerSchema == null) {
        throw new UnsupportedOperationException("Unable to deserialize " + type);
    } else if (writerSchema == null && readerSchema != null) {
      writerSchema = readerSchema;
    } else if (writerSchema != null && readerSchema == null) {
      readerSchema = writerSchema;
    }
    DatumReader reader = new ReflectDatumReader(writerSchema, readerSchema);
    try {
      if (decoder == null) {
        decoder = getDecoder(writerSchema, entityStream);
      }
      return reader.read(null, decoder);
    } catch (IOException | RuntimeException ex) {
      throw new RuntimeException(this + " parsing failed for " + writerSchema.getFullName()
              + ", from " + entityStream, ex);
    }
  }


}
