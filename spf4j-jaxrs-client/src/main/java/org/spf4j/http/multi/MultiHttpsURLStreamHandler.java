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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("URLCONNECTION_SSRF_FD")
public final class MultiHttpsURLStreamHandler extends URLStreamHandler {

  static final MultiHttpsURLStreamHandler INSTANCE = new MultiHttpsURLStreamHandler();

  private MultiHttpsURLStreamHandler() { }

  @Override
  protected URLConnection openConnection(final URL u) throws IOException {
    return new MultiHttpsURLConnection(u,  MultiURLs.getChoiceCount());
  }

}
