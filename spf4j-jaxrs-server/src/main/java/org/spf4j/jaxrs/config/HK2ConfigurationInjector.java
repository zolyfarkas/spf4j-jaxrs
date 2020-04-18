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
package org.spf4j.jaxrs.config;

import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

/**
 * a configuration injector for HK2.
 * @author Zoltan Farkas
 */
public final class HK2ConfigurationInjector implements InjectionResolver<ConfigProperty> {

  private final ObjectConverters resolver;

  private final Configuration configuration;

  @Inject
  public HK2ConfigurationInjector(@Context final Configuration configuration,
          final JerseyMicroprofileConfigurationProvider prov) {
    this.configuration = configuration;
    this.resolver = prov.getConverters();
  }

  @Override
  @Nullable
  @SuppressFBWarnings("URV_INHERITED_METHOD_WITH_RELATED_TYPES")
  public Object resolve(final Injectee injectee,  final ServiceHandle<?> root) {
    ConfigurationParam cfgParam = getConfigAnnotation(injectee);
    if (cfgParam == null) {
      throw new IllegalStateException("Config annotations not present in " + injectee);
    }
    Type requiredType = injectee.getRequiredType();
    if (requiredType instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType) requiredType;
      TypeToken<?> tt = TypeToken.of(ptype);
      if (tt.isSubtypeOf(Provider.class) || tt.isSubtypeOf(Supplier.class)) {
        BiFunction<Object, Type, Object> typeConv
                = resolver.get(ptype.getActualTypeArguments()[0]);
        return new ConfigSupplier(configuration,  typeConv, cfgParam, requiredType);
      } else {
        throw new IllegalArgumentException("Unable to inject " + injectee);
      }
    }
    Object val = configuration.getProperty(cfgParam.getPropertyName());
    if (val != null) {
      BiFunction<Object, Type, Object> exact = resolver.get(requiredType);
      return exact.apply(val, requiredType);
    } else {
      String dVal = cfgParam.getDefaultValue();
      if (dVal != null) {
        BiFunction<Object, Type, Object> exact = resolver.get(requiredType);
        return exact.apply(dVal, requiredType);
      } else {
        return null;
      }
    }
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
    return "HK2ConfigurationInjector{" + "resolver=" + resolver + '}';
  }

  public static final class ConfigSupplier implements Supplier, Provider {

    private final  BiFunction<Object, Type, Object> typeConv;
    private final ConfigurationParam cfgParam;
    private final Type type;
    private final Configuration configuration;

    ConfigSupplier(final Configuration configuration, final  BiFunction<Object, Type, Object> typeConv,
            final ConfigurationParam cfgParam, final Type type) {
      this.configuration = configuration;
      this.typeConv = typeConv;
      this.cfgParam = cfgParam;
      this.type = type;
    }

    @Override
    public Object get() {
      Object val = configuration.getProperty(cfgParam.getPropertyName());
      if (val != null) {
        return typeConv.apply(val, type);
      } else {
        String dval = cfgParam.getDefaultValue();
        if (dval != null) {
          return typeConv.apply(dval, type);
        } else {
          return null;
        }
      }
    }
  }

}
