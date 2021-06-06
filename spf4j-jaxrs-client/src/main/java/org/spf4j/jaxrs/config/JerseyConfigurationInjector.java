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
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;

/**
 * A configuration injector for Jersey.
 * @author Zoltan Farkas
 */
public final class JerseyConfigurationInjector implements InjectionResolver<ConfigProperty> {

  private final Configuration configuration;

  private final ObjectConverters resolver;

  @Inject
  public JerseyConfigurationInjector(@Context final Configuration configuration,
           final JerseyMicroprofileConfigurationProvider prov) {
    this.configuration = new MergedConfigs(configuration, prov.getConfiguration());
    this.resolver = prov.getConverters();
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
        BiFunction<Object, Type, Object> typeConv = resolver.get(ptype.getActualTypeArguments()[0]);
        if (tt.isSubtypeOf(ObservableSupplier.class)) {
          return new ObservableRXConfigSupplier(configuration,  typeConv, cfgParam, ptype.getActualTypeArguments()[0]);
        }
        return new RXConfigSupplier(configuration, typeConv, cfgParam, requiredType);
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
        if (cfgParam.isNullable()) {
          return null;
        } else {
          throw new IllegalArgumentException("Unable to inject " + injectee + ", not nullable");
        }
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
      boolean nullable = false;
      for (Annotation ann : parameterAnnotation) {
        Class<? extends Annotation> annotationType = ann.annotationType();
        if (annotationType == ConfigProperty.class) {
          ConfigProperty cfgAnn = (ConfigProperty) ann;
          paramName = cfgAnn.name();
          paramDefaultValue =  cfgAnn.defaultValue();
        } else if (annotationType == Nullable.class) {
          nullable = true;
        }
      }
      if (paramName != null) {
        result = new ConfigurationParam(paramName, paramDefaultValue, nullable);
      }
    } else {
      ConfigProperty cfg = elem.getAnnotation(ConfigProperty.class);
      if (cfg != null) {
        result = new ConfigurationParam(cfg.name(), cfg.defaultValue(),
                elem.getAnnotation(Nullable.class) != null);
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
    return "JerseyConfigurationInjector{" + "resolver=" + resolver + '}';
  }

  @Override
  public Class<ConfigProperty> getAnnotation() {
    return ConfigProperty.class;
  }

}
