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
package org.spf4j.jaxrs.server.providers.filters;


import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.util.List;
import javax.annotation.Priority;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.jaxrs.Buffered;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 * @author Zoltan Farkas
 */
@ProjectionSupport
@Provider
@Priority(Priorities.ENTITY_CODER - 10)
public final class ProjectionJaxRsFilter implements ContainerResponseFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectionJaxRsFilter.class);

  @Override
  public void filter(final ContainerRequestContext requestContext,
          final ContainerResponseContext responseContext) {
    MultivaluedMap<String, String> qp = requestContext.getUriInfo().getQueryParameters();
    String select = qp.getFirst("_project");
    if (select == null) {
      return;
    }
    List<String> projection;
    try {
      projection = Csv.readRow(select);
    } catch (CsvParseException ex) {
      throw new ClientErrorException("Invalid projection " + select, 400, ex);
    }
    Iterable<? extends IndexedRecord> entity = (Iterable<? extends IndexedRecord>) responseContext.getEntity();
    LOG.debug("Projecting: {} entity: {}", select, entity);
    Schema sourceSchema = MessageBodyRWUtils.getAvroSchemaFromType(responseContext.getEntityClass(),
            responseContext.getEntityType(), entity, responseContext.getEntityAnnotations());
    if (sourceSchema.getType() != Schema.Type.ARRAY) {
      throw new IllegalStateException("you can only use " + this
              + " with methods that return an array/collection/iterable");
    }
    Schema elementType = sourceSchema.getElementType();
    Schema resultSchema = Schemas.project(elementType, projection);
    if (resultSchema == null) {
      throw new ClientErrorException("Invalid projection " + projection + " of " + elementType, 400);
    }
    Closeable cl;
    if (entity instanceof Closeable) {
      cl = (Closeable) entity;
    } else {
      cl = () -> { };
    }
    int bufferSize;
    if (entity instanceof Buffered) {
      bufferSize = ((Buffered) entity).getElementBufferSize();
    } else {
      bufferSize = 64;
    }
    IterableArrayContent<IndexedRecord> projected = IterableArrayContent.from(Iterables.transform(entity,
            (x) -> Schemas.project(elementType, sourceSchema, x)), cl, bufferSize,
            resultSchema);
    responseContext.setEntity(projected);
  }

}
