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

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.avro.Schema;

/**
 * @author Zoltan Farkas
 */
public interface SchemaProtocol {

  /**
   * De-serialize the schema transmitted via HTTP headers.
   * @param headers the headers.
   * @param type - the type the schema will be converted to.
   * @param genericType - the generic type the schema will be converted to.
   * @return the schema.
   */
  @Nullable
  Schema deserialize(Function<String, String> headers, Class<Object> type, Type genericType);


  /**
   * Serialize a deadline to HTTP headers.
   * @param headers
   * @param schema the schema to encode.
   */
  void serialize(BiConsumer<String, String> headers, Schema schema);


  SchemaProtocol NONE = new SchemaProtocol() {
    @Override
    public Schema deserialize(final Function<String, String> headers,
            final Class<Object> type, final Type genericType) {
      return null;
    }

    @Override
    public void serialize(final BiConsumer<String, String> headers, final Schema schema) {
      //NOTHING
    }
  };

}
