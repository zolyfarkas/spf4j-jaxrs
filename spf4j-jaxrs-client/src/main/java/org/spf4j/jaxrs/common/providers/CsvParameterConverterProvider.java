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
package org.spf4j.jaxrs.common.providers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.spf4j.io.csv.CharSeparatedValues;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.io.csv.CsvReader;
import org.spf4j.io.csv.CsvWriter;
import org.spf4j.jaxrs.CsvParam;

/**
 * @author Zoltan Farkas
 */
@Provider
public final class CsvParameterConverterProvider implements ParamConverterProvider {


  private final Iterable<ParamConverterProvider> providers;

  /**
   * see https://javaee.github.io/hk2/introduction.html#iterable-injection
   * @param providers
   */
  @Inject
  public CsvParameterConverterProvider(final Iterable<ParamConverterProvider> providers) {
    this.providers = providers;
  }


  @Nullable
  private <T> ParamConverter getOthersConverter(final Class<T> clasz, final Type type, final Annotation[] annotations) {
    for (ParamConverterProvider prov : providers) {
      ParamConverter<T> converter = prov.getConverter(clasz, type, annotations);
      if (converter != null) {
        return converter;
      }
    }
    return getConverter(clasz, type, annotations);
  }

  @Override
  @Nullable
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  public <T> ParamConverter<T> getConverter(final Class<T> clasz, final Type type, final Annotation[] annotations) {
    if (annotations != null && Iterable.class.isAssignableFrom(clasz)) {
      for (Annotation annot : annotations) {
        if (annot instanceof CsvParam) {
          final Type compType;
          if (type instanceof ParameterizedType) {
            compType = ((ParameterizedType) type).getActualTypeArguments()[0];
          } else {
            compType = String.class;
          }
          ParamConverter compConverter = null;
          if (compType instanceof Class) {
            compConverter = getOthersConverter((Class) compType, compType, annotations);
          } else if (compType instanceof ParameterizedType) {
            compConverter = getOthersConverter((Class) ((ParameterizedType) compType).getRawType(),
                    compType, annotations);
          }
          if (compConverter == null) {
            throw new UnsupportedOperationException("Cannot get converter for " + compType);
          }
          return (ParamConverter<T>) new IterableCsvParamConverter((CsvParam) annot, compConverter);
        }
      }
    }
    return null;
  }

  private static class IterableCsvParamConverter implements ParamConverter<Iterable> {

    private final CharSeparatedValues csv;
    private final ParamConverter compConverter;

    IterableCsvParamConverter(final CsvParam annot, final ParamConverter compConverter) {
      this.csv = new CharSeparatedValues(annot.value());
      this.compConverter = compConverter;
    }

    @Override
    public Iterable fromString(final String value) {
      List result = new ArrayList();
      try {
        CsvReader reader = csv.reader(new StringReader(value));
        while (reader.next() != CsvReader.TokenType.END_DOCUMENT) {
          result.add(compConverter.fromString(reader.getElement().toString()));
        }
      } catch (IOException | CsvParseException ex) {
        throw new IllegalArgumentException("Invalid csv " + value, ex);
      }
      return result;
    }

    @Override
    public String toString(final Iterable itrble) {
      StringWriter sw = new StringWriter(32);
      CsvWriter writer = csv.writer(sw);
      for (Object obj : itrble) {
        try {
          writer.writeElement(compConverter.toString(obj));
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
      return sw.toString();
    }

    @Override
    public String toString() {
      return "ItarableCsvParamConverter{" + "csv=" + csv + ", compConverter=" + compConverter + '}';
    }
  }

  @Override
  public String toString() {
    return "CsvParameterConverterProvider{" + "providers=" + providers + '}';
  }

}
