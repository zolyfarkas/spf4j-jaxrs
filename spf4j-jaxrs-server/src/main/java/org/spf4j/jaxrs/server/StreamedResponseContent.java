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
import java.io.InputStream;
import java.io.OutputStream;
import javax.ws.rs.core.StreamingOutput;
import org.spf4j.io.Streams;

/**
 *
 * @author Zoltan Farkas
 */
public final class StreamedResponseContent implements StreamingOutput {

  private final InputStream is;
  private final long from;
  private final long to;

  public StreamedResponseContent(final InputStream is) {
    this(is, 0, -1);
  }

  public StreamedResponseContent(final InputStream is, final long from, final long to) {
    this.is = is;
    this.from = from;
    this.to = to;
  }

  @Override
  public void write(final OutputStream output) throws IOException {
    try (InputStream bis = is) {
       long skip = bis.skip(from);
        if (skip != from) {
          throw new UnsupportedOperationException("Unable to skip " + from + " bytes, managed only " + skip);
        }
        if (to < 0) {
          Streams.copy(bis, output);
        } else {
          Streams.copy(bis, output, 8192, to);
        }
    }
  }

  @Override
  public String toString() {
    return "StreamedResponseContent{" + "is=" + is + '}';
  }
}
