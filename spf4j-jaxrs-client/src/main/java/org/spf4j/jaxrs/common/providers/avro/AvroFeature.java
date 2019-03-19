
package org.spf4j.jaxrs.common.providers.avro;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.apache.avro.SchemaResolver;
import org.spf4j.jaxrs.common.providers.avro.stream.CsvAvroArrayMessageBodyReader;

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
    context.register(new CsvAvroArrayMessageBodyReader(protocol));
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
