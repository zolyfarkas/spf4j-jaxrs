
package org.spf4j.jaxrs.common.providers.avro.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.apache.avro.AvroArrayWriter;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.Arrays;
import org.spf4j.base.avro.AvroContainer;
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
    Schema actualSchema;
    Schema elemSchema;
    Schema schemaFromAnnotations = MessageBodyRWUtils.getSchemaFromAnnotations(annotations);
    if (schemaFromAnnotations != null) {
      actualSchema = schemaFromAnnotations;
      elemSchema = schemaFromAnnotations.getElementType();
    } else {
      elemSchema = t instanceof AvroContainer ? ((AvroContainer) t).getElementSchema() : null;
      if (elemSchema == null) {
        ParameterizedType genericType = MessageBodyRWUtils.toParameterizedType(Iterable.class, pgenericType);
        if (genericType == null) {
          if (t instanceof Collection) {
            Collection o = (Collection) t;
            if (o.isEmpty()) {
              elemSchema = Schema.create(Schema.Type.NULL);
              actualSchema = Schema.createArray(Schema.createArray(elemSchema));
            } else {
              Object elemVal = o.iterator().next();
              Class<? extends Object> aClass = elemVal.getClass();
              elemSchema = MessageBodyRWUtils.getAvroSchemaFromType(aClass, aClass, elemVal,
                      Arrays.EMPTY_ANNOT_ARRAY);
              actualSchema = Schema.createArray(elemSchema);
            }
          } else {
            throw new IllegalStateException("Cannot serialize " + t);
          }
        } else {
          Type actualTypeArgument = genericType.getActualTypeArguments()[0];
          elemSchema = MessageBodyRWUtils.getAvroSchemaFromType2(actualTypeArgument, annotations);
          if (elemSchema == null) {
             throw new IllegalStateException("Cannot serialize " + actualTypeArgument + ": " + t);
          }
          actualSchema = Schema.createArray(elemSchema);
        }
      } else {
        actualSchema = Schema.createArray(elemSchema);
      }
    }
    Schema acceptedSchema = protocol.getAcceptableSchema(mediaType);
    if (acceptedSchema == null) {
      writeDirect(actualSchema, mediaType, httpHeaders, elemSchema, entityStream, t);
    } else {
      writeProjected(acceptedSchema, mediaType, httpHeaders, entityStream, t, elemSchema);
    }

  }

  private void writeDirect(final Schema actualSchema, final MediaType mediaType,
          final MultivaluedMap<String, Object> httpHeaders, final Schema elemSchema,
          final OutputStream entityStream, final Iterable t)  {
    protocol.serialize(mediaType, httpHeaders::putSingle, actualSchema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(elemSchema);
      Encoder encoder = getEncoder(actualSchema, entityStream);
      int bufferSize;
      if (t instanceof Buffered) {
        bufferSize = ((Buffered) t).getElementBufferSize();
      } else {
        bufferSize = 64;
      }

      try (AvroArrayWriter arrWriter = new AvroArrayWriter(encoder, writer, bufferSize)) {
        for (Object o : t) {
          arrWriter.write(o);
        }
      }
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + actualSchema.getName(), e);
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

  private  void writeProjected(final Schema acceptedSchema,
          final MediaType mediaType,
          final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream, final Iterable t, final Schema elemSchema) {
    protocol.serialize(mediaType, httpHeaders::putSingle, acceptedSchema);
    Schema respElemSchema = acceptedSchema.getElementType();
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(respElemSchema);
      Encoder encoder = getEncoder(acceptedSchema, entityStream);
      int bufferSize;
      if (t instanceof Buffered) {
        bufferSize = ((Buffered) t).getElementBufferSize();
      } else {
        bufferSize = 64;
      }
      try (AvroArrayWriter arrWriter = new AvroArrayWriter(encoder, writer, bufferSize)) {
        for (Object o : t) {
          arrWriter.write(Schemas.project(respElemSchema, elemSchema, o));
        }
      }
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + acceptedSchema.getName(), e);
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