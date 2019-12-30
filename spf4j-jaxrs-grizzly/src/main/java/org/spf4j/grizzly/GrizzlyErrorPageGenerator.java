package org.spf4j.grizzly;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;
import org.spf4j.avro.SchemaClient;
import org.spf4j.base.Arrays;
import org.spf4j.base.Throwables;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.common.providers.avro.XJsonAvroMessageBodyWriter;

/**
 *
 * @author Zoltan Farkas
 */
public final class GrizzlyErrorPageGenerator implements ErrorPageGenerator {

  private final SchemaClient schemaClient;

  public GrizzlyErrorPageGenerator(final SchemaClient schemaClient) {
    this.schemaClient = schemaClient;
  }



  @Override
  public String generate(final Request request, final int status,
          final String reasonPhrase, final String description, final Throwable exception) {
    ServiceError err = ServiceError.newBuilder()
            .setCode(status)
            .setMessage(reasonPhrase + ';' + description)
            .setDetail(new DebugDetail("origin", Collections.EMPTY_LIST,
                    exception != null ? Converters.convert(exception) : null, Collections.EMPTY_LIST))
            .build();
    ByteArrayBuilder bab = new ByteArrayBuilder(256);
    XJsonAvroMessageBodyWriter writer = new XJsonAvroMessageBodyWriter(new DefaultSchemaProtocol(schemaClient));
    try {
      writer.writeTo(err, err.getClass(), err.getClass(),
              Arrays.EMPTY_ANNOT_ARRAY, MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(2),
              bab);
    } catch (RuntimeException ex) {
      if (exception != null) {
        Throwables.suppressLimited(ex, exception);
      }
      Logger.getLogger(reasonPhrase).log(Level.SEVERE, "RuntimeException while writing detail", ex);
      throw ex;
    } catch (IOException ex) {
      if (exception != null) {
        Throwables.suppressLimited(ex, exception);
      }
      Logger.getLogger(reasonPhrase).log(Level.SEVERE, "Exception while writing detail", ex);
      throw new UncheckedIOException(ex);
    }
    return bab.toString(StandardCharsets.UTF_8);
  }
}
