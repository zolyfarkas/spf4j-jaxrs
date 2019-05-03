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

import java.lang.reflect.Type;

/**
 *
 * @author Zoltan Farkas
 */
public final class MessageBodyRWUtils {

  private MessageBodyRWUtils() { }

  /**
   * when using dynamic resources, type can be an ArrayList.class, and genericType is Object.class.
   */
  public static Type effectiveType(final Class<?> type, final Type genericType) {
    return genericType != null && genericType != Object.class ? genericType : type;
  }
}
