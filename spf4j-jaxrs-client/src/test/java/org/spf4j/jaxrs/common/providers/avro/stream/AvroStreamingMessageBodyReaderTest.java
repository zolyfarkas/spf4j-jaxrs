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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.ParameterizedType;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.ArrayWriter;
import org.spf4j.jaxrs.StreamingArrayContent;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 *
 * @author Zoltan Farkas
 */
public class AvroStreamingMessageBodyReaderTest {


  @Test
  @SuppressFBWarnings("DM_NEW_FOR_GETCLASS")
  public void testStreamingTypeIntrospection() {
    ParameterizedType pType = MessageBodyRWUtils
            .toParameterizedType(StreamingArrayContent.class, new StreamingArrayContent<String>() {
      @Override
      public void write(final ArrayWriter<String> output) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    }.getClass());
    Assert.assertEquals(String.class, pType.getActualTypeArguments()[0]);

  }

}
