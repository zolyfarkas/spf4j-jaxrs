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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import org.spf4j.base.Reflections;

/**
 *
 * @author Zoltan Farkas
 */
public final class ProviderUtils {

  private static final String SYS_PACKAGE = "org.glassfish";

  private ProviderUtils() { }

  public static int getProviderPriority(final Class<?> providerClass) {
    Priority pa = Reflections.getInheritedAnnotation(Priority.class, providerClass);
    if (pa != null) {
      return pa.value();
    } else {
      return Priorities.USER;
    }
  }

  public static <T> List<T> ordered(final Iterable<T> providers) {
    List<T> result = new ArrayList<>();
    for (T p : providers) {
      result.add(p);
    }
    Collections.sort(result, (a, b) -> {
      Class<? extends Object> aClass = a.getClass();
      Class<? extends Object> bClass = b.getClass();
      String aClassName = aClass.getName();
      String bClassName = bClass.getName();
      if (aClassName.startsWith(SYS_PACKAGE)) {
        if (bClassName.startsWith(SYS_PACKAGE)) {
            return Integer.compare(getProviderPriority(aClass), getProviderPriority(bClass));
        } else {
          return 1;
        }
      } else {
        if (bClassName.startsWith(SYS_PACKAGE)) {
          return -1;
        } else {
          return Integer.compare(getProviderPriority(aClass), getProviderPriority(bClass));
        }
      }
    });
    return result;
  }
}
