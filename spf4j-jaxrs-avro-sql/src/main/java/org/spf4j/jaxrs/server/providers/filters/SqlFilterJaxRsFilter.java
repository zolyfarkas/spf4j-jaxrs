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
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.calcite.SqlRowPredicate;
import org.spf4j.jaxrs.Buffered;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.jaxrs.SqlFilterSupport;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 * @author Zoltan Farkas
 */
@SqlFilterSupport
@Provider
@Priority(Priorities.ENTITY_CODER - 5)
public final class SqlFilterJaxRsFilter implements ContainerResponseFilter {

  private static final Logger LOG = LoggerFactory.getLogger(SqlFilterJaxRsFilter.class);

  @Override
  public void filter(final ContainerRequestContext requestContext,
          final ContainerResponseContext responseContext) {
    MultivaluedMap<String, String> qp = requestContext.getUriInfo().getQueryParameters();
    String where = qp.getFirst("_where");
    if (where == null) {
      return;
    }
    Iterable<IndexedRecord> entity = (Iterable<IndexedRecord>) responseContext.getEntity();
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
    LOG.debug("Filtering: {} entity: {}", where, entity);
    Schema sourceSchema = MessageBodyRWUtils.getAvroSchemaFromType(responseContext.getEntityClass(),
            responseContext.getEntityType(), entity, responseContext.getEntityAnnotations());
    SqlRowPredicate predicate;
    try {
      predicate = new SqlRowPredicate(where, sourceSchema.getElementType());
    } catch (SqlParseException | ValidationException | RelConversionException ex) {
      throw new ClientErrorException("Invalid predicate " + where, 400, ex);
    }
    Iterable<IndexedRecord> filtered = Iterables.filter(entity, predicate::test);
    IterableArrayContent<IndexedRecord> fresp = IterableArrayContent.from(filtered, cl, bufferSize, sourceSchema);
    responseContext.setEntity(fresp);
  }

}
