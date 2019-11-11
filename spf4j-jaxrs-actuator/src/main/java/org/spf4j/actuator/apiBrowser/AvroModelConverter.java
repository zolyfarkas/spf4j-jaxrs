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
import org.apache.avro.LogicalType;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.reflect.ExtendedReflectData;
import org.spf4j.avro.schema.Schemas;

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
    Type type = atype.getType();
    Type actualType;
    if (type instanceof JavaType) {
      JavaType javaType = (JavaType) type;
      if (javaType.isArrayType() || javaType.isCollectionLikeType()) {
        actualType = java.lang.reflect.Array.newInstance(javaType.getContentType().getRawClass(), 0).getClass();
      } else if (javaType.isTypeOrSubTypeOf(Iterable.class) && javaType.hasGenericTypes()) {
        JavaType containedType = javaType.containedType(0);
        Class<?> rawContainedClass = containedType.getRawClass();
        if (rawContainedClass == GenericRecord.class || rawContainedClass == IndexedRecord.class) {
          rawContainedClass = Object.class;
        }
        actualType = java.lang.reflect.Array.newInstance(rawContainedClass, 0).getClass();
      } else {
        actualType = javaType;
      }
    } else {
      actualType = type;
    }
    org.apache.avro.Schema aSchema = ExtendedReflectData.get().getSchema(actualType);
    if (aSchema == null) { // fallback to jackson model introspector
      return jackson.resolve(atype, context, chain);
    }
    return resolve(aSchema, new IdentityHashMap<>(), context);
  }

  @Nullable
  private static org.apache.avro.Schema nullableSchema(final org.apache.avro.Schema aSchema) {
    List<org.apache.avro.Schema> types = aSchema.getTypes();
    if (types.size() != 2) {
      return null;
    }
    if (!Schemas.isNullableUnion(aSchema)) {
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
      return new Schema().$ref("#/components/schemas/" + aSchema.getFullName());
    }
    LogicalType logicalType = aSchema.getLogicalType();
    Schema result;
    switch (aSchema.getType()) {
         case ARRAY:
           result = new ArraySchema().items(resolve(aSchema.getElementType(), resolved, context));
           result.nullable(Boolean.FALSE);
           break;
         case BOOLEAN:
           result = PrimitiveType.BOOLEAN.createProperty().nullable(Boolean.FALSE);
           break;
         case BYTES:
           result = new BinarySchema().nullable(Boolean.FALSE);
           break;
         case DOUBLE:
           result = PrimitiveType.DOUBLE.createProperty().nullable(Boolean.FALSE);
           break;
         case ENUM:
           result = PrimitiveType.STRING.createProperty().nullable(Boolean.FALSE);
           result.setEnum(aSchema.getEnumStringSymbols());
           break;
         case FIXED:
           result = new BinarySchema().nullable(Boolean.FALSE);
           break;
         case FLOAT:
           result = PrimitiveType.FLOAT.createProperty().nullable(Boolean.FALSE);
           break;
         case INT:
           result = PrimitiveType.INT.createProperty().nullable(Boolean.FALSE);
           break;
         case LONG:
           result = PrimitiveType.LONG.createProperty().nullable(Boolean.FALSE);
           break;
         case STRING:
            if (logicalType != null) {
              switch (logicalType.getName()) {
                case "date":
                  result = PrimitiveType.DATE.createProperty().nullable(Boolean.FALSE);
                  break;
                case "instant":
                  result = PrimitiveType.DATE_TIME.createProperty().nullable(Boolean.FALSE);
                  break;
                case "url":
                  result = PrimitiveType.URL.createProperty().nullable(Boolean.FALSE);
                  break;
                case "uri":
                  result = PrimitiveType.URI.createProperty().nullable(Boolean.FALSE);
                  break;
                case "uuid":
                  result = PrimitiveType.UUID.createProperty().nullable(Boolean.FALSE);
                  break;
                default:
                  result = PrimitiveType.STRING.createProperty().nullable(Boolean.FALSE);
              }
            } else {
              result = PrimitiveType.STRING.createProperty().nullable(Boolean.FALSE);
            }
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
           result.nullable(Boolean.FALSE);
           break;
         case MAP:
           result = new MapSchema();
           result.addExtension("avsc", aSchema);
           result.nullable(Boolean.FALSE);
           break;
         default:
           throw new UnsupportedOperationException("Unsupported schema type " + aSchema);
       }
    if (aSchema.getProp("deprecated") != null) {
      result.deprecated(Boolean.TRUE);
    }
    return result.description((aSchema.getProp("beta") != null ? "Beta model; " : "") + aSchema.getDoc());
  }

}
