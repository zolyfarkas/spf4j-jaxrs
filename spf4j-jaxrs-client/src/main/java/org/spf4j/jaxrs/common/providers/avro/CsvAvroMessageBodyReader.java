package org.spf4j.jaxrs.common.providers.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.glassfish.jersey.internal.guava.Maps;
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
      CsvReader reader = Csv.CSV.readerILEL(new InputStreamReader(is, StandardCharsets.UTF_8));
      Schema record = Schema.createRecord("DynCsv", "Infered schema", "org.spf4j.avro", false);
      List<Schema.Field> bfields = new ArrayList<>();
      if (readerSchema == null) {
        reader.readRow((cs) -> {
          bfields.add(new Schema.Field(validateName(cs), Schema.create(Schema.Type.STRING),
                  cs.toString(), (Object) null));
        });
      } else {
        Schema elementType = readerSchema.getElementType();
        List<Schema.Field> fields = elementType.getFields();
        Map<String, Schema.Field> fieldMap = Maps.newHashMapWithExpectedSize(fields.size() + 2);
        for (Schema.Field field : fields) {
          fieldMap.put(field.name(), field);
          for (String alias : field.aliases()) {
            fieldMap.put(alias, field);
          }
        }
        reader.readRow((cs) -> {
          String validatedName = validateName(cs);
          Schema.Field field = fieldMap.get(validatedName);
          if (field == null) {
             bfields.add(new Schema.Field(validatedName, Schema.create(Schema.Type.STRING),
                     cs.toString(), (Object) null));
          } else {
            bfields.add(new Schema.Field(validatedName, field.schema(), cs.toString(), (Object) null));
          }
        });
      }
      record.setFields(bfields);
      Schema arraySchema = Schema.createArray(record);
      return new DecodedSchema(arraySchema, new CsvDecoder(reader, arraySchema));
    } catch (CsvParseException ex) {
      throw new RuntimeException(ex);
    }
  }




  @Override
  public Decoder getDecoder(final Schema writerSchema, final InputStream is) throws IOException {
    CsvReader reader = Csv.CSV.readerILEL(new InputStreamReader(is, StandardCharsets.UTF_8));
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


  private static String validateName(final CharSequence name) {
    int length = name.length();
    if (length == 0) {
      return "_";
    }
    StringBuilder result = null;
    char first = name.charAt(0);
    if (first != '_' && !Character.isLetter(first)) {
      result = new StringBuilder(length);
      result.append('_');
    }
    for (int i = 1; i < length; i++) {
      char c = name.charAt(i);
      if (c != '_' && !Character.isLetterOrDigit(c)) {
        if (result == null) {
           result = new StringBuilder(length);
           result.append(name, 0, i);
        }
        result.append('_');
      } else  if (result != null) {
        result.append(c);
      }
    }
    return result == null ? name.toString() : result.toString();
  }

}
