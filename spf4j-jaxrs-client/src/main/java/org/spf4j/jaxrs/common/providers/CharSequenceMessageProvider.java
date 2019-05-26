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

/**
 *
 * @author Zoltan Farkas
 */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import static org.glassfish.jersey.message.internal.ReaderWriter.BUFFER_SIZE;

@Provider
@Produces({"text/plain", "*/*"})
@Consumes({"text/plain", "*/*"})
@Singleton
public final class CharSequenceMessageProvider extends AbstractMessageReaderWriterProvider<CharSequence> {

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return CharSequence.class == type;
  }

  @Override
  public CharSequence readFrom(
          final Class<CharSequence> type,
          final Type genericType,
          final Annotation[] annotations,
          final MediaType mediaType,
          final MultivaluedMap<String, String> httpHeaders,
          final InputStream entityStream) throws IOException {
    return readFromAsString(new InputStreamReader(entityStream, getCharset(mediaType)));
  }

  private static CharSequence readFromAsString(final Reader reader) throws IOException {
    StringBuilder sb = new StringBuilder(128);
    char[] c = new char[BUFFER_SIZE];
    int l;
    while ((l = reader.read(c)) != -1) {
      sb.append(c, 0, l);
    }
    return sb;
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return CharSequence.class.isAssignableFrom(type) && DirectStringMessageProvider.isRaw(annotations);
  }

  @Override
  public long getSize(final CharSequence s, final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType) {
    return s.length();
  }

  @Override
  public void writeTo(
          final CharSequence t,
          final Class<?> type,
          final Type genericType,
          final Annotation[] annotations,
          final MediaType mediaType,
          final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream) throws IOException {
    writeToAsString(t, entityStream, mediaType);
  }

  public static void writeToAsString(final CharSequence s, final OutputStream out, final MediaType type)
          throws IOException {
    Writer osw = new OutputStreamWriter(out, getCharset(type));
    osw.append(s);
    osw.flush();
  }

}
