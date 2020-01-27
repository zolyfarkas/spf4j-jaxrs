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
package org.spf4j.actuator.openApi;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.http.Headers;
import org.spf4j.log.Level;

/**
 * @author Zoltan Farkas
 */
public final class DefaultAspectsApiFilter extends AbstractSpecFilter {

  @Override
  public Optional<OpenAPI> filterOpenAPI(final OpenAPI openAPI, final Map<String, List<String>> params,
          final Map<String, String> cookies, final Map<String, List<String>> headers) {
    openAPI.getComponents().getSchemas().putAll(ModelConverters.getInstance().readAll(ServiceError.class));
    Paths np = new Paths();
    openAPI.getPaths().entrySet().stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(entry -> np.put(entry.getKey(), entry.getValue()));
    openAPI.setPaths(np);
    return Optional.of(openAPI);
  }

  @Override
  public Optional<Operation> filterOperation(final Operation operation, final ApiDescription api,
          final Map<String, List<String>> params, final Map<String, String> cookies,
          final Map<String, List<String>> headers) {
    ApiResponses responses = operation.getResponses();
    List<Parameter> parameters = operation.getParameters();
    if (parameters == null) {
      parameters = new ArrayList<>(4);
      operation.setParameters(parameters);
    }
    parameters.add(new Parameter().required(Boolean.FALSE).name(Headers.REQ_TIMEOUT).in("header")
            .description("request-timeout → TimeoutValue TimeoutUnit?\n"
                    + " TimeoutValue → {positive integer as ASCII string of at most 8 digits}\n"
                    + " TimeoutUnit → Hour / Minute / Second / Millisecond / Microsecond / Nanosecond\n"
                    + " Hour → \"H\"\n"
                    + " Minute → \"M\"\n"
                    + " Second → \"S\"\n"
                    + " Millisecond → \"m\"\n"
                    + " Microsecond → \"u\"\n"
                    + " Nanosecond → \"n\"")
            .schema(PrimitiveType.STRING.createProperty()));
    parameters.add(new Parameter().description("Request context log level")
            .required(Boolean.FALSE).name(Headers.CTX_LOG_LEVEL).in("header")
            .schema(ModelConverters.getInstance().readAllAsResolvedSchema(Level.class).schema));
    parameters.add(new Parameter().description("Request id, if not provided server will generate one")
            .required(Boolean.FALSE).name(Headers.REQ_ID).in("header")
            .schema(PrimitiveType.STRING.createProperty().description("Request ID")));
    responses.addApiResponse("500", new ApiResponse().description("standard error response")
            .content(new Content().addMediaType("application/json",
            new MediaType().
                    schema(new Schema().$ref("#/components/schemas/"
                            + ServiceError.getClassSchema().getFullName())))));
    return Optional.of(operation);
  }

}
