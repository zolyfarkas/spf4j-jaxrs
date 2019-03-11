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
package org.spf4j.jaxrs.common.avro;

import com.fasterxml.jackson.core.JsonGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
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

  private final SchemaResolver client;

  public DefaultSchemaProtocol(final SchemaResolver client) {
    this.client = client;
  }

  @Override
  @Nullable
  public Schema deserialize(final Function<String, String> headers, final Class<Object> type, final Type genericType) {
    String schemaRefStr = headers.apply(Headers.CONTENT_SCHEMA);
    if (schemaRefStr == null) {
      return null;
    } else {
      return new Schema.Parser(new AvroNamesRefResolver(client)).parse(schemaRefStr);
    }
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void serialize(final BiConsumer<String, String> headers, final Schema schema) {
    String id = schema.getProp("mvnId");
    if (id  == null || id.contains("SNAPSHOT")) {
      headers.accept(Headers.CONTENT_SCHEMA, schema.toString());
    } else {
      try {
        StringWriter sw = new StringWriter();
        JsonGenerator jgen = Json.FACTORY.createGenerator(sw);
        schema.toJson(new AvroNamesRefResolver(client), jgen);
        jgen.flush();
        headers.accept(Headers.CONTENT_SCHEMA, sw.toString());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  @Override
  public String toString() {
    return "DefaultSchemaProtocol{" + "client=" + client + '}';
  }



}
