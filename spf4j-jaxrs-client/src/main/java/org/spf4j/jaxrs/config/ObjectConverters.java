/*
 * Copyright 2020 SPF4J.
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
package org.spf4j.jaxrs.config;

import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.CharSequenceReader;
import org.eclipse.microprofile.config.spi.Converter;
import org.spf4j.avro.Configs;
import org.spf4j.io.Csv;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;

/**
 * A converter utility that optionally Converts from Object.
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"URV_UNRELATED_RETURN_VALUES", "UP_UNUSED_PARAMETER"})
@ParametersAreNonnullByDefault
public final class ObjectConverters {

  private final CachingTypeMapWrapper<BiFunction<Object, Type, Object>> resolvers;

  private final SchemaResolver schemaResolver;

  public ObjectConverters(final Map<Type, SortedMap<Integer, Converter<?>>> converters,
          final SchemaResolver schemaResolver) {
    this.schemaResolver = schemaResolver;
    resolvers = new CachingTypeMapWrapper<>(new GraphTypeMap());
    resolvers.safePut(CharSequence.class, this::resolveString);
    resolvers.safePut(Integer.class, this::resolveInt);
    resolvers.safePut(int.class, this::resolveInt);
    resolvers.safePut(Long.class, this::resolveLong);
    resolvers.safePut(long.class, this::resolveLong);
    resolvers.safePut(Double.class, this::resolveDouble);
    resolvers.safePut(double.class, this::resolveDouble);
    resolvers.safePut(Float.class, this::resolveFloat);
    resolvers.safePut(float.class, this::resolveFloat);
    resolvers.safePut(Boolean.class, this::resolveBoolean);
    resolvers.safePut(boolean.class, this::resolveBoolean);
    resolvers.safePut(BigInteger.class, this::resolveBigInteger);
    resolvers.safePut(BigDecimal.class, this::resolveBigDecimal);
    resolvers.safePut(Duration.class, this::resolveDuration);
    resolvers.safePut(SpecificRecord.class, this::resolveSpecificRecord);
    resolvers.safePut(Object.class, this::resolveAny);
    for (Map.Entry<Type, SortedMap<Integer, Converter<?>>> entry : converters.entrySet()) {
      Type type = entry.getKey();
      for (Converter<?> converter : entry.getValue().values()) {
        resolvers.safePut(type, (v, t) -> converter.convert(v.toString()));
      }
    }
  }

  private ObjectConverters(final CachingTypeMapWrapper<BiFunction<Object, Type, Object>> resolvers,
          final SchemaResolver schemaResolver) {
    this.resolvers = resolvers;
    this.schemaResolver = schemaResolver;
  }

  public ObjectConverters withNewSchemaResolver(final SchemaResolver nschemaResolver) {
    return new ObjectConverters(this.resolvers, nschemaResolver);
  }

  public BiFunction<Object, Type, Object> get(final Type clasz) {
    return resolvers.get(clasz);
  }

  @Nullable
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")// when isArray true component type is not null
  private Object resolveAny(final Object val, final Type type) {
    Class<? extends Object> valClass = val.getClass();
    TypeToken<?> tt = TypeToken.of(type);
    if (tt.isArray()) {
      if (valClass.isArray()) {
        TypeToken<?> tComponentType = tt.getComponentType();
        if (valClass.getComponentType() == tComponentType.getRawType()) {
          return val;
        } else {
          Type ct = tComponentType.getType();
          BiFunction<Object, Type, Object> compResolver = resolvers.get(ct);
          if (compResolver == null) {
            throw new IllegalArgumentException("No resolver of array component " + ct);
          }
          int length = Array.getLength(val);
          Object result = Array.newInstance(tComponentType.getRawType(), length);
          for (int i = 0; i < length; i++) {
            Array.set(result, i, compResolver.apply(Array.get(val, i), ct));
          }
          return result;
        }
      } else if (val instanceof String) {
        TypeToken<?> tComponentType = tt.getComponentType();
        Type ct = tComponentType.getType();
        BiFunction<Object, Type, Object> compResolver = resolvers.get(ct);
        if (compResolver == null) {
          throw new IllegalArgumentException("No resolver for " + ct);
        }
        List result = new ArrayList();
        for (CharSequence elem : Csv.CSV.singleRow(new StringReader((String) val))) {
          result.add(compResolver.apply(elem, ct));
        }
        int length = result.size();
        Object array = Array.newInstance(tComponentType.getRawType(), length);
        for (int i = 0; i < length; i++) {
          Array.set(array, i, result.get(i));
        }
        return array;
      }
      resolvers.get(type).apply(val, type);
    }
    if (valClass == type || tt.isSupertypeOf(valClass)) {
      return val;
    } else {
      try {
        return tt.getRawType().getConstructor(String.class).newInstance(val);
      } catch (NoSuchMethodException | SecurityException | InstantiationException
              | IllegalAccessException | InvocationTargetException ex) {
        throw new IllegalArgumentException("Unable to convert to " + type, ex);
      }
    }
  }

  @Nullable
  private Object resolveString(final Object val, final Type type) {
    return val.toString();
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveSpecificRecord(final Object val, final Type type) {
    if (val instanceof SpecificRecord) {
      return val;
    } else if (val instanceof CharSequence) {
      try {
        return Configs.read(
                (Class) type, new org.spf4j.avro.SchemaResolver() {
          @Nonnull
          public Schema resolveSchema(final String id) {
            return schemaResolver.resolveSchema(id);
          }

          @Nullable
          public String getId(final Schema schema) {
            return schemaResolver.getId(schema);
          }

        }, new CharSequenceReader((CharSequence) val));
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    } else {
      throw new IllegalArgumentException("Invalid configuration, cannot be converted to int: " + val);
    }
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveInt(final Object val, final Type type) {
    if (val instanceof Integer) {
      return val;
    } else if (val instanceof Number) {
      return ((Number) val).intValue();
    } else if (val instanceof CharSequence) {
      return Integer.valueOf(val.toString());
    } else {
      throw new IllegalArgumentException("Invalid configuration, cannot be converted to int: " + val);
    }
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveLong(final Object val, final Type type) {
    if (val instanceof Long) {
      return val;
    } else if (val instanceof Number) {
      return ((Number) val).longValue();
    } else if (val instanceof CharSequence) {
      return Long.valueOf(val.toString());
    } else {
      throw new IllegalArgumentException("Invalid configuration cannot be converted to long: " + val);
    }
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveDouble(final Object val, final Type type) {
    if (val instanceof Double) {
      return val;
    } else if (val instanceof Number) {
      return (((Number) val).doubleValue());
    } else if (val instanceof CharSequence) {
      return Double.valueOf(val.toString());
    } else {
      throw new IllegalArgumentException("Invalid configuration, cannot be converted to double: " + val);
    }
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveFloat(final Object val, final Type type) {
    if (val instanceof Float) {
      return val;
    } else if (val instanceof Number) {
      return (((Number) val).floatValue());
    } else if (val instanceof CharSequence) {
      return Float.valueOf(val.toString());
    } else {
      throw new IllegalArgumentException("Invalid configuration, cannot be converted to float: " + val);
    }
  }

  @Nullable
  private Object resolveBigDecimal(final Object val, final Type type) {
    if (val instanceof BigDecimal) {
      return val;
    }
    return new BigDecimal(val.toString());
  }

  @Nullable
  private Object resolveDuration(final Object val, final Type type) {
    if (val instanceof Duration) {
      return val;
    }
    return Duration.parse(val.toString());
  }

  @Nullable
  private Object resolveBigInteger(final Object val, final Type type) {
    if (val instanceof BigInteger) {
      return val;
    }
    return new BigInteger(val.toString());
  }

  @Nullable
  private Object resolveBoolean(final Object val, final Type type) {
    if (val instanceof Boolean) {
      return val;
    }
    return Boolean.valueOf(val.toString());
  }

  @Override
  public String toString() {
    return "ConfigurationResolver{ resolvers=" + resolvers + '}';
  }

}
