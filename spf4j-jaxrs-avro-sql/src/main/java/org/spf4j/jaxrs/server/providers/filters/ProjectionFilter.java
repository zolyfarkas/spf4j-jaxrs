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


import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.jaxrs.ProjectionSupport;

/**
 * @author Zoltan Farkas
 */
@ProjectionSupport
@Provider
@Priority(Priorities.ENTITY_CODER)
public final class ProjectionFilter implements ContainerResponseFilter {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectionFilter.class);

  @Override
  public void filter(final ContainerRequestContext requestContext,
          final ContainerResponseContext responseContext) {
    MultivaluedMap<String, String> qp = requestContext.getUriInfo().getQueryParameters();
    String select = qp.getFirst("_select");
    Object entity = responseContext.getEntity();
    LOG.debug("Projecting: {} entity: {}", select, entity);
  }

}
