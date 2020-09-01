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
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.core.MediaType;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.ExtendedJsonDecoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.eclipse.microprofile.config.spi.Converter;
import org.spf4j.base.CharSequences;
import org.spf4j.io.Csv;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;

/**
 * A converter utility that optionally Converts from Object.
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({ "URV_UNRELATED_RETURN_VALUES", "UP_UNUSED_PARAMETER" })
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

  private static class ParsedHeader {

    private final MediaType mediaType;
    private final int startContent;

    ParsedHeader(final MediaType mediaType, final int startContent) {
      this.mediaType = mediaType;
      this.startContent = startContent;
    }

    public MediaType getMediaType() {
      return mediaType;
    }

    public int getStartContent() {
      return startContent;
    }

  }


  private static ParsedHeader getConfigMediaType(final CharSequence seq) {
    int length = seq.length();
    int indexOf = CharSequences.indexOf(seq, 0, length, ':');
    int next = indexOf + 1;
    int endMt = CharSequences.indexOf(seq, next, length, '\n');
    return new ParsedHeader(MediaType.valueOf(seq.subSequence(next,  endMt).toString()), endMt + 1);
  }


  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveSpecificRecord(final Object val, final Type type) {
    if (val instanceof SpecificRecord) {
      return val;
    } else if (val instanceof CharSequence) {
      CharSequence cs = (CharSequence) val;
      SpecificData sd = SpecificData.get();
      Schema rSchema = sd.getSchema(type);
      Schema wSchema;
      int contentIdx;
      if (CharSequences.startsWith(cs, "#", 0)) {
        ParsedHeader mt = getConfigMediaType(cs);
        contentIdx = mt.getStartContent();
        Schema.Parser parser = new Schema.Parser(new AvroNamesRefResolver(schemaResolver));
        parser.setValidate(false);
        String schemaStr = mt.getMediaType()
                .getParameters().get(DefaultSchemaProtocol.CONTENT_TYPE_AVRO_SCHEMA_PARAM);
        if (schemaStr != null) {
          wSchema = parser.parse(schemaStr);
        } else {
          wSchema = rSchema;
        }
      } else {
        contentIdx = 0;
        wSchema = rSchema;
      }
      DatumReader<?> reader = new SpecificDatumReader<>(wSchema, rSchema, sd);
      try {
        Decoder decoder = new ExtendedJsonDecoder(wSchema,
                Schema.FACTORY.createParser(CharSequences.reader(cs.subSequence(contentIdx, cs.length()))), true);
        return reader.read(null, decoder);
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
