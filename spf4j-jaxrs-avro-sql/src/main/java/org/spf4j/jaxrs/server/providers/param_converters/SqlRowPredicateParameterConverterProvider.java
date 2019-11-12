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
package org.spf4j.jaxrs.server.providers.param_converters;

import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.avro.calcite.SqlRowPredicate;

/**
 * @author Zoltan Farkas
 */
@Provider
public final class SqlRowPredicateParameterConverterProvider implements ParamConverterProvider {



  @Override
  @Nullable
  public <T> ParamConverter<T> getConverter(final Class<T> clasz, final Type type, final Annotation[] annotations) {
    if (SqlPredicate.class.isAssignableFrom(clasz) && type instanceof ParameterizedType) {
      Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
      TypeToken<?> tt = TypeToken.of(typeArgument);
      if (tt.isSubtypeOf(SpecificRecord.class)) {
        Schema rowSchema;
        try {
          rowSchema = ((SpecificRecord) tt.getRawType().getConstructor().newInstance()).getSchema();
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
          throw new IllegalStateException("Invalid implementation of SpecificRecord: " + tt, ex);
        }
        return (ParamConverter<T>) new ParamConverterImpl(rowSchema);
      }
    }
    return null;
  }

  private static class ParamConverterImpl implements ParamConverter<SqlPredicate<? extends IndexedRecord>> {

    private final Schema rowSchema;

    ParamConverterImpl(final Schema rowSchema) {
      this.rowSchema = rowSchema;
    }

    @Override
    @Nullable
    public SqlPredicate<? extends IndexedRecord> fromString(@Nullable final String value) {
      if (value == null) {
        return null;
      }
      try {
        return new SqlRowPredicate(value, rowSchema);
      } catch (SqlParseException | ValidationException | RelConversionException ex) {
        throw new ClientErrorException("Invalid sql expression: " + value, 400, ex);
      }
    }

    @Override
    public String toString(final SqlPredicate<? extends IndexedRecord> value) {
      return value.getSqlString();
    }
  }

}
