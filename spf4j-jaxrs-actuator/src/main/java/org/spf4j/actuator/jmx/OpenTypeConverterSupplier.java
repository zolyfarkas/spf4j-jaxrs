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
package org.spf4j.actuator.jmx;

import javax.annotation.Nullable;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;

/**
 * @author Zoltan Farkas
 */
public final class OpenTypeConverterSupplier {

  public static final OpenTypeConverterSupplier INSTANCE  = new OpenTypeConverterSupplier();

  private final CachingTypeMapWrapper<OpenTypeAvroConverter> typeHandler;

  private OpenTypeConverterSupplier() {
    typeHandler = new CachingTypeMapWrapper<>(new GraphTypeMap());
    typeHandler.safePut(SimpleType.class, OpenTypeAvroConverter.SIMPLE_TYPE)
            .safePut(ArrayType.class, OpenTypeAvroConverter.ARRAY_TYPE)
            .safePut(CompositeType.class, OpenTypeAvroConverter.COMPOSITE_TYPE)
            .safePut(TabularType.class, OpenTypeAvroConverter.TABULAR_TYPE);
  }

  public OpenTypeAvroConverter getConverter(@Nullable final OpenType<?> openType) {
    if (openType == null) {
      return OpenTypeAvroConverter.SIMPLE_TYPE;
    } else {
      OpenTypeAvroConverter exact = typeHandler.getExact(openType.getClass());
      if (exact == null) {
        throw new UnsupportedOperationException("No converter for " + openType);
      }
      return exact;
    }
  }

  @Override
  public String toString() {
    return "OpenTypeConverterSupplier{" + "typeHandler=" + typeHandler + '}';
  }
  
}
