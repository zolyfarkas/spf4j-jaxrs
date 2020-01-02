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

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.ws.rs.core.MediaType;
import org.apache.avro.Schema;

/**
 * A Schema protocol compounder to support multiple ways of transmitting a schema over HTTP headers.
 *
 * @author Zoltan Farkas
 */
final class CombinedSchemaProtocol implements SchemaProtocol {

  private final SchemaProtocol primary;

  private final SchemaProtocol secondary;

  CombinedSchemaProtocol(final SchemaProtocol primary, final SchemaProtocol secondary) {
    this.primary = primary;
    this.secondary = secondary;
  }

  @Override
  public Schema deserialize(final MediaType mediaType,
          final Function<String, String> headers, final Class<?> type, final Type genericType) {
    Schema schema = primary.deserialize(mediaType, headers, type, genericType);
    if (schema != null) {
      return schema;
    }
    return secondary.deserialize(mediaType, headers, type, genericType);
  }

  @Override
  public void serialize(final MediaType mediaType,
          final BiConsumer<String, String> headers, final Schema schema) {
    secondary.serialize(mediaType, headers, schema);
    primary.serialize(mediaType, headers, schema);
  }

  @Override
  public Schema getAcceptableSchema(final MediaType acceptedMediaType) {
    Schema acceptableSchema = primary.getAcceptableSchema(acceptedMediaType);
    if (acceptableSchema == null) {
      return secondary.getAcceptableSchema(acceptedMediaType);
    } else {
      return acceptableSchema;
    }
  }

  @Override
  public String toString() {
    return "CombinedSchemaProtocol{" + "primary=" + primary + ", secondary=" + secondary + '}';
  }

}
