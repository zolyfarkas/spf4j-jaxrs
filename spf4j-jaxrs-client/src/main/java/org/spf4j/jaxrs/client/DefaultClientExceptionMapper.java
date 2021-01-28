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
package org.spf4j.jaxrs.client;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.Throwables;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.service.avro.ServiceError;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.base.avro.StackSamples;
import org.spf4j.http.Headers;
import static org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol.CONTENT_TYPE_AVRO_SCHEMA_PARAM;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.log.Level;
import org.spf4j.ssdump2.Converter;

/**
 *
 * @author Zoltan Farkas
 */
public final class DefaultClientExceptionMapper implements ClientExceptionMapper {

  private static final ExecContextLogger LOG
          = new ExecContextLogger(LoggerFactory.getLogger(DefaultClientExceptionMapper.class));

  public static final ClientExceptionMapper INSTANCE = new DefaultClientExceptionMapper();



  public Exception handleServiceError(final Exception e,
          final ExecutionContext current) {
    Throwable rex = com.google.common.base.Throwables.getRootCause(e);
    if (!(rex instanceof WebApplicationException)) {
      return e;
    }
    final WebApplicationException ex = (WebApplicationException) rex;
    Response response = ex.getResponse();
    String schemaStr = null;
    MultivaluedMap<String, Object> headers = response.getHeaders();
    Object mto = headers.getFirst(HttpHeaders.CONTENT_TYPE);
    if (mto != null) {
      MediaType mediaType = MediaType.valueOf(mto.toString());
      schemaStr = mediaType.getParameters().get(CONTENT_TYPE_AVRO_SCHEMA_PARAM);
    }
    if (schemaStr == null) { // for backward compattibility.
      Object so = headers.getFirst(Headers.CONTENT_SCHEMA);
      if (so != null) {
        schemaStr = so.toString();
      }
    }
    if (schemaStr == null) {
      return e;
    }
    ServiceError se;
    try {
      se = response.readEntity(ServiceError.class);
    } catch (RuntimeException x) {
      // not a Propagable service error.
      Throwables.suppressLimited(ex, x);
      return x;
    }
    LOG.debug("ServiceError: {}", se.getMessage());
    DebugDetail detail = se.getDetail();
    Throwable rootCause = null;
    if (detail != null) {
      org.spf4j.base.avro.Throwable throwable = detail.getThrowable();
      String origin = detail.getOrigin();
      if (throwable != null) {
        rootCause = Converters.convert(origin, throwable);
      }
      if (current != null) {
        for (LogRecord log : detail.getLogs()) {
          if (log.getOrigin().isEmpty()) {
            log.setOrigin(origin);
          }
          LOG.log(current, Level.DEBUG, log);
        }
        List<StackSampleElement> stackSamples = detail.getStackSamples();
        if (!stackSamples.isEmpty()) {
          LOG.debug("remoteProfileDetail", new StackSamples(stackSamples));
          current.add(Converter.convert(stackSamples.iterator()));
        }
      }
    }
    WebApplicationException nex = new WebApplicationException(rootCause,
            Response.fromResponse(response).entity(se).build());
    nex.setStackTrace(ex.getStackTrace());
    return nex;
  }

}
