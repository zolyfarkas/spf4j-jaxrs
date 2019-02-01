package org.spf4j.jaxrs.common.avro.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;
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
public abstract class AvroArrayMessageBodyReader implements MessageBodyReader<Iterable> {

  private final SchemaResolver client;

  @Inject
  public AvroArrayMessageBodyReader(final SchemaResolver client) {
    this.client = client;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return Iterable.class == type;
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
    DatumReader reader = new ReflectDatumReader(writerSchema.getElementType(), readerSchema.getElementType());
    InputStream entityStream = wrapInputStream(pentityStream);
    Decoder decoder = getDecoder(writerSchema, entityStream);
    return new CloseableIterableImpl(pentityStream, decoder, reader);
  }


  private static class ArrayIterator implements Iterator {

    private final Decoder decoder;
    private final DatumReader reader;
    private long l;
    private long i = 0;
    private boolean hasNext;

    ArrayIterator(final Decoder decoder, final DatumReader reader) {
      this.decoder = decoder;
      this.reader = reader;
      try {
        l = decoder.readArrayStart();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      if (l == 0) {
        hasNext = false;
      } else {
        hasNext = true;
      }
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public Object next() {
      try {
        if (!hasNext) {
          throw new NoSuchElementException();
        }
        Object read = reader.read(null, decoder);
        i++;
        if (i >= l) {
          l = decoder.arrayNext();
          i = 0;
          if (l == 0) {
            hasNext = false;
          }
        }
        return read;
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  private static class CloseableIterableImpl implements CloseableIterable {

    private final InputStream pentityStream;
    private final Decoder decoder;
    private final DatumReader reader;

    CloseableIterableImpl(final InputStream pentityStream, final Decoder decoder, final DatumReader reader) {
      this.pentityStream = pentityStream;
      this.decoder = decoder;
      this.reader = reader;
    }

    @Override
    public void close() throws IOException {
      pentityStream.close();
    }

    @Override
    public Iterator iterator() {
      return new ArrayIterator(decoder, reader);
    }
  }

}
