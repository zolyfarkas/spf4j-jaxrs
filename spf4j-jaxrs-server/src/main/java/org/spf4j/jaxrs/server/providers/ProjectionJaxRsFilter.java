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


import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.Reflections;
import org.spf4j.io.Csv;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.jaxrs.Buffered;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.jaxrs.StreamingArrayContent;
import org.spf4j.jaxrs.common.providers.avro.MessageBodyRWUtils;

/**
 * @author Zoltan Farkas
 */
@ProjectionSupport
@Provider
@Priority(Priorities.ENTITY_CODER + 10)
public final class ProjectionJaxRsFilter implements ContainerResponseFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectionJaxRsFilter.class);

  private final javax.inject.Provider<ResourceInfo> resourceInfoProvider;

  @Inject
  public ProjectionJaxRsFilter(@Context final javax.inject.Provider<ResourceInfo> resourceInfo) {
    this.resourceInfoProvider = resourceInfo;
  }

  @Nullable
  private List<String> getDefaultProjection(final ResourceInfo resourceInfo) {
    Method method = resourceInfo.getResourceMethod();
    ProjectionSupport annotation = Reflections.getInheritedAnnotation(ProjectionSupport.class, method);
    if (annotation == null) {
      annotation = Reflections.getInheritedAnnotation(ProjectionSupport.class, method.getDeclaringClass());
    }
    String defaultProjectionCsv = annotation.defaultProjection();
    if (defaultProjectionCsv.isEmpty()) {
      return null;
    } else {
      try {
        return Csv.readRow(defaultProjectionCsv);
      } catch (CsvParseException ex) {
        throw new IllegalStateException("Invalid default projection: " + defaultProjectionCsv, ex);
      }
    }
  }

  @Override
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  public void filter(final ContainerRequestContext requestContext,
          final ContainerResponseContext responseContext) {
    MultivaluedMap<String, String> qp = requestContext.getUriInfo().getQueryParameters();
    String select = qp.getFirst("_project");
    List<String> projection;
    if (select == null) {
      List<String> defaultProjection = getDefaultProjection(resourceInfoProvider.get());
      if (defaultProjection == null) {
        return;
      } else {
        projection = defaultProjection;
      }
    } else {
      try {
        projection = Csv.readRow(select);
      } catch (CsvParseException ex) {
        throw new ClientErrorException("Invalid projection " + select, 400, ex);
      }
    }
    Object responseObject = responseContext.getEntity();
    LOG.debug("Projecting: {} entity: {}", select, responseObject);
    Schema sourceSchema = MessageBodyRWUtils.getAvroSchemaFromType(responseContext.getEntityClass(),
            responseContext.getEntityType(), responseObject, responseContext.getEntityAnnotations());
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
    if (responseObject instanceof Closeable) {
      cl = (Closeable) responseObject;
    } else {
      cl = () -> { };
    }
    int bufferSize;
    if (responseObject instanceof Buffered) {
      bufferSize = ((Buffered) responseObject).getElementBufferSize();
    } else {
      bufferSize = 64;
    }
    if (responseObject instanceof Iterable) {
      Iterable<? extends IndexedRecord> entity = (Iterable<? extends IndexedRecord>) responseObject;
      IterableArrayContent<IndexedRecord> projected = IterableArrayContent.from(Iterables.transform(entity,
              (x) -> Schemas.project(resultSchema, elementType, x)), cl, bufferSize,
              resultSchema);
      responseContext.setEntity(projected);
    } else if (responseObject instanceof StreamingArrayContent) {
      StreamingArrayContent<IndexedRecord> toWrap = (StreamingArrayContent<IndexedRecord>) responseObject;
      StreamingArrayContent<IndexedRecord> projected = toWrap.project(resultSchema, elementType, bufferSize);
      responseContext.setEntity(projected);
    } else {
      throw new IllegalStateException("Response type cannot be projected " + responseObject);
    }
  }

  @Override
  public String toString() {
    return "ProjectionJaxRsFilter{" + "defaultProjection=" + resourceInfoProvider + '}';
  }

}
