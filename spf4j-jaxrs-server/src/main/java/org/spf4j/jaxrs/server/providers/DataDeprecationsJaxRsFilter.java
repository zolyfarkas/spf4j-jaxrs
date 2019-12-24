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
package org.spf4j.jaxrs.server.providers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.avro.AvroContainer;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;


/**
 * @author Zoltan Farkas
 */
@Provider
@Priority(Priorities.ENTITY_CODER + 5)
public final class DataDeprecationsJaxRsFilter implements ContainerResponseFilter {

  private static final Logger LOG = Logger.getLogger(DataDeprecationsJaxRsFilter.class.getName());

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    Schema respSchema = null;
    Object entity = responseContext.getEntity();
    if (entity instanceof AvroContainer) {
      Schema elementSchema = ((AvroContainer) entity).getElementSchema();
      if (elementSchema != null) {
        respSchema = Schema.createArray(elementSchema);
      }
    }
    if (respSchema == null) {
      try {
         respSchema = MessageBodyRWUtils.getAvroSchemaFromType(responseContext.getEntityClass(),
              responseContext.getEntityType(), responseContext.getEntityAnnotations());
      } catch (RuntimeException e) {
        LOG.log(Level.FINE, "Schema unavailability reason", e);
        return;
      }
    }
    if (respSchema == null) {
      LOG.log(Level.FINE, "No schema available for {0}", entity);
      return;
    }
    Map<String, String> deprecations = new HashMap<>(4);
    Schemas.deprecations(respSchema, deprecations::put);
    if (deprecations.isEmpty()) {
      return;
    }
    MultivaluedMap<String, Object> headers = responseContext.getHeaders();
    for (Map.Entry<String, String> dep : deprecations.entrySet()) {
      headers.add(Headers.WARNING, new HttpWarning(HttpWarning.MISCELLANEOUS, "deprecation",
              "Deprecated " + dep.getKey() + "; " + dep.getValue()));
    }
  }

}
