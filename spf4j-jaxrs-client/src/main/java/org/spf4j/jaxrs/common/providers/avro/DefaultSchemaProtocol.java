/*
 * Copyright 2019 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.jaxrs.common.providers.avro;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.spf4j.base.Json;
import org.spf4j.http.Headers;

/**
 * Implements avro schema transmission over HTTP headers.
 * @author Zoltan Farkas
 */
public final class DefaultSchemaProtocol implements SchemaProtocol {

  public static final String CONTENT_TYPE_AVRO_SCHEMA_PARAM = "avsc";

  private final SchemaResolver client;

  public DefaultSchemaProtocol(final SchemaResolver client) {
    this.client = client;
  }

  @Override
  @Nullable
  public Schema deserialize(final MediaType mediaType,
          final Function<String, String> headers, final Class<?> type, final Type genericType) {
    String schemaStr = mediaType.getParameters().get(CONTENT_TYPE_AVRO_SCHEMA_PARAM);
    if (schemaStr == null) {
      schemaStr = headers.apply(Headers.CONTENT_SCHEMA);
    }
    if (schemaStr == null) {
      return null;
    } else {
      Schema.Parser parser = new Schema.Parser(new AvroNamesRefResolver(client));
      parser.setValidate(false);
      return parser.parse(schemaStr);
    }
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void serialize(final MediaType acceptableMediaType,
          final BiConsumer<String, String> headers, final Schema schema) {
    try {
      StringWriter sw = new StringWriter();
      JsonGenerator jgen = Json.FACTORY.createGenerator(sw);
      jgen = new FilteringGeneratorDelegate(jgen, NonSerPropertyFilter.INSTANCE, true, true);
      schema.toJson(new AvroNamesRefResolver(client), jgen);
      jgen.flush();
      headers.accept(HttpHeaders.CONTENT_TYPE, new MediaType(acceptableMediaType.getType(),
              acceptableMediaType.getSubtype(),
              ImmutableMap.of(CONTENT_TYPE_AVRO_SCHEMA_PARAM, sw.toString())).toString());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String toString() {
    return "DefaultSchemaProtocol{" + "client=" + client + '}';
  }

  @Override
  public Schema getAcceptableSchema(final MediaType acceptedMediaType) {
    String schemaStr = acceptedMediaType.getParameters().get(CONTENT_TYPE_AVRO_SCHEMA_PARAM);
    if (schemaStr == null) {
      return null;
    }
    Schema.Parser parser = new Schema.Parser(new AvroNamesRefResolver(client));
    parser.setValidate(false);
    return parser.parse(schemaStr);
  }

}
