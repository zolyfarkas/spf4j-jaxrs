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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import org.glassfish.jersey.spi.ContentEncoder;
import org.spf4j.io.LazyOutputStreamWrapper;

/**
 * GZIP encoding support. Interceptor that encodes the output or decodes the input if
 * {@link HttpHeaders#CONTENT_ENCODING Content-Encoding header} value equals to {@code gzip} or {@code x-gzip}.
 * since GZIPOutputStream writes to the underlying stream in the constructor,
 * we have to use a lazy wrapper to make sure Headers are not being lost.
 *
 * @author zolyfarkas
 */
@Priority(Priorities.ENTITY_CODER)
public final class GZipEncoderDecoder extends ContentEncoder {
    /**
     * Initialize GZipEncoder.
     */
    public GZipEncoderDecoder() {
        super("gzip", "x-gzip");
    }

    @Override
    public InputStream decode(final String contentEncoding, final InputStream encodedStream)
            throws IOException {
        return new GZIPInputStream(encodedStream);
    }

    @Override
    public OutputStream encode(final String contentEncoding, final OutputStream entityStream) {
        return new LazyOutputStreamWrapper(() ->  {
          try {
            return new GZIPOutputStream(entityStream);
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
        });
    }
}