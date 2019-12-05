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
package org.spf4j.jaxrs.aql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.Reader;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.avro.Schema;
import org.apache.calcite.rel.RelNode;

/**
 * REST avro sql endpoint.
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("JAXRS_ENDPOINT")
public interface AvroQueryResource {


  /**
   * Execute a SQL query against the available resources(tables)
   * @param query the query string
   * @param secCtx the security context.
   * @return query result-set.
   */
  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  @Operation(
         description = "Run a SQL query, for a list of queryable entities please see schemas endpoint",
         responses = {
           @ApiResponse(
                 description = "Return a resultset (array of objects)",
                 responseCode = "200",
                 content = @Content(array = @ArraySchema(schema
                                = @io.swagger.v3.oas.annotations.media.Schema(implementation = Object.class))
                         ))
         }
  )
  Response query(
          @Parameter(name = "query", in = ParameterIn.QUERY,
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class),
            description = "sql select statement", example = "select a,b,c from t where ...")
          @QueryParam("query") String query,
          @Context SecurityContext secCtx);


  /**
   * Execute a SQL query against the available resources(tables)
   * @param query the query string
   * @param secCtx the security context.s
   * @return query result-set.
   */
  @POST
  @Produces({"application/json", "application/avro+json", "application/avro"})
  @Consumes("text/plain")
  @Operation(
         description = "Run a SQL query, for a list of queryable entities please see schemas endpoint",
         requestBody = @RequestBody(content = @Content(
                 examples = @ExampleObject(value = "select a,b,c from t where ..."),
                 schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class))),
         responses = {
           @ApiResponse(
                 description = "Return a resultset (array of objects)",
                 responseCode = "200",
                 content = @Content(array = @ArraySchema(schema
                                = @io.swagger.v3.oas.annotations.media.Schema(implementation = Object.class))
                         ))
         }
  )
  Response query(
          Reader query,
          @Context SecurityContext secCtx);


  /**
   * Retrieve the query plan for the executed query.
   * @param query the query string.
   * @param secCtx the security context.
   * @return query execution plan.
   */
  @GET
  @Path("plan")
  @Produces({"text/plain", "application/json"})
  RelNode plan(@QueryParam("query") String query, @Context SecurityContext secCtx);

  /**
   * Retrieve the query plan for the executed query.
   * @param query the query string.
   * @param secCtx he security context.
   * @return query execution plan.
   */
  @POST
  @Path("plan")
  @Produces({"text/plain", "application/json"})
  @Consumes("text/plain")
  RelNode plan(Reader query, @Context SecurityContext secCtx);


  /**
   * Retrieve the schema of the result-set returned by the query.
   * @param query the query string.
   * @param secCtx the security context.
   * @return avro schema of the result-set.
   */
  @GET
  @Path("schema")
  @Produces({"application/json"})
  Schema schema(@QueryParam("query") String query, @Context SecurityContext secCtx);

  /**
   * Retrieve the schema of the result-set returned by the query.
   * @param query the query string.
   * @param secCtx the security context.
   * @return avro schema of the result-set.
   */
  @POST
  @Path("schema")
  @Produces({"application/json"})
  @Consumes("text/plain")
  Schema schema(Reader query, @Context SecurityContext secCtx);


  /**
   * Get the table schemas available for query.
   * @param secCtx the security context.
   * @return a map of avro schemas.
   */
  @GET
  @Path("schemas")
  @Produces({"application/json"})
  Map<String, Schema> schemas(@Context SecurityContext secCtx);

  /**
   * Get a specific table schema.
   * @param entityName the table name.
   * @param secCtx the security context.
   * @return avro schema of the table.
   */
  @GET
  @Path("schemas/{entityName}")
  @Produces({"application/json"})
  Schema entitySchema(@PathParam("entityName") String entityName, @Context SecurityContext secCtx);

}
