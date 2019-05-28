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
package org.spf4j.actuator.apiBrowser;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.spf4j.base.avro.ServiceError;

/**
 * @author Zoltan Farkas
 */
public final class DefaultAspectsApiFilter extends AbstractSpecFilter {


  @Override
  public Optional<OpenAPI> filterOpenAPI(final OpenAPI openAPI, final Map<String, List<String>> params,
          final Map<String, String> cookies, final Map<String, List<String>> headers) {
    openAPI.getComponents().getSchemas().putAll(ModelConverters.getInstance().readAll(ServiceError.class));
    return Optional.of(openAPI);
  }

  @Override
  public Optional<Operation> filterOperation(final Operation operation, final ApiDescription api,
          final Map<String, List<String>> params, final Map<String, String> cookies,
          final Map<String, List<String>> headers) {
    ApiResponses responses = operation.getResponses();
    responses.addApiResponse("error", new ApiResponse().content(new Content().addMediaType("application/json",
            new MediaType().schema(new Schema().$ref("#/components/schemas/"
                    + ServiceError.getClassSchema().getFullName())))));
    return Optional.of(operation);
  }

}
