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
package org.spf4j.jaxrs.server.providers.msg_rw;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlExplainFormat;
import org.apache.calcite.sql.SqlExplainLevel;
import org.spf4j.jaxrs.common.providers.ProviderUtils;

/**
 *
 * @author Zoltan Farkas
 */
@Provider
@Produces("text/plain")
public final class PlanTextMessageBodyWriter implements MessageBodyWriter<RelNode> {

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType) {
    return RelNode.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(final RelNode t, final Class<?> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
          final OutputStream entityStream) throws IOException {
    String dumpPlan = RelOptUtil.dumpPlan("", t, SqlExplainFormat.TEXT, SqlExplainLevel.ALL_ATTRIBUTES);
    entityStream.write(dumpPlan.getBytes(ProviderUtils.getCharset(mediaType)));
  }

}
