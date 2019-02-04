package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.reflect.ReflectData;
import org.codehaus.jackson.JsonGenerator;
import org.spf4j.base.Json;

/**
 * @author Zoltan Farkas
 */
@Provider
public final class AvroParameterConverterProvider implements ParamConverterProvider {


  private final ParamConverter<Schema> schemaConv;

  @Inject
  public AvroParameterConverterProvider(final SchemaResolver client) {
    this.schemaConv = new SchemaParamConverter(client);
  }

  @Override
  @Nullable
  public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType,
          final Annotation[] annotations) {
    if (rawType == Schema.class) {
      return (ParamConverter<T>) schemaConv;
    } else if (rawType == Class.class)  {
      return (ParamConverter<T>) new ClassParamConverter();
    }
    return null;
  }

  private static class SchemaParamConverter implements ParamConverter<Schema> {

    private final SchemaResolver client;

    SchemaParamConverter(final SchemaResolver client) {
      this.client = client;
    }

    @Override
    public Schema fromString(final String value) {
      try {
        return new Schema.Parser(new AvroNamesRefResolver(client)).parse(value);
      } catch (RuntimeException ex) {
        throw new IllegalArgumentException("Invalid schema " + value, ex);
      }
    }

    @Override
    public String toString(final Schema schema) {
      try {
        StringWriter sw = new StringWriter();
        JsonGenerator jgen = Json.FACTORY.createJsonGenerator(sw);
        schema.toJson(new AvroNamesRefResolver(client), jgen);
        jgen.flush();
        return sw.toString();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  private class ClassParamConverter implements ParamConverter<Type> {


    @Override
    public Type fromString(final String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString(final Type value) {
      Schema schema = ReflectData.get().getSchema(value);
      return schemaConv.toString(schema);
    }
  }

  @Override
  public String toString() {
    return "AvroParameterConverterProvider{" + "schemaConv=" + schemaConv + '}';
  }

}
