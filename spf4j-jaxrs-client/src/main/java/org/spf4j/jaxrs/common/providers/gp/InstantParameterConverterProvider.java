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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * Ability to support Instant from parameters in the form of:
 * "now", "-P5D", "-PT5H", "2010-01-01T00:00:00.0Z"
 *
 * @author Zoltan Farkas
 */
@Provider
public final class InstantParameterConverterProvider implements ParamConverterProvider {

  @Override
  @SuppressWarnings("unchecked")
  public <T> ParamConverter<T> getConverter(final Class<T> clasz, final Type type, final Annotation[] annotations) {
    if (Instant.class == clasz) {
      return (ParamConverter<T>) new InstantParameterConverter();
    }
    return null;
  }

  private static class InstantParameterConverter implements ParamConverter<Instant> {

    @Override
    public Instant fromString(final String str) {
      if ("now".equals(str)) {
        return Instant.now();
      }
      try {
        if (str.indexOf('P') >= 0) {
          return Instant.now().plus(Duration.parse(str));
        } else {
          return Instant.parse(str);
        }
      } catch (DateTimeParseException ex) {
        throw new ClientErrorException("Invalid instant format: " + str, 400, ex);
      }
    }

    @Override
    public String toString(final Instant instant) {
      return instant.toString();
    }
  }

}
