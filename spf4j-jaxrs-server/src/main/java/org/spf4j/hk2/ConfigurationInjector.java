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

import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;
import org.spf4j.jaxrs.SystemConfiguration;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"URV_UNRELATED_RETURN_VALUES", "UP_UNUSED_PARAMETER"})
public final class ConfigurationInjector implements InjectionResolver<ConfigProperty> {

  private final Configuration configuration;

  private final CachingTypeMapWrapper<Function<ConfigurationParam, Object>> typeResolvers;

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
  private Object resolveString(final ConfigurationParam param) {
    if (param != null) {
      return getConfig(param.getPropertyName(), param.getDefaultValue());
    }
    return null;
  }

  @Nullable
  private Object resolveInt(final ConfigurationParam param) {
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
  private Object resolveLong(final ConfigurationParam param) {
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
  private Object resolveDouble(final ConfigurationParam param) {
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
  private Object resolveFloat(final ConfigurationParam param) {
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
  private Object resolveBigDecimal(final ConfigurationParam param) {
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
  private Object resolveBigInteger(final ConfigurationParam param) {
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
  private Object resolveBoolean(final ConfigurationParam param) {
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
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public Object resolve(final Injectee injectee) {
    ConfigurationParam cfgParam = getConfigAnnotation(injectee);
    if (cfgParam == null) {
      throw new IllegalStateException("Config annotations not present in " + injectee);
    }
    Type requiredType = injectee.getRequiredType();
    if (requiredType instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) requiredType;
      TypeToken<?> tt = TypeToken.of(ptype);
      if (tt.isSubtypeOf(Provider.class) || tt.isSubtypeOf(Supplier.class)) {
        Function<ConfigurationParam, Object> typeConv
                = typeResolvers.get(ptype.getActualTypeArguments()[0]);
        return new ConfigSupplier(typeConv, cfgParam);
      } else {
        throw new IllegalArgumentException("Unable to inject " + injectee);
      }
    }
    Function<ConfigurationParam, Object> exact = typeResolvers.get(requiredType);
    if (exact == null) {
      Object res = configuration.getProperty(cfgParam.getPropertyName());
      if (res == null) {
        return cfgParam.getDefaultValue();
      }
      return res;
    }
    return exact.apply(cfgParam);
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
          ConfigProperty cfgAnn = (ConfigProperty) ann;
          paramName = cfgAnn.name();
          paramDefaultValue =  cfgAnn.defaultValue();
          break;
        }
      }
      if (paramName != null) {
        result = new ConfigurationParam(paramName, paramDefaultValue);
      }
    } else {
      ConfigProperty cfg = elem.getAnnotation(ConfigProperty.class);
      if (cfg != null) {
        result = new ConfigurationParam(cfg.name(), cfg.defaultValue());
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

  @Override
  public Class<ConfigProperty> getAnnotation() {
    return ConfigProperty.class;
  }

  private static class ConfigSupplier implements Supplier, Provider {

    private final Function<ConfigurationParam, Object> typeConv;
    private final ConfigurationParam cfgParam;

    ConfigSupplier(final Function<ConfigurationParam, Object> typeConv, final ConfigurationParam cfgParam) {
      this.typeConv = typeConv;
      this.cfgParam = cfgParam;
    }

    @Override
    public Object get() {
      return typeConv.apply(cfgParam);
    }
  }

}
