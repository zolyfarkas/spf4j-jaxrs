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
package org.spf4j.http;

import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContext.Tag;

/**
 * @author Zoltan Farkas
 */
public final class RequestContextTags {

  private RequestContextTags() {
  }

  public static final Tag<Integer> TRY_COUNT = new ExecutionContext.Tag<Integer>() {
    @Override
    public Integer accumulate(final Integer existing, final Integer current) {
      if (existing == null) {
        return current;
      }
      return existing  + current;
    }

  };

}
