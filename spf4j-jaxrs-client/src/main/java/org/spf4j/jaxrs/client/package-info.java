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
 *
 * This package conatins a JAX-RS client extended to include configurable
 * execution policies.
 * Execution policy configurations will be lookep up from:
 *
 * http.endpoint.policies -> EndpointsPolicies
 * this endpoint matcher 2 execution policy mapping is kept at client level.
 * when creating a web target this mapping will be used to lookup the web target's execution policy.
 *
 */
package org.spf4j.jaxrs.client;
