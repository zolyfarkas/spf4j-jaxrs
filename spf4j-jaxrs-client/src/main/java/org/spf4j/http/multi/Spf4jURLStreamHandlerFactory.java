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
package org.spf4j.http.multi;

import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import javax.annotation.Nullable;

/**
 *
 * @author Zoltan Farkas
 */
@Beta
public final class Spf4jURLStreamHandlerFactory implements URLStreamHandlerFactory {

  @Override
  @Nullable
  public URLStreamHandler createURLStreamHandler(final String protocol) {
    if (protocol == null) {
      return null;
    }
    if (Ascii.equalsIgnoreCase("mhttp", protocol)) {
      return MultiHttpURLStreamHandler.INSTANCE;
    } else if (Ascii.equalsIgnoreCase("mhttps", protocol)) {
      return MultiHttpsURLStreamHandler.INSTANCE;
    } else {
      return null;
    }
  }

}
