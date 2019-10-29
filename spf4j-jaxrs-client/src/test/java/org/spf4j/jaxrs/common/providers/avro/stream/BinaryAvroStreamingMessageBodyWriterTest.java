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
package org.spf4j.jaxrs.common.providers.avro.stream;

import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Assert;
import org.apache.avro.SchemaResolver;
import org.junit.Test;
import org.spf4j.base.Arrays;
import org.spf4j.base.avro.MediaType;
import org.spf4j.base.ArrayWriter;
import org.spf4j.base.CloseableIterable;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.StreamingArrayContent;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("SE_BAD_FIELD_INNER_CLASS")
public class BinaryAvroStreamingMessageBodyWriterTest {

  @Test
  public void testSerDeser() throws IOException {
    MediaType m1 = new MediaType("a", "b", Collections.EMPTY_MAP);
    MediaType m2 = new MediaType("c", "d", Collections.EMPTY_MAP);
    MediaType m3 = new MediaType("e", "f", Collections.EMPTY_MAP);
    BinaryAvroStreamingMessageBodyWriter writer = new BinaryAvroStreamingMessageBodyWriter(
            new DefaultSchemaProtocol(SchemaResolver.NONE));

    MultivaluedMap headers = new MultivaluedHashMap();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    writer.writeTo(new StreamingArrayContent<MediaType>() {
      @Override
      public void write(final ArrayWriter<MediaType> w) throws IOException {
        w.accept(m1);
        w.accept(m2);
        w.flush();
        w.accept(m3);
        w.close();
      }
    }, StreamingArrayContent.class,
            new TypeToken<StreamingArrayContent<MediaType>>() { }.getType(),
            Arrays.EMPTY_ANNOT_ARRAY, new javax.ws.rs.core.MediaType("application", "avro"), headers, bos);
    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    BinaryAvroIterableMessageBodyReader reader = new BinaryAvroIterableMessageBodyReader(
            new DefaultSchemaProtocol(SchemaResolver.NONE));
    CloseableIterable<MediaType> rf = reader.readFrom(CloseableIterable.class,
            new TypeToken<CloseableIterable<MediaType>>() { }.getType(),
            Arrays.EMPTY_ANNOT_ARRAY, new javax.ws.rs.core.MediaType("application", "avro"), headers, bis);
    Iterator<MediaType> iterator = rf.iterator();
    Assert.assertEquals(m1, iterator.next());
    Assert.assertEquals(m2, iterator.next());
    Assert.assertEquals(m3, iterator.next());

  }

}
