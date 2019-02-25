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
package org.spf4j.jaxrs.server;

import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.spf4j.jaxrs.ConfigProperty;

/**
 * @author Zoltan Farkas
 */
@Provider
@PreMatching
public final class HeaderOverwriteRequestFilter implements ContainerRequestFilter {

  private final String prefix;

  public HeaderOverwriteRequestFilter(@ConfigProperty(value = "spf4j.jaxrs.qpHeaderOverwritePrefix")
          @DefaultValue("_") final String prefix) {
    this.prefix = prefix;
  }

  public void filter(final ContainerRequestContext ctx) {
    if (prefix.isEmpty()) {
      return;
    }
    MultivaluedMap<String, String> queryParameters = ctx.getUriInfo().getQueryParameters();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      String qp = entry.getKey();
      if (qp.startsWith(prefix)) {
        ctx.getHeaders().put(qp.substring(prefix.length()), entry.getValue());
      }
    }
  }

  @Override
  public String toString() {
    return "HeaderOverwriteRequestFilter{" + "prefix=" + prefix + '}';
  }

}
