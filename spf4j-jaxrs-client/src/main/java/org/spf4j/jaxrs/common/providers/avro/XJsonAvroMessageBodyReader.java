package org.spf4j.jaxrs.common.providers.avro;

import org.spf4j.io.MemorizingBufferedInputStream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.ExtendedJsonDecoder;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/json;fmt=avro-x", "application/avro-x+json", "text/plain;fmt=avro-x"})
public final class XJsonAvroMessageBodyReader extends AvroMessageBodyReader {


  @Inject
  public XJsonAvroMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    return new ExtendedJsonDecoder(writerSchema, is);
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public  InputStream wrapInputStream(final InputStream pentityStream)  {
      return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8, 32768);
     //return new DebugInputStream(pentityStream, new File(org.spf4j.base.Runtime.TMP_FOLDER));
  }

}
