# spf4j-jaxrs
Collection of JAX-RS features and utilities.

Components:

 1. **spf4j-jaxrs-client** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-client/)

  * Avro schema client. (retrieve avro schemas from maven repositories)
  * JAX-RS client with retries + hedged execution, timeout propagation.
  * json/binary/csv avro based message body readers and writers.

 2. **spf4j-jaxrs-server** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-server/)

  * Distributed Stack Traces.
  * Debug logs on Error.
  * Access logging.
  * Configuration injector.
  * Header overwrite via Query parameters.
  * Context aware continuous profiling.

 3. **spf4j-jaxrs-kube** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-kube/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-kube/)
   
  * tiny kubernetes client for membership discovery.

 4. **spf4j-jaxrs-codegen-extension** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-codegen-extension/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-codegen-extension/)

  * JAX-RS annotations support in avro schemas.

 5. **spf4j-jaxrs-actuator** [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-actuator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-jaxrs-actuator/)

  * info endpoint
  * health endpoint
  * logs endpoint.

