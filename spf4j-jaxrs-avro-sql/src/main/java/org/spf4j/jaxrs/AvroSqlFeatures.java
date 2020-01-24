/*
 * Copyright 2020 SPF4J.
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
package org.spf4j.jaxrs;

import com.google.common.annotations.Beta;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.spf4j.jaxrs.aql.AvroQueryResourceImpl;
import org.spf4j.jaxrs.aql.server.providers.filters.SqlFilterJaxRsFilter;
import org.spf4j.jaxrs.aql.server.providers.msg_rw.PlanJsonMessageBodyWriter;
import org.spf4j.jaxrs.aql.server.providers.msg_rw.PlanTextMessageBodyWriter;
import org.spf4j.jaxrs.aql.server.providers.param_converters.SqlRowPredicateParameterConverterProvider;

/**
 * Avro SQL features.
 *
 * Sql query endpoint.
 * A JAX-RS filter implementation to filter any result-set via a SQL where predicate.
 * Execution plan serializers.
 * SqlPredicate parameter converter..
 *
 * @author Zoltan Farkas
 */
@Beta
public final class AvroSqlFeatures implements Feature {

  @Override
  public boolean configure(final FeatureContext fc) {
    fc.register(AvroQueryResourceImpl.class);
    fc.register(SqlFilterJaxRsFilter.class);
    fc.register(PlanJsonMessageBodyWriter.class);
    fc.register(PlanTextMessageBodyWriter.class);
    fc.register(SqlRowPredicateParameterConverterProvider.class);
    return true;
  }

}
