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
package org.spf4j.hk2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;
import org.spf4j.jaxrs.ConfigProperty;
import org.spf4j.jaxrs.SystemConfiguration;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"URV_UNRELATED_RETURN_VALUES", "UP_UNUSED_PARAMETER"})
public final class ConfigurationInjector implements InjectionResolver<ConfigProperty> {

  private final Configuration configuration;

  private final CachingTypeMapWrapper<BiFunction<Injectee, ServiceHandle<?>, Object>> typeResolvers;

  @Inject
  public ConfigurationInjector(@Context final Configuration configuration) {
    this.configuration = new SystemConfiguration(configuration);
    this.typeResolvers = new CachingTypeMapWrapper<>(new GraphTypeMap());
    this.typeResolvers.safePut(CharSequence.class, this::resolveString)
            .safePut(Integer.class, this::resolveInt)
            .safePut(int.class, this::resolveInt)
            .safePut(Long.class, this::resolveLong)
            .safePut(long.class, this::resolveLong)
            .safePut(Double.class, this::resolveDouble)
            .safePut(double.class, this::resolveDouble)
            .safePut(Float.class, this::resolveFloat)
            .safePut(float.class, this::resolveFloat)
            .safePut(Boolean.class, this::resolveBoolean)
            .safePut(boolean.class, this::resolveBoolean)
            .safePut(BigInteger.class, this::resolveBigInteger)
            .safePut(BigDecimal.class, this::resolveBigDecimal);
  }

  String getConfig(final String param, @Nullable final String defaultValue) {
    Object result = configuration.getProperty(param);
    if (result == null) {
      return defaultValue;
    } else {
      return result.toString();
    }
  }


  @Nullable
  private Object resolveString(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      return getConfig(param.getPropertyName(), param.getDefaultValue());
    }
    return null;
  }

  @Nullable
  private Object resolveInt(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof Integer) {
        return val;
      } else if (val instanceof Number) {
        return ((Number) val).intValue();
      } else if (val instanceof String) {
        return Integer.valueOf((String) val);
      } else if (val == null) {
        return null;
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to int: " + val);
      }
    }
    return null;
  }

  @Nullable
  private Object resolveLong(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof Long) {
        return val;
      } else if (val instanceof Number) {
        return ((Number) val).longValue();
      } else if (val instanceof String) {
        return Long.valueOf((String) val);
      } else if (val == null) {
        return null;
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to long: " + val);
      }
    }
    return null;
  }

  @Nullable
  private Object resolveDouble(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof Double) {
        return val;
      } else if (val instanceof Number) {
        return (((Number) val).doubleValue());
      } else if (val instanceof String) {
        return Double.valueOf((String) val);
      } else if (val == null) {
        return null;
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to double: " + val);
      }
    }
    return null;
  }

  @Nullable
  private Object resolveFloat(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof Float) {
        return val;
      } else if (val instanceof Number) {
        return (((Number) val).floatValue());
      } else if (val instanceof String) {
        return Float.valueOf((String) val);
      } else if (val == null) {
        return null;
      } else {
        throw new IllegalArgumentException("Invalid configuration " + prop + ", cannot be converted to float: " + val);
      }
    }
    return null;
  }

  @Nullable
  private Object resolveBigDecimal(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof BigDecimal) {
        return val;
      }
      if (val == null) {
        return null;
      } else {
        return new BigDecimal(val.toString());
      }
    }
    return null;
  }

  @Nullable
  private Object resolveBigInteger(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof BigInteger) {
        return val;
      }
      if (val == null) {
        return null;
      } else {
        return new BigInteger(val.toString());
      }
    }
    return null;
  }

  @Nullable
  private Object resolveBoolean(final Injectee injectee, final ServiceHandle<?> handle) {
    ConfigurationParam param = getConfigAnnotation(injectee);
    if (param != null) {
      String prop = param.getPropertyName();
      Object val = configuration.getProperty(prop);
      if (val == null) {
        val = param.getDefaultValue();
      }
      if (val instanceof Boolean) {
        return val;
      }
      if (val == null) {
        return null;
      } else {
        return Boolean.valueOf(val.toString());
      }
    }
    return null;
  }

  @Override
  @Nullable
  public Object resolve(final Injectee injectee, final ServiceHandle<?> handle) {
    Type requiredType = injectee.getRequiredType();
    BiFunction<Injectee, ServiceHandle<?>, Object> exact = typeResolvers.get(requiredType);
    if (exact == null) {
      return null;
    }
    return exact.apply(injectee, handle);
  }

  @Nullable
  private static ConfigurationParam getConfigAnnotation(final Injectee injectee) {
    ConfigurationParam result = null;
    AnnotatedElement elem = injectee.getParent();
    if (elem instanceof Constructor) {
      Constructor ctor = (Constructor) elem;
      Annotation[] parameterAnnotation = ctor.getParameterAnnotations()[injectee.getPosition()];
      String paramName = null;
      String paramDefaultValue = null;
      for (Annotation ann : parameterAnnotation) {
        Class<? extends Annotation> annotationType = ann.annotationType();
        if (annotationType == ConfigProperty.class) {
          paramName = ((ConfigProperty) ann).value();
        } else if (annotationType == DefaultValue.class) {
          paramDefaultValue = ((DefaultValue) ann).value();
        }
      }
      if (paramName != null) {
        result = new ConfigurationParam(paramName, paramDefaultValue);
      }
    } else {
      ConfigProperty cfg = elem.getAnnotation(ConfigProperty.class);
      if (cfg != null) {
        DefaultValue defVal = elem.getAnnotation(DefaultValue.class);
        result = new ConfigurationParam(cfg.value(), defVal == null ? null : defVal.value());
      }
    }
    return result;
  }

  @Override
  public boolean isConstructorParameterIndicator() {
    return true;
  }

  @Override
  public boolean isMethodParameterIndicator() {
    return true;
  }

  @Override
  public String toString() {
    return "ConfigurationInjector{" + "configuration=" + configuration + '}';
  }

}
