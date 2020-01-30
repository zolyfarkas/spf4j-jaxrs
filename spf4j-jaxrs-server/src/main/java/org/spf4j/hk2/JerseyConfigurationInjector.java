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
import org.spf4j.jaxrs.SystemConfiguration;

/**
 * A configuration injector for Jersey.
 * @author Zoltan Farkas
 */
public final class JerseyConfigurationInjector implements InjectionResolver<ConfigProperty> {

  private final ConfigurationResolver resolver;

  @Inject
  public JerseyConfigurationInjector(@Context final Configuration configuration) {
    this.resolver = new ConfigurationResolver(new SystemConfiguration(configuration));
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
        Function<ConfigurationParam, Object> typeConv = resolver.get(ptype.getActualTypeArguments()[0]);
        return new ConfigSupplier(typeConv, cfgParam);
      } else {
        throw new IllegalArgumentException("Unable to inject " + injectee);
      }
    }
    Function<ConfigurationParam, Object> exact = resolver.get(requiredType);
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
    return "JerseyConfigurationInjector{" + "resolver=" + resolver + '}';
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
