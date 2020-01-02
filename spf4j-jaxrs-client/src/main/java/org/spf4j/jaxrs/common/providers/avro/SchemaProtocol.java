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
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import org.apache.avro.Schema;

/**
 * Abstraction of Request Content Schema transmission over HTTP(or other protocol) Headers.
 * @author Zoltan Farkas
 */
public interface SchemaProtocol {

  SchemaProtocol NONE = new SchemaProtocol() {
    @Override
    public Schema deserialize(final MediaType mediaType, final Function<String, String> headers,
            final Class<?> type, final Type genericType) {
      return null;
    }

    @Override
    public void serialize(final MediaType mediaType,
            final BiConsumer<String, String> headers, final Schema schema) {
      //NOTHING
    }
  };


  /**
   * De-serialize the schema transmitted via HTTP headers.
   * @param headers the headers.
   * @param type - the type the schema will be converted to.
   * @param genericType - the generic type the schema will be converted to.
   * @return the schema or null if schema not transmitted with this protocol.
   */
  @Nullable
  Schema deserialize(MediaType mediaType, Function<String, String> headers, Class<?> type, Type genericType);


  /**
   * Serialize a deadline to HTTP headers.
   * @param headers
   * @param schema the schema to encode.
   */
  void serialize(MediaType acceptedMediaType, BiConsumer<String, String> headers, Schema schema);


  default SchemaProtocol combine(final SchemaProtocol secondary) {
    return new CombinedSchemaProtocol(this, secondary);
  }


}
