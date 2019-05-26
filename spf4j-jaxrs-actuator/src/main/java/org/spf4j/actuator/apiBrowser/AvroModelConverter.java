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
package org.spf4j.actuator.apiBrowser;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.avro.reflect.ExtendedReflectData;

/**
 *
 * @author Zoltan Farkas
 */
public final class AvroModelConverter implements ModelConverter {

  public static final AvroModelConverter INSTANCE = new AvroModelConverter();

  private final ModelResolver jackson;

  private AvroModelConverter() {
    jackson = new ModelResolver(Json.mapper());
  }

  @Override
  public Schema resolve(final AnnotatedType atype, final ModelConverterContext context,
          final Iterator<ModelConverter> chain) {
    ExtendedReflectData reflector = ExtendedReflectData.get();
    Type type = atype.getType();
    Type actualType;
    if (type instanceof JavaType) {
      JavaType javaType = (JavaType) type;
      if (javaType.isArrayType() || javaType.isCollectionLikeType()) {
        actualType = java.lang.reflect.Array.newInstance(javaType.getContentType().getRawClass(), 0).getClass();
      } else {
        actualType = javaType.getRawClass();
      }
    } else {
      actualType = type;
    }
    org.apache.avro.Schema aSchema = reflector.getSchema(actualType);
    if (aSchema == null) { // fallback to jackson model introspector
      return jackson.resolve(atype, context, chain);
    }
    return resolve(aSchema, new IdentityHashMap<>(), context);
  }

  private static boolean isNullableUnion(final org.apache.avro.Schema aSchema) {
    for (org.apache.avro.Schema s : aSchema.getTypes()) {
      if (s.getType() == org.apache.avro.Schema.Type.NULL) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static org.apache.avro.Schema nullableSchema(final org.apache.avro.Schema aSchema) {
    List<org.apache.avro.Schema> types = aSchema.getTypes();
    if (types.size() != 2) {
      return null;
    }
    if (!isNullableUnion(aSchema)) {
      return null;
    }
    for (org.apache.avro.Schema s : types) {
      if (s.getType() != org.apache.avro.Schema.Type.NULL) {
        return s;
      }
    }
    return null;
  }

  public Schema resolve(final org.apache.avro.Schema aSchema,
          final Map<org.apache.avro.Schema, Schema> resolved, final ModelConverterContext context) {
    Schema get = resolved.get(aSchema);
    if (get != null) {
      return new Schema().$ref(aSchema.getFullName());
    }
    Schema result;
    switch (aSchema.getType()) {
         case ARRAY:
           result = new ArraySchema().items(resolve(aSchema.getElementType(), resolved, context));
           break;
         case BOOLEAN:
           result = PrimitiveType.BOOLEAN.createProperty();
           break;
         case BYTES:
           result = new BinarySchema();
           break;
         case DOUBLE:
           result = PrimitiveType.DOUBLE.createProperty();
           break;
         case ENUM:
           result = PrimitiveType.STRING.createProperty();
           result.setEnum(aSchema.getEnumStringSymbols());
           break;
         case FIXED:
           result = new BinarySchema();
           break;
         case FLOAT:
           result = PrimitiveType.FLOAT.createProperty();
           break;
         case INT:
           result = PrimitiveType.INT.createProperty();
           break;
         case LONG:
           result = PrimitiveType.LONG.createProperty();
           break;
         case STRING:
           result = PrimitiveType.STRING.createProperty();
           break;
         case NULL:
           result = PrimitiveType.STRING.createProperty().format("").nullable(Boolean.TRUE);
           break;
         case UNION:
           org.apache.avro.Schema nullableSchema = nullableSchema(aSchema);
           if (nullableSchema != null) {
             result = resolve(nullableSchema, resolved, context);
             result = result.nullable(Boolean.TRUE);
           } else {
            ComposedSchema cs = new ComposedSchema();
            for (org.apache.avro.Schema ss : aSchema.getTypes()) {
              if (ss.getType() == org.apache.avro.Schema.Type.NULL) {
                cs.nullable(Boolean.TRUE);
              } else {
                cs.addAnyOfItem(resolve(ss, resolved, context));
              }
            }
            result = cs;
           }
           break;

         case RECORD:
           result = new ObjectSchema();
           resolved.put(aSchema, result);
           context.defineModel(aSchema.getFullName(), result);
           for (org.apache.avro.Schema.Field field : aSchema.getFields()) {
             Schema fs = resolve(field.schema(), resolved, context);
             fs = fs.description(field.doc() + "; " + fs.getDescription());
             result.addProperties(field.name(), fs);
           }
           break;
         case MAP:
           result = new MapSchema();
           result.addExtension("avsc", aSchema);
           break;
         default:
           throw new UnsupportedOperationException("Unsupported schema type " + aSchema);
       }
    return result.description(aSchema.getDoc());
  }

}
