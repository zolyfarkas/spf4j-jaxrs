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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.spf4j.io.PushbackInputStreamEx;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/stack.samples.d3+json"})
@Consumes({"application/stack.samples.d3+json"})
@Singleton
public final class SampleNodeMessageProviderD3Json
        implements MessageBodyReader<SampleNode>, MessageBodyWriter<SampleNode> {

  @Override
  public boolean isReadable(final Class<?> type, final Type type1,
          final Annotation[] antns, final MediaType mt) {
    return type == SampleNode.class;
  }

  @Override
  public SampleNode readFrom(final Class<SampleNode> type, final Type type1, final Annotation[] antns,
          final MediaType mt, final MultivaluedMap<String, String> mm, final InputStream pin)
          throws IOException {
    PushbackInputStreamEx in = new PushbackInputStreamEx(pin);
    int read = in.read();
    if (read < 0) {
      return null;
    }
    in.unread(read);
    Charset charset = AbstractMessageReaderWriterProvider.getCharset(mt);
    try (BufferedReader br = new BufferedReader(new InputStreamReader(in, charset))) {
      return SampleNode.parseD3Json(br).getSecond();
    }
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type type1, final Annotation[] antns, final MediaType mt) {
    return type == SampleNode.class;
  }

  @Override
  public void writeTo(final SampleNode t, final Class<?> type,
          final Type type1, final Annotation[] antns, final MediaType mt,
          final MultivaluedMap<String, Object> mm, final OutputStream out)
          throws IOException {
    if (t == null) {
      return;
    }
    Charset charset = AbstractMessageReaderWriterProvider.getCharset(mt);
    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, charset))) {
      t.writeD3JsonTo(bw);
      bw.flush();
    }
  }

}
