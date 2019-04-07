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
package org.spf4j.jaxrs.server.providers;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

/**
 *
 * @author Zoltan Farkas
 */
@Provider
public final class GZipReaderInterceptor implements ReaderInterceptor {

  @Override
  public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException {
    List<String> header = context.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
    if (header != null && header.contains("gzip")) {
      context.setInputStream(new GZIPInputStream(context.getInputStream()));
    }
    return context.proceed();
  }
}
