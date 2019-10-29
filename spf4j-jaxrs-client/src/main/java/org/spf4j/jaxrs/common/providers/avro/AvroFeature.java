
package org.spf4j.jaxrs.common.providers.avro;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.apache.avro.SchemaResolver;
import org.spf4j.jaxrs.common.providers.avro.stream.BinaryAvroIterableMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.BinaryAvroIterableMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.BinaryAvroStreamingMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.BinaryAvroStreamingMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.CsvAvroIterableMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.CsvAvroIterableMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.CsvAvroStreamingMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.CsvAvroStreamingMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.JsonAvroIterableMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.JsonAvroIterableMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.JsonAvroStreamingMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.JsonAvroStreamingMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.XJsonAvroIterableMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.XJsonAvroIterableMessageBodyWriter;
import org.spf4j.jaxrs.common.providers.avro.stream.XJsonAvroStreamingMessageBodyReader;
import org.spf4j.jaxrs.common.providers.avro.stream.XJsonAvroStreamingMessageBodyWriter;

/**
 * registers all avro message body readers and writers + parameter converters.
 * @author Zoltan Farkas
 */
public final class AvroFeature implements Feature {

  private final SchemaProtocol protocol;

  private final SchemaResolver schemaClient;

  @Inject
  public AvroFeature(final SchemaProtocol protocol, final SchemaResolver client) {
    this.protocol = protocol;
    this.schemaClient = client;
  }

  @Override
  public boolean configure(final FeatureContext context) {
    context.register(new JsonAvroMessageBodyReader(protocol));
    context.register(new JsonAvroMessageBodyWriter(protocol));
    context.register(new XJsonAvroMessageBodyReader(protocol));
    context.register(new XJsonAvroMessageBodyWriter(protocol));
    context.register(new CsvAvroMessageBodyReader(protocol));
    context.register(new CsvAvroMessageBodyWriter(protocol));
    context.register(new BinaryAvroMessageBodyReader(protocol));
    context.register(new BinaryAvroMessageBodyWriter(protocol));
    context.register(new CsvAvroIterableMessageBodyReader(protocol));
    context.register(new CsvAvroIterableMessageBodyWriter(protocol));
    context.register(new JsonAvroIterableMessageBodyReader(protocol));
    context.register(new JsonAvroIterableMessageBodyWriter(protocol));
    context.register(new XJsonAvroIterableMessageBodyReader(protocol));
    context.register(new XJsonAvroIterableMessageBodyWriter(protocol));
    context.register(new BinaryAvroIterableMessageBodyReader(protocol));
    context.register(new BinaryAvroIterableMessageBodyWriter(protocol));
    context.register(new CsvAvroStreamingMessageBodyReader(protocol));
    context.register(new JsonAvroStreamingMessageBodyReader(protocol));
    context.register(new XJsonAvroStreamingMessageBodyReader(protocol));
    context.register(new BinaryAvroStreamingMessageBodyReader(protocol));
    context.register(new XJsonAvroStreamingMessageBodyWriter(protocol));
    context.register(new JsonAvroStreamingMessageBodyWriter(protocol));
    context.register(new BinaryAvroStreamingMessageBodyWriter(protocol));
    context.register(new CsvAvroStreamingMessageBodyWriter(protocol));
    context.register(new SchemaMessageBodyReader());
    context.register(new SchemaMessageBodyWriter());
    context.register(new AvroParameterConverterProvider(schemaClient));
    return true;
  }

  @Override
  public String toString() {
    return "AvroFeature{" + "protocol=" + protocol + '}';
  }

}
