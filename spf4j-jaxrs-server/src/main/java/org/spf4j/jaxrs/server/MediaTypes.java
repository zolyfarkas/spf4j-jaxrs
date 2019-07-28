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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class MediaTypes {

  private static final Properties EXT_MAPPINGS = loadExtensionMappings();

  private static Properties loadExtensionMappings() {
    Properties m = new Properties();
    try {
      Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
              .getResources("/mt-mapping.properties");
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();
        try (Reader stream = new InputStreamReader(url.openStream(),
                StandardCharsets.UTF_8)) {
          m.load(stream);
        }
      }
      return m;
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

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

  @Nullable
  public static MediaType fromExtension(final String extension) {
    String mt = EXT_MAPPINGS.getProperty(extension);
    if (mt == null) {
      return null;
    }
    return MediaType.valueOf(mt);
  }

}
