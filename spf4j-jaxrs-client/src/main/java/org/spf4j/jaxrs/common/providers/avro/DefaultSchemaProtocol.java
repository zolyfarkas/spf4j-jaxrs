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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.glassfish.jersey.internal.guava.Maps;

/**
 * Implements avro schema transmission over HTTP headers.
 * @author Zoltan Farkas
 */
public final class DefaultSchemaProtocol implements SchemaProtocol {

  public static final String CONTENT_TYPE_AVRO_SCHEMA_PARAM = "avsc";

  private final SchemaResolver client;

  @Inject
  public DefaultSchemaProtocol(final SchemaResolver client) {
    this.client = client;
  }

  @Override
  @Nullable
  public Schema deserialize(final MediaType mediaType,
          final Function<String, String> headers, final Class<?> type, final Type genericType) {
    String schemaStr = mediaType.getParameters().get(CONTENT_TYPE_AVRO_SCHEMA_PARAM);
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
    String type = acceptableMediaType.getType();
    Map<String, String> parameters = acceptableMediaType.getParameters();
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
            .putAll(parameters);
    if (!parameters.containsKey(CONTENT_TYPE_AVRO_SCHEMA_PARAM)) {
      builder.put(CONTENT_TYPE_AVRO_SCHEMA_PARAM, schemaToString(schema));
    }
    if ("text".equals(type)
            && !parameters.containsKey(MediaType.CHARSET_PARAMETER)) {
      // all texts are defaulted to utf8
      builder.put(MediaType.CHARSET_PARAMETER, "utf-8");
    }
    headers.accept(HttpHeaders.CONTENT_TYPE,
            new MediaType(type, acceptableMediaType.getSubtype(), builder.build()).toString());

  }

  @VisibleForTesting
  String schemaToString(final Schema schema) {
    try {
      StringWriter sw = new StringWriter();
      JsonGenerator jgen = Schema.FACTORY.createGenerator(sw);
      jgen = new FilteringGeneratorDelegate(jgen, NonSerPropertyFilter.INSTANCE, true, true);
      schema.toJson(new AvroNamesRefResolver(client), jgen);
      jgen.flush();
      return sw.toString();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public Schema getAcceptableSchema(final MediaType acceptedMediaType) {
    String schemaStr = acceptedMediaType.getParameters().get(CONTENT_TYPE_AVRO_SCHEMA_PARAM);
    if (schemaStr == null) {
      return null;
    }
    Schema.Parser parser = new Schema.Parser(new AvroNamesRefResolver(client));
    parser.setValidate(false);
    try {
      return parser.parse(schemaStr);
    } catch (RuntimeException ex) {
      throw new ClientErrorException("Unable to parse schema: " + schemaStr, 400, ex);
    }
  }

  @Override
  public MediaType acceptable(final MediaType mediaType, final Schema schema) {
    Map<String, String> xp = mediaType.getParameters();
    if (xp.containsKey(CONTENT_TYPE_AVRO_SCHEMA_PARAM)) {
      return mediaType;
    }
    Map<String, String> parameters = Maps.newHashMapWithExpectedSize(xp.size() + 1);
    parameters.putAll(xp);
    parameters.put(CONTENT_TYPE_AVRO_SCHEMA_PARAM, schemaToString(schema));
    return new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters);
  }

  @Override
  public String toString() {
    return "DefaultSchemaProtocol{" + "client=" + client + '}';
  }


}
