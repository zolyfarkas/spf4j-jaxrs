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
package org.spf4j.jaxrs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import org.apache.avro.Schema;
import org.spf4j.base.CloseableIterable;


/**
 * @author Zoltan Farkas
 */
public interface IterableArrayContent<T> extends CloseableIterable<T>, Buffered, AvroContainer {

  static <T>  IterableArrayContent<T> from(final Iterable<T> it, final Schema elementSchema) {
    return from(it, () -> { }, 64, elementSchema);
  }

  static <T>  IterableArrayContent<T> from(final Iterable<T> it, final Closeable toClose,
          final int bufferSize, final Schema elementSchema) {
    return new IterableArrayContent<T>() {
      @Override
      @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
      public void close() {
        try {
          toClose.close();
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }

      @Override
      public Schema getElementSchema() {
        return elementSchema;
      }

      @Override
      public int getElementBufferSize() {
        return bufferSize;
      }


      @Override
      public Iterator<T> iterator() {
        return it.iterator();
      }
    };
  }

}
