package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.io.Decoder;
import org.spf4j.io.Csv;
import org.spf4j.avro.csv.CsvDecoder;
import org.spf4j.io.MemorizingBufferedInputStream;
import org.spf4j.io.csv.CsvParseException;
import org.spf4j.io.csv.CsvReader;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"text/csv;fmt=avro"})
public final class CsvAvroMessageBodyReader extends AvroMessageBodyReader {

  @Inject
  public CsvAvroMessageBodyReader(final SchemaProtocol protocol) {
    super(protocol);
  }

  @Override
  public DecodedSchema tryDecodeSchema(final Schema readerSchema, final InputStream is, final Annotation[] annotations)
          throws IOException {
    return tryDecodeSchema(is, readerSchema);
  }

  public static DecodedSchema tryDecodeSchema(final InputStream is, final Schema readerSchema)
          throws IOException {
    try {
      CsvReader reader = Csv.CSV.reader(new InputStreamReader(is, StandardCharsets.UTF_8));
      SchemaBuilder.FieldAssembler<Schema> fields = SchemaBuilder.record("DynCsv")
              .fields();
      if (readerSchema == null) {
        reader.readRow((cs) -> fields.requiredString(cs.toString()));
      } else {
        reader.readRow((cs) -> {
          String name = cs.toString();
          //todo should consider field aliasses.
          Schema.Field field = readerSchema.getField(name);
          if (field == null) {
            fields.requiredString(name);
          } else {
            fields.name(name).type(field.schema()).noDefault();
          }
        });
      }
      Schema schema = fields.endRecord();
      return new DecodedSchema(schema, new CsvDecoder(reader, schema));
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
  }




  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    CsvReader reader = Csv.CSV.reader(new InputStreamReader(is, StandardCharsets.UTF_8));
    try {
      reader.skipRow(); // skip headers
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
    return new CsvDecoder(reader, writerSchema);
  }

  public InputStream wrapInputStream(final InputStream pentityStream) {
    return new MemorizingBufferedInputStream(pentityStream, StandardCharsets.UTF_8);
  }

}
