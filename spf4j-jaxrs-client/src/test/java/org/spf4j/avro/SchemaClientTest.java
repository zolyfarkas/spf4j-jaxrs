
package org.spf4j.avro;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.NotFoundException;
import org.apache.avro.Schema;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.spf4j.base.avro.DebugDetail;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.http.multi.MultiURLs;
import org.spf4j.http.Spf4jURLStreamHandlerFactoryTest;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaClientTest.class);

  @Test
  public void testSchemaClient() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://repo1.maven.org/maven2"));
    Schema schema = client.resolveSchema(DebugDetail.getClassSchema().getProp("mvnId"));
    Assert.assertEquals(DebugDetail.SCHEMA$.getName(), schema.getName());
  }

  @Test
  public void testArbitrarySchema() throws IOException, URISyntaxException {
    String mvnId = "org.spf4j.avro:core-schema:1.0.3:6";
    Files.deleteIfExists(Paths.get(org.spf4j.base.Runtime.USER_HOME,
            ".m2/repository/org/spf4j/avro/core-schema/1.0.3/core-schema-1.0.3.jar"));
    SchemaClient client = new SchemaClient(new URI("https://repo1.maven.org/maven2"));
    Schema resolveSchema = client.resolveSchema(mvnId);
    Assert.assertEquals("FileLocation", resolveSchema.getName());
  }

   @Test
  public void testCPSchema() throws IOException, URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://repo1.maven.org/maven2"));
    Schema resolveSchema = client.getFromClassPath("org.spf4j.avro:core-schema:1.0.4:6");
    Assert.assertNotNull(resolveSchema);
    Assert.assertEquals("org.spf4j.avro:core-schema:1.0.4:6", resolveSchema.getProp("mvnId"));
    resolveSchema = client.getFromClassPath("org.spf4j.avro:core-schema:0.14:6");
    Assert.assertNull(resolveSchema);
  }

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testArbitrarySchemaMulti() throws IOException, URISyntaxException, ClassNotFoundException {
    Class.forName(Spf4jURLStreamHandlerFactoryTest.class.getName());
    String mvnId = "org.spf4j.avro:core-schema:1.0.3:6";
    Files.deleteIfExists(Paths.get(org.spf4j.base.Runtime.USER_HOME,
            ".m2/repository/org/spf4j/avro/core-schema/0.2/core-schema-1.0.2.jar"));
    SchemaClient client = new SchemaClient(MultiURLs.newURL(MultiURLs.Protocol.mhttps, "https://repo1.maven.org/maven2",
            "https://repo1.maven.org/maven2").toURI());
    Schema resolveSchema = client.resolveSchema(mvnId);
    Assert.assertEquals("FileLocation", resolveSchema.getName());
    Schema resolveSchema2 = client.resolveSchema("org.spf4j.avro:core-schema:1.0.3:6");
    Assert.assertEquals("FileLocation", resolveSchema2.getName());
  }

  @Test
  public void testSchemaClient2() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://repo1.maven.org/maven2"));
    try {
      client.resolveSchema("a:b:c:r");
      Assert.fail();
    } catch (NotFoundException ex) {
      LOG.debug("Expected exception", ex);
    }
    try {
      client.resolveSchema("a:b:c:r");
    } catch (NotFoundException ex) {
      Assert.assertThat(ex.getMessage(), Matchers.containsString("re-attempt"));
      LOG.debug("Expected exception", ex);
    }
  }

}
