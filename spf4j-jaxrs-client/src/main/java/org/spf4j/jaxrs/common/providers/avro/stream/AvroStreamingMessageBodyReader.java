
package org.spf4j.jaxrs.common.providers.avro.stream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.ArrayIterator;
import org.spf4j.avro.DecodedSchema;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.base.ArrayWriter;
import org.spf4j.jaxrs.common.providers.avro.SchemaProtocol;
import org.spf4j.jaxrs.StreamingArrayContent;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroStreamingMessageBodyReader implements MessageBodyReader<StreamingArrayContent<?>> {

  private final SchemaProtocol protocol;

  @Inject
  public AvroStreamingMessageBodyReader(final SchemaProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * @inheritdoc
   */
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType) {
    return StreamingArrayContent.class.isAssignableFrom(type);
  }

  public abstract Decoder getDecoder(Schema writerSchema, InputStream os)
          throws IOException;

  /** Overwrite for decoders that are capable of decoding schema from stream  */
  @Nullable
  public DecodedSchema tryDecodeSchema(@Nullable final Schema readerSchema,
          final InputStream is, final Annotation[] annotations)
          throws IOException {
    return null;
  }

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
   * Can be extended to add additional behavior.
   */
  @Override
  public StreamingArrayContent readFrom(final Class<StreamingArrayContent<?>> type,
          final Type pgenericType, final Annotation[] annotations, final MediaType mediaType,
          final MultivaluedMap<String, String> httpHeaders, final InputStream pentityStream)
          throws IOException {
    Schema writerSchema = protocol.deserialize(httpHeaders::getFirst, (Class) type, pgenericType);
    ParameterizedType genericType = MessageBodyRWUtils.toParameterizedType(pgenericType);
    Type elType = genericType.getActualTypeArguments()[0];
    Schema elemSchema = MessageBodyRWUtils.getAvroSchemaFromType(elType, annotations);
    Schema readerSchema = Schema.createArray(elemSchema);

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
    } else if (readerSchema != null) {
      writerSchema = readerSchema;
    } else {
      readerSchema = writerSchema;
    }
    if (decoder == null) {
      decoder = getDecoder(writerSchema, entityStream);
    }
    return new StreamingArrayOutputImpl(entityStream, decoder, readerSchema, writerSchema);
  }

  private static class StreamingArrayOutputImpl implements StreamingArrayContent {

    private final Decoder decoder;
    private final DatumReader reader;
    private final InputStream entityStream;


    StreamingArrayOutputImpl(final InputStream entityStream, final Decoder decoder,
            final Schema readerSchema, final Schema writerSchema) {
      this.entityStream = entityStream;
      this.decoder = decoder;
      this.reader = new ReflectDatumReader(writerSchema.getElementType(), readerSchema.getElementType());
    }

    @Override
    public void write(final ArrayWriter output) throws IOException {
      try (ArrayWriter wr = output; InputStream is = entityStream;) {
        ArrayIterator arrayIterator = new ArrayIterator(decoder, reader);
        while (arrayIterator.hasNext()) {
          wr.accept(arrayIterator.next());
        }
      }
    }

    @Override
    public void close() throws IOException {
      entityStream.close();
    }

  }



}