/*
 * Copyright 2020 SPF4J.
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
package org.spf4j.jaxrs.features;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.jersey.message.DeflateEncoder;
import org.spf4j.jaxrs.common.providers.GZipEncoderDecoder;
import org.spf4j.jaxrs.common.providers.gp.CharSequenceMessageProvider;
import org.spf4j.jaxrs.common.providers.gp.CsvParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.gp.DirectStringMessageProvider;
import org.spf4j.jaxrs.common.providers.gp.InstantParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.gp.NullabilityParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.gp.SampleNodeMessageProviderD3Json;
import org.spf4j.jaxrs.common.providers.gp.SampleNodeMessageProviderJson;

/**
 * @author Zoltan Farkas
 */
public final class GeneralPurposeFeatures implements Feature {

  @Override
  public boolean configure(final FeatureContext fc) {
    fc.register(NullabilityParameterConverterProvider.class);
    fc.register(CsvParameterConverterProvider.class);
    fc.register(new CharSequenceMessageProvider());
    fc.register(new DirectStringMessageProvider());
    fc.register(new InstantParameterConverterProvider());
    fc.register(new SampleNodeMessageProviderJson());
    fc.register(new SampleNodeMessageProviderD3Json());
    fc.register(new GZipEncoderDecoder());
    fc.register(DeflateEncoder.class);
    return true;
  }

}
