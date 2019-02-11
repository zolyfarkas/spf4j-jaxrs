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
package org.spf4j.jaxrs.server;

import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Zoltan Farkas
 */
public final class MediaTypes {

  private MediaTypes() { }

  public static MediaType getMatch(final List<MediaType> acceptableMediaTypes, final Set<MediaType> supported) {
    for (MediaType mt : acceptableMediaTypes) {
      if (supported.contains(mt)) {
        return mt;
      }
      for (MediaType smt : supported) {
        if (match(mt, smt)) {
          return smt;
        }
      }
    }
    return MediaType.APPLICATION_JSON_TYPE;
  }

  public static boolean match(final MediaType accept, final MediaType produces) {
    if ("*".equals(accept.getSubtype())) {
      if ("*".equals(accept.getType())) {
        return true;
      }
      return accept.getType().equals(produces.getType());
    } else {
      if ("*".equals(accept.getType())) {
        return accept.getSubtype().equals(produces.getSubtype());
      } else {
        return accept.getSubtype().equals(produces.getSubtype()) && accept.getType().equals(produces.getType());
      }
    }
  }

}
