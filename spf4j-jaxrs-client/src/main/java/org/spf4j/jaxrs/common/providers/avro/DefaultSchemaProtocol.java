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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.spf4j.avro.schema.CloningVisitor;
import org.spf4j.avro.schema.SchemaUtils;
import org.spf4j.avro.schema.Schemas;
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
  public Schema deserialize(final Function<String, String> headers, final Class<?> type, final Type genericType) {
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
    try {
      StringWriter sw = new StringWriter();
      JsonGenerator jgen = Json.FACTORY.createGenerator(sw);
      jgen = new FilteringGeneratorDelegate(jgen, NonSerPropertyFilter.INSTANCE, true, true);
      schema.toJson(new AvroNamesRefResolver(client), jgen);
      jgen.flush();
      headers.accept(Headers.CONTENT_SCHEMA, sw.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Nonnull
  @SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
  public static Schema stripNonSerializationAttrs(final Schema schema) {
    return Schemas.visit(schema, new CloningVisitor(SchemaUtils.FIELD_ESENTIALS,
            DefaultSchemaProtocol::copyLogicalTypeAndAliasses, false, schema));
  }

  private static void copyLogicalTypeAndAliasses(final Schema from, final Schema to) {
    LogicalType logicalType = from.getLogicalType();
    if (logicalType != null) {
      logicalType.addToSchema(to);
    }
    SchemaUtils.copyAliases(from, to);
    if (from.getType() == Schema.Type.ENUM) {
      SchemaUtils.copyProperties(from, to);
    }
    String id = from.getProp("mvnId");
    if (id != null) {
      to.addProp("mvnId", id);
    }
  }

  @Override
  public String toString() {
    return "DefaultSchemaProtocol{" + "client=" + client + '}';
  }

}
