
package org.spf4j.jaxrs.common.providers.avro;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.spf4j.base.Json;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/json", "text/plain"})
public final class SchemaMessageBodyWriter implements MessageBodyWriter<Schema> {

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return Schema.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(final Schema t, final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType,
          final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
          throws IOException {
    JsonGenerator gen = Json.FACTORY.createGenerator(entityStream);
    t.toJson(gen);
    gen.flush();
  }


}
