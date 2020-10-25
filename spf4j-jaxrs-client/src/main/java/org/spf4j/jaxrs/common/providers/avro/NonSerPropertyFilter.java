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
package org.spf4j.jaxrs.common.providers.avro;

import com.fasterxml.jackson.core.filter.TokenFilter;
import javax.annotation.Nullable;

/**
 *
 * @author Zoltan Farkas
 */
public final class NonSerPropertyFilter extends TokenFilter {

  public static final NonSerPropertyFilter INSTANCE = new NonSerPropertyFilter();

  private NonSerPropertyFilter() {
  }



  @Override
  @Nullable
  public TokenFilter includeProperty(final String name) {
    switch (name) {
      case "doc":
      case "java-class":
      case "avro.java.string":
        return null;
      case "default": // for whatever reson without this empty arrays are converted to null.
        return TokenFilter.INCLUDE_ALL;
      default:
        return this;
    }
  }
}
