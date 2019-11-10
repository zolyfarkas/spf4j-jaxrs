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

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.ext.ParamConverterProvider;
import org.junit.Assert;
import org.glassfish.jersey.internal.inject.ParamConverters;
import org.junit.Test;
import org.spf4j.jaxrs.common.providers.gp.CsvParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.gp.NullabilityParameterConverterProvider;


/**
 *
 * @author Zoltan Farkas
 */
public class ProviderUtilsTest {



  @Test
  public void testOrdering() {
    NullabilityParameterConverterProvider npp = new NullabilityParameterConverterProvider(null);
    CsvParameterConverterProvider csvp = new CsvParameterConverterProvider(null);
    List<ParamConverterProvider> ordered = ProviderUtils.ordered(Arrays.asList(new ParamConverters.DateProvider(),
            new ParamConverters.AggregatedProvider(), csvp, npp));
    Assert.assertSame(npp, ordered.get(0));
    Assert.assertSame(csvp, ordered.get(1));

  }

}
