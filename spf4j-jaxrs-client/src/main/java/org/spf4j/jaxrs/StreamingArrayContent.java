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

import java.io.Closeable;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.ArrayWriter;
import org.spf4j.base.avro.AvroContainer;

/**
 * Streaming Output, that will stream back an array.
 */
public interface StreamingArrayContent<T> extends Closeable, AvroContainer, Buffered {

  StreamingArrayContent EMPTY = new StreamingArrayContent() {
    @Override
    public void write(final ArrayWriter output) {
    }
  };

  static <T> StreamingArrayContent<T> empty() {
    return EMPTY;
  }

  void write(ArrayWriter<T> output) throws IOException;

  /**
   * Jersey will lose the stream after a MessageBodyReader is finished unless the object implements Closeable.
   *
   * @throws IOException
   */
  default void close() throws IOException {
    // nothing to close by default.
  }

  default StreamingArrayContent<IndexedRecord> project(final Schema resultSchema,
          final Schema elementType, final int bufferSize) {
    return new StreamingArrayContentProjection(this, resultSchema, elementType, bufferSize);
  }

  class StreamingArrayContentProjection<T> implements StreamingArrayContent {

    private final StreamingArrayContent<IndexedRecord> toWrap;
    private final Schema resultSchema;
    private final Schema elementType;
    private final int bufferSize;

    public StreamingArrayContentProjection(final StreamingArrayContent<IndexedRecord> toWrap,
            final Schema resultSchema, final Schema elementType, final int bufferSize) {
      this.toWrap = toWrap;
      this.resultSchema = resultSchema;
      this.elementType = elementType;
      this.bufferSize = bufferSize;
    }

    @Override
    public void write(final ArrayWriter output) throws IOException {
      toWrap.write((final IndexedRecord t) -> {
        output.write(Schemas.project(resultSchema, elementType, t));
      });
    }

    @Override
    public int getElementBufferSize() {
      return bufferSize;
    }

    @Override
    public Schema getElementSchema() {
      return resultSchema;
    }
  }

}
