
package org.spf4j.jaxrs.common.avro;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.codehaus.jackson.JsonParser;
import org.spf4j.avro.SchemaClient;

/**
 * registers all avro message body readers and writers + parameter converters.
 * @author Zoltan Farkas
 */
public final class AvroFeature implements Feature {

  static {
    org.apache.avro.Schema.FACTORY.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
  }

  private final SchemaClient client;

  @Inject
  public AvroFeature(final SchemaClient client) {
    this.client = client;
  }



  @Override
  public boolean configure(final FeatureContext context) {
    context.register(new JsonAvroMessageBodyReader(client));
    context.register(new JsonAvroMessageBodyWriter(client));
    context.register(new XJsonAvroMessageBodyReader(client));
    context.register(new XJsonAvroMessageBodyWriter(client));
    context.register(new CsvAvroMessageBodyReader(client));
    context.register(new CsvAvroMessageBodyWriter(client));
    context.register(new BinaryAvroMessageBodyReader(client));
    context.register(new BinaryAvroMessageBodyWriter(client));
    context.register(new SchemaMessageBodyReader());
    context.register(new SchemaMessageBodyWriter());
    context.register(new AvroParameterConverterProvider(client));
    return true;
  }

  @Override
  public String toString() {
    return "AvroFeature{" + "client=" + client + '}';
  }

}
