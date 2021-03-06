package org.spf4j.jaxrs.client;

import java.net.URI;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.spf4j.failsafe.concurrent.FailSafeExecutor;

/**
 * @author Zoltan Farkas
 */
public final class Spf4jWebTarget implements WebTarget {

  private FailSafeExecutor executor;

  private final WebTarget tg;

  private final Spf4JClient client;

  public Spf4jWebTarget(final Spf4JClient client,
          final WebTarget tg, final FailSafeExecutor executor) {
    this.tg = tg;
    this.client = client;
    this.executor = executor;
  }

  public Spf4JClient getClient() {
    return client;
  }

  @Override
  public URI getUri() {
    return tg.getUri();
  }

  @Override
  public UriBuilder getUriBuilder() {
    return tg.getUriBuilder();
  }

  @Override
  public Spf4jWebTarget path(final String path) {
    return new Spf4jWebTarget(client, tg.path(path), executor);
  }

  @Override
  public Spf4jWebTarget resolveTemplate(final String name, final Object value) {
    return new Spf4jWebTarget(client, tg.resolveTemplate(name, value), executor);
  }

  @Override
  public Spf4jWebTarget resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
    return new Spf4jWebTarget(client, tg.resolveTemplate(name, value, encodeSlashInPath), executor);
  }

  @Override
  public Spf4jWebTarget resolveTemplateFromEncoded(final String name, final Object value) {
    return new Spf4jWebTarget(client, tg.resolveTemplateFromEncoded(name, value), executor);
  }

  @Override
  public Spf4jWebTarget resolveTemplates(final Map<String, Object> templateValues) {
    if (templateValues.isEmpty()) {
      return this;
    }
    return new Spf4jWebTarget(client, tg.resolveTemplates(templateValues), executor);
  }

  @Override
  public Spf4jWebTarget resolveTemplates(final Map<String, Object> templateValues, final boolean encodeSlashInPath) {
    if (templateValues.isEmpty()) {
      return this;
    }
    return new Spf4jWebTarget(client, tg.resolveTemplates(templateValues, encodeSlashInPath), executor);
  }

  @Override
  public Spf4jWebTarget resolveTemplatesFromEncoded(final Map<String, Object> templateValues) {
    if (templateValues.isEmpty()) {
      return this;
    }
    return new Spf4jWebTarget(client, tg.resolveTemplatesFromEncoded(templateValues), executor);
  }

  @Override
  public Spf4jWebTarget matrixParam(final String name, final Object... values) {
    return new Spf4jWebTarget(client, tg.matrixParam(name,
            Spf4JClient.convert(Spf4JClient.getParamConverters(getConfiguration()), values)),
            executor);
  }

  @Override
  public Spf4jWebTarget queryParam(final String name, final Object... values) {
    return new Spf4jWebTarget(client, tg.queryParam(name,
            Spf4JClient.convert(Spf4JClient.getParamConverters(getConfiguration()), values)),
            executor);
  }

  @Override
  public Spf4jInvocationBuilder request() {
    return new Spf4jInvocationBuilder(client, tg.request(), executor, this);
  }

  @Override
  public Spf4jInvocationBuilder request(final String... acceptedResponseTypes) {
    return new Spf4jInvocationBuilder(client, tg.request(acceptedResponseTypes), executor, this);
  }

  @Override
  public Spf4jInvocationBuilder request(final MediaType... acceptedResponseTypes) {
    return new Spf4jInvocationBuilder(client, tg.request(acceptedResponseTypes), executor, this);
  }

  @Override
  public Configuration getConfiguration() {
    return tg.getConfiguration();
  }

  @Override
  public Spf4jWebTarget property(final String name, final Object value) {
    tg.property(name, value);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Class<?> componentClass) {
    tg.register(componentClass);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Class<?> componentClass, final int priority) {
    tg.register(componentClass, priority);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Class<?> componentClass, final Class<?>... contracts) {
    tg.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
    tg.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Object component) {
    tg.register(component);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Object component, final int priority) {
    tg.register(component, priority);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Object component, final Class<?>... contracts) {
    tg.register(component, contracts);
    return this;
  }

  @Override
  public Spf4jWebTarget register(final Object component, final Map<Class<?>, Integer> contracts) {
    tg.register(component, contracts);
    return this;
  }

  @Override
  public String toString() {
    return "Spf4jWebTarget{" + "executor=" + executor + ", tg=" + tg + ", client=" + client
            + '}';
  }

}
