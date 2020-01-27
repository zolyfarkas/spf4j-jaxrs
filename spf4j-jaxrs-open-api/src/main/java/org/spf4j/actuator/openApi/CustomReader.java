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

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import org.spf4j.jaxrs.ProjectionSupport;

/**
 * Reader to manipulate OpenApi model and add Annotation based aspects.
 * @author Zoltan Farkas
 */
public class CustomReader extends Reader {

  public CustomReader() {
  }

  public CustomReader(final OpenAPI openAPI) {
    super(openAPI);
  }

  public CustomReader(final OpenAPIConfiguration openApiConfiguration) {
    super(openApiConfiguration);
  }

  /**
   * Intercept to process extra annotations, etc.
   * @return
   */
  @Override
  @SuppressWarnings("checkstyle:ParameterNumber")
  protected Operation parseMethod(final Class<?> cls,
          final Method method,
          final List<Parameter> globalParameters,
          final Produces methodProduces,
          final Produces classProduces,
          final Consumes methodConsumes,
          final Consumes classConsumes,
          final List<SecurityRequirement> classSecurityRequirements,
          final Optional<ExternalDocumentation> classExternalDocs,
          final Set<String> classTags,
          final List<Server> classServers,
          final boolean isSubresource,
          final RequestBody parentRequestBody,
          final ApiResponses parentResponses,
          final JsonView jsonViewAnnotation,
          final ApiResponse[] classResponses,
          final AnnotatedMethod annotatedMethod) {
    Operation operation = super.parseMethod(cls, method, globalParameters,
            methodProduces, classProduces, methodConsumes, classConsumes,
            classSecurityRequirements, classExternalDocs, classTags, classServers,
            isSubresource, parentRequestBody, parentResponses, jsonViewAnnotation,
            classResponses, annotatedMethod);
    ProjectionSupport projections = method.getAnnotation(ProjectionSupport.class);
    if (projections != null) {
      QueryParameter qp = new QueryParameter();
      qp.name("_project");
      qp.setSchema(new StringSchema());
      qp.setRequired(false);
      qp.setDescription("A comma separated list of field names or field paths");
      List<Parameter> parameters = operation.getParameters();
      if (parameters == null) {
        parameters = new ArrayList<>(2);
        operation.setParameters(parameters);
      }
      parameters.add(qp);
    }
    return operation;
  }

}
