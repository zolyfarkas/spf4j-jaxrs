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
package org.spf4j.http;

import com.google.common.collect.Range;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class HttpRangeTest {


  @Test
  public void testHttpRangeMethod() {
    String rVal = "bytes=0-100,100-200,200-";
    HttpRange range = HttpRange.valueOf(rVal);
    Assert.assertEquals("bytes", range.getUnit());
    List<Range<Long>> ranges = range.getRanges();
    Assert.assertEquals(0L, ranges.get(0).lowerEndpoint().longValue());
    Assert.assertEquals(100L, ranges.get(0).upperEndpoint().longValue());
    Assert.assertEquals(200L, ranges.get(2).lowerEndpoint().longValue());
    Assert.assertFalse(ranges.get(2).hasUpperBound());
    Assert.assertEquals(rVal, range.toString());
  }

}
