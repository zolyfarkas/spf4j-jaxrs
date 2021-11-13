/*
 * Copyright 2019 The Apache Software Foundation.
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
package org.spf4j.avro.compiler.extension;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.GenEntity;
import org.apache.avro.compiler.specific.JavaAnnotationsGenerator;
import org.apache.avro.compiler.specific.SpecificCompiler;

/**
 * @author Zoltan Farkas
 */
public final class JaxRsAnnotationsGenerator implements JavaAnnotationsGenerator {

  private static final Map<String, String> PARAM_MAP;

  static {
    PARAM_MAP = new HashMap<>();
    for (Class clasz : new Class[] {PathParam.class, QueryParam.class,
            HeaderParam.class, CookieParam.class, MatrixParam.class, FormParam.class, BeanParam.class}) {
      PARAM_MAP.put(clasz.getSimpleName(), clasz.getName());
    }
  }

  @Override
  @SuppressWarnings("checkstyle:Regexp")
  public Set<String> generate(final SpecificCompiler compiler, final GenEntity entity, final JsonProperties props,
          final Schema schema, @Nullable final Schema outSchema) {

    if (schema == null) {
      return Collections.EMPTY_SET;
    }
    switch (entity) {
      case FIELD:
         Map<String, Object> objectProps = props.getObjectProps();
        Set<String> propKeys = objectProps.keySet();
        Sets.SetView<String> annots = Sets.intersection(PARAM_MAP.keySet(), propKeys);
        if (annots.isEmpty()) {
          return Collections.EMPTY_SET;
        } else {
          Set<String> result = Sets.newHashSetWithExpectedSize(annots.size());
          for (String annot : annots) {
            result.add(PARAM_MAP.get(annot) + "(\"" + objectProps.get(annot) + "\")");
          }
          return result;
        }

      default:
        return Collections.EMPTY_SET;
    }
  }

}
