package org.spf4j.jaxrs.common.providers.avro.stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.jaxrs.CloseableIterable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.reflect.ReflectDatumReader;
import org.spf4j.avro.ArrayIterator;
import org.spf4j.avro.DecodedSchema;
import org.spf4j.avro.MapIterator;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroIterableMessageBodyReader implements MessageBodyReader<Iterable> {

  private final SchemaProtocol protocol;

  @Inject
  public AvroIterableMessageBodyReader(final SchemaProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return CloseableIterable.class == type;
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
   *
   * @param pentityStream
   * @return
   */
  public InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream);
  }

  /**
   * @inheritdoc
   */
  @Override
  public Iterable readFrom(final Class<Iterable> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
          final InputStream pentityStream)
          throws IOException {
    Schema writerSchema = protocol.deserialize(httpHeaders::getFirst, (Class) type, genericType);
    Schema readerSchema = MessageBodyRWUtils.getAvroSchemaFromType(type, genericType, annotations);
    Decoder decoder = null;
    InputStream entityStream = wrapInputStream(pentityStream);
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
    if (decoder == null) {
      decoder = getDecoder(writerSchema, entityStream);
    }
    if (readerSchema.getType() == Schema.Type.ARRAY) {
      DatumReader reader = new ReflectDatumReader(writerSchema.getElementType(), readerSchema.getElementType());
      return new IterableAdaptor(pentityStream, new ArrayIterator(decoder, reader));
    } else if (readerSchema.getType() == Schema.Type.MAP) {
      DatumReader reader = new ReflectDatumReader(writerSchema.getValueType(), readerSchema.getValueType());
      return new IterableAdaptor(pentityStream, new MapIterator(decoder, reader));
    } else {
      throw new IllegalStateException("invalid reader schema " + readerSchema + " for " + genericType);
    }
  }


  private static class IterableAdaptor implements CloseableIterable {

    private final InputStream pentityStream;
    private final Iterator iterator;

    IterableAdaptor(final InputStream pentityStream, final Iterator iterator) {
      this.pentityStream = pentityStream;
      this.iterator = iterator;
    }

    @Override
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    public void close() {
      try {
        pentityStream.close();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    @Override
    public Iterator iterator() {
      return iterator;
    }

    @Override
    public String toString() {
      return "CloseableIterableImpl{" + "iterator=" + iterator + '}';
    }

  }

}
