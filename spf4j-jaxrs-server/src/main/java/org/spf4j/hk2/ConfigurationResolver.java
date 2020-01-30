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
package org.spf4j.hk2;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.ws.rs.core.Configuration;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
public final class ConfigurationResolver {

  private final Configuration configuration;

  private final Map<Type, Function<ConfigurationParam, Object>> resolvers;

  public ConfigurationResolver(final Configuration configuration) {
    this.configuration = configuration;
     resolvers = ImmutableMap.<Type, Function<ConfigurationParam, Object>>builder()
            .put(CharSequence.class, this::resolveString)
            .put(String.class, this::resolveString)
            .put(Integer.class, this::resolveInt)
            .put(int.class, this::resolveInt)
            .put(Long.class, this::resolveLong)
            .put(long.class, this::resolveLong)
            .put(Double.class, this::resolveDouble)
            .put(double.class, this::resolveDouble)
            .put(Float.class, this::resolveFloat)
            .put(float.class, this::resolveFloat)
            .put(Boolean.class, this::resolveBoolean)
            .put(boolean.class, this::resolveBoolean)
            .put(BigInteger.class, this::resolveBigInteger)
            .put(BigDecimal.class, this::resolveBigDecimal)
            .put(Duration.class, this::resolveDuration).build();
  }

  public Function<ConfigurationParam, Object> get(final Type clasz) {
    Function<ConfigurationParam, Object> result = resolvers.get(clasz);
    if (result == null) {
      return (cp) -> resolveAny(cp, clasz);
    } else {
      return result;
    }
  }


  @Nullable
  private Object resolveAny(final ConfigurationParam param, final Type clasz) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        try {
          return TypeToken.of(clasz).getRawType().getConstructor(String.class).newInstance(param.getDefaultValue());
        } catch (NoSuchMethodException | SecurityException | InstantiationException
                | IllegalAccessException | InvocationTargetException ex) {
          throw new IllegalArgumentException("Unable to convert " + param.getDefaultValue() + " to " + clasz, ex);
        }
      }
      Class<? extends Object> valClass = val.getClass();
      if (valClass == clasz || TypeToken.of(clasz).isSupertypeOf(valClass)) {
        return val;
      } else {
        throw new IllegalArgumentException("Invalid paran " + val + ", expected:  " + clasz);
      }
    }
    return null;
  }

 private String getConfig(final String param, @Nullable final String defaultValue) {
    Object result = configuration.getProperty(param);
    if (result == null) {
      return defaultValue;
    } else {
      return result.toString();
    }
  }

  @Nullable
  private Object resolveString(final ConfigurationParam param) {
    if (param != null) {
      return getConfig(param.getPropertyName(), param.getDefaultValue());
    }
    return null;
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveInt(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return Integer.valueOf(param.getDefaultValue());
      }
      if (val instanceof Integer) {
        return val;
      } else if (val instanceof Number) {
        return ((Number) val).intValue();
      } else if (val instanceof String) {
        return Integer.valueOf((String) val);
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to int: " + val);
      }
    }
    return null;
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveLong(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return Long.valueOf(param.getDefaultValue());
      }
      if (val instanceof Long) {
        return val;
      } else if (val instanceof Number) {
        return ((Number) val).longValue();
      } else if (val instanceof String) {
        return Long.valueOf((String) val);
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to long: " + val);
      }
    }
    return null;
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveDouble(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return Double.valueOf(param.getDefaultValue());
      }
      if (val instanceof Double) {
        return val;
      } else if (val instanceof Number) {
        return (((Number) val).doubleValue());
      } else if (val instanceof String) {
        return Double.valueOf((String) val);
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to double: " + val);
      }
    }
    return null;
  }

  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  private Object resolveFloat(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return Float.valueOf(param.getDefaultValue());
      }
      if (val instanceof Float) {
        return val;
      } else if (val instanceof Number) {
        return (((Number) val).floatValue());
      } else if (val instanceof String) {
        return Float.valueOf((String) val);
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to float: " + val);
      }
    }
    return null;
  }

  @Nullable
  private Object resolveBigDecimal(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return new BigDecimal(param.getDefaultValue());
      }
      if (val instanceof BigDecimal) {
        return val;
      }
      return new BigDecimal(val.toString());
    }
    return null;
  }

  @Nullable
  private Object resolveDuration(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return Duration.parse(param.getDefaultValue());
      }
      if (val instanceof Duration) {
        return val;
      }
      return Duration.parse(val.toString());
    }
    return null;
  }


  @Nullable
  private Object resolveBigInteger(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return new BigInteger(param.getDefaultValue());
      }
      if (val instanceof BigInteger) {
        return val;
      }
      return new BigInteger(val.toString());
    }
    return null;
  }

  @Nullable
  private Object resolveBoolean(final ConfigurationParam param) {
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        return Boolean.valueOf(param.getDefaultValue());
      }
      if (val instanceof Boolean) {
        return val;
      }
      return Boolean.valueOf(val.toString());
    }
    return null;
  }

  @Override
  public String toString() {
    return "ConfigurationResolver{" + "configuration=" + configuration + ", resolvers=" + resolvers + '}';
  }

}
