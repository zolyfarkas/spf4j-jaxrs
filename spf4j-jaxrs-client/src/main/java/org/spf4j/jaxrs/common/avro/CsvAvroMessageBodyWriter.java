package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.Encoder;
import org.spf4j.avro.csv.CsvEncoder;
import org.spf4j.io.Csv;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"text/csv;fmt=avro"})
public final class CsvAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  @Inject
  public CsvAvroMessageBodyWriter(final SchemaResolver client) {
    super(client);
  }

  @Override
  public Encoder getEncoder(final Schema writerSchema, final OutputStream os) throws IOException {
    return new CsvEncoder(Csv.CSV.writer(new OutputStreamWriter(os, StandardCharsets.UTF_8)), writerSchema);
  }

  @Override
  public void writeTo(final Object t, final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream)
          throws IOException {
    httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, "text/csv;fmt=avro,charset=UTF-8");
    super.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
  }

}
