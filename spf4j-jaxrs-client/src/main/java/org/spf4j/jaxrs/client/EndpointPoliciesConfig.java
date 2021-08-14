/*
 * Copyright 2021 SPF4J.
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.ws.rs.core.Configuration;
import org.spf4j.avro.Configs;
import org.spf4j.avro.SchemaResolver;
import org.spf4j.base.Either;
import org.spf4j.base.ResultMatchers;

import org.spf4j.jaxrs.config.ExtendedConfig;
import org.spf4j.jaxrs.config.ObservableSupplier;

import org.spf4j.service.avro.EndpointPolicyRule;
import org.spf4j.service.avro.EndpointRequestMatcher;
import org.spf4j.service.avro.EndpointsPolicies;
import org.spf4j.service.avro.HttpExecutionPolicy;

/**
 * Class that abstracts the configuration of HTTP endpoint policies.
 *
 * @author Zoltan Farkas
 */
public final class EndpointPoliciesConfig {

  public static final String CONFIG_NAME = "http.endpoint.policies";

  public static final String RESULT_MATCHERS = "http.operationResultPatterns";

  private final ObservableSupplier<EndpointsPolicies> policies;

  private final SchemaResolver schemaResolver;

  private final ExtendedConfig config;

  private final ObservableSupplier<ResultMatchers.Supplier> patternConfigSupp;

  /**
   * Multiple matchers can apply to a particular request.
   *
   * for example the following will match everything:
   *
   * {
   * "host: : ".*" "port": -1 "path" : ".*" "methods" : [] }
   *
   * and we might use it to set defaults for everything, and choose to overwrite things at service level:
   *
   * {
   * "host: : "my-service\\.prod\\.svc\\.cluster\\.local" "port": -1 "path" : ".*" "methods" : [] }
   *
   * or
   *
   * {
   * "host: : ".*" "port" : 9090 "path" : ".*" "methods" : ["GET"] }
   *
   * The order of definition will matter since the HttpExecutionPolicies can be partial, and values not provided will
   * fall back to the next matching rule.
   *
   * Matchers will reference an execution policy by config name.
   *
   */
  public EndpointPoliciesConfig(final Configuration configuration, final SchemaResolver schemaResolver) {
    this((ExtendedConfig) configuration.getProperty(ExtendedConfig.PROPERTY_NAME), schemaResolver);
  }

  public EndpointPoliciesConfig(final ExtendedConfig configuration, final SchemaResolver schemaResolver) {
    this.config = configuration;
    this.policies
            = this.config.getObservableValueSupplier(CONFIG_NAME, EndpointsPolicies.class, "{\"policies\":[]}", false);
    this.schemaResolver = schemaResolver;
    this.patternConfigSupp
            = this.config.getObservableValueSupplier(RESULT_MATCHERS,  null, true,
                    confStr -> {
                      Map<String, Either<Predicate<Throwable>, Predicate<Object>>> map
                      = ResultMatchers.operationfromConfigValue(confStr);
                      return new SupplierImpl(map).chain(ResultMatchers.toSupplier());
                    });
  }

  private static boolean matches(final Map<String, Pattern> patterns, final Map<String, List<String>> values) {
    if (patterns.isEmpty()) {
      return true;
    }
    for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
      List<String> headerValues = values.get(entry.getKey());
      boolean matchedOneHeaderValue = false;
      for (String value : headerValues) {
        if (entry.getValue().matcher(value).matches()) {
          matchedOneHeaderValue = true;
          break;
        }
      }
      if (!matchedOneHeaderValue) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public HttpExecutionPolicy getHttpExecutionPolicy(final String hostName,
          final int port,
          final String path,
          final String method,
          final Map<String, List<String>> headers,
          final Map<String, List<String>> queryParams) {
    List<String> execPolifyRefs = new ArrayList<>(2);
    EndpointsPolicies ep = this.policies.get();
    for (EndpointPolicyRule rule : ep.getPolicies()) {
      EndpointRequestMatcher matcher = rule.getMatcher();
      if (!matcher.getHost().matcher(hostName).matches()) {
        continue;
      }
      int mport = matcher.getPort();
      if (mport > 0 && mport != port) {
        continue;
      }
      if (!matcher.getPath().matcher(path).matches()) {
        continue;
      }
      List<String> methods = matcher.getMethods();
      if ((methods.isEmpty() || methods.contains(method))
              && matches(matcher.getHeaders(), headers)
              && matches(matcher.getQueryParameters(), queryParams)) {
        execPolifyRefs.add(rule.getPolicies().getHttpExecutionPolicyRef());
      }
    }
    int size = execPolifyRefs.size();
    Reader[] configs = new Reader[size];
    int k = 0;
    for (String ref : execPolifyRefs) {
      String derefValue = this.config.getValue(ref, String.class);
      if (derefValue == null) {
        Logger log = Logger.getLogger(EndpointPoliciesConfig.class.getName());
        log.log(Level.SEVERE, "Referenced Value: {0} does not exist, ignoring it", ref);
        configs = java.util.Arrays.copyOf(configs, configs.length - 1);
      } else {
        configs[k++] = new StringReader(derefValue);
      }
    }
    try {
      return Configs.read(HttpExecutionPolicy.class, this.schemaResolver, configs);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public ResultMatchers.Supplier toResultMatcherSupplier() {
    return this.patternConfigSupp.get();
  }

  @Override
  public String toString() {
    return "EnpointPoliciesConfig{" + "policies=" + policies + ", schemaResolver="
            + schemaResolver + ", config=" + config + '}';
  }

  private static class SupplierImpl implements ResultMatchers.Supplier {

    private final Map<String, Either<Predicate<Throwable>, Predicate<Object>>> map;

    SupplierImpl(final Map<String, Either<Predicate<Throwable>, Predicate<Object>>> map) {
      this.map = map;
    }

    public Either<Predicate<Throwable>, Predicate<Object>> apply(final String matcherName) {
      return map.get(matcherName);
    }
  }

}
