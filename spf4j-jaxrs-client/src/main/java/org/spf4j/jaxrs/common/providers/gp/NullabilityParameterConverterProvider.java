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
package org.spf4j.jaxrs.common.providers.gp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.Wrapper;
import org.spf4j.jaxrs.common.providers.ProviderUtils;

/**
 *
 * @author Zoltan Farkas
 */
@Priority(0)
@Provider
public final class NullabilityParameterConverterProvider implements ParamConverterProvider {

  private final Iterable<ParamConverterProvider> providers;

  /** org.glassfish are considered system providers */
  @Inject
  public NullabilityParameterConverterProvider(final Iterable<ParamConverterProvider> providers) {
    this.providers = providers;
  }

  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  public static <T> ParamConverter<T> from(final ParamConverter<T> toWrap, final Annotation... annots) {
    for (Annotation a : annots) {
      if (a instanceof Nullable) {
        return new NullableParameterConverterWrapper(toWrap);
      } else if (a instanceof Nonnull) {
        return new NonnullParameterConverterWrapper(toWrap);
      }
    }
    return new NonnullParameterConverterWrapper(toWrap);
  }

  @Override
  @Nullable
  public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType,
          final Annotation[] annotations) {
    for (ParamConverterProvider p : ProviderUtils.ordered(providers)) {
      if (p instanceof NullabilityParameterConverterProvider) {
        continue;
      }
      ParamConverter<T> converter = p.getConverter(rawType, genericType, annotations);
      if (converter != null) {
        return from(converter, annotations);
      }
    }
    return null;
  }



  private static class NonnullParameterConverterWrapper<T> implements ParamConverter<T>, Wrapper<ParamConverter<T>> {

    private final ParamConverter<T> wrapped;

    NonnullParameterConverterWrapper(final ParamConverter<T> wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public T fromString(@Nullable final String value) {
      if (value == null) {
        throw new ClientErrorException("Parameter cannot be null/optional for " + this, Response.Status.BAD_REQUEST);
      }
      return wrapped.fromString(value);
    }

    @Override
    public String toString(@Nullable final T value) {
      if (value == null) {
        throw new ClientErrorException("Parameter value cannot be null/optional for " + this,
                Response.Status.BAD_REQUEST);
      }
      return wrapped.toString();
    }

    @Override
    public String toString() {
      return "NonnullParameterConverterWrapper{" + "wrapped=" + wrapped + '}';
    }

    @Override
    public ParamConverter<T> getWrapped() {
      return wrapped;
    }

  }

  private static class NullableParameterConverterWrapper<T> implements ParamConverter<T>, Wrapper<ParamConverter<T>> {

    private final ParamConverter<T> wrapped;

    NullableParameterConverterWrapper(final ParamConverter<T> wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public T fromString(@Nullable final String value) {
      if (value == null) {
        return null;
      }
      return wrapped.fromString(value);
    }

    @Override
    public String toString(@Nullable final T value) {
      if (value == null) {
        return null;
      }
      return wrapped.toString();
    }

    @Override
    public String toString() {
      return "NullableParameterConverterWrapper{" + "wrapped=" + wrapped + '}';
    }

    @Override
    public ParamConverter<T> getWrapped() {
      return wrapped;
    }

  }

  @Override
  public String toString() {
    return "NullabilityParameterConverterProvider{" + "providers=" + providers + '}';
  }

}
