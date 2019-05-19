/*
 * Copyright 2019 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.actuator.jmx;

import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.ExtendedReflectData;
import org.spf4j.base.Reflections;
import org.spf4j.base.SuppressForbiden;

/**
 * Convert to-from OpenType.ALLOWED_CLASSNAMES_LIST and Avro Objects.
 *
 * @author Zoltan Farkas
 */
public interface OpenTypeAvroConverter<T extends OpenType, A, C> {

  @Nullable
  A fromOpenValue(T type, @Nullable C openValue, OpenTypeConverterSupplier convSupp);

  @Nullable
  C toOpenValue(T type, @Nullable A value, OpenTypeConverterSupplier convSupp);

  @Nonnull
  Schema getSchema(T type, OpenTypeConverterSupplier convSupp);

  OpenTypeAvroConverter<SimpleType, Object, Object> SIMPLE_TYPE =
          new OpenTypeAvroConverter<SimpleType, Object, Object>() {
    @Override
    @SuppressForbiden
    public Object fromOpenValue(final SimpleType type, final Object openValue,
            final OpenTypeConverterSupplier convSupp) {
      if (openValue == null) {
        return openValue;
      }
      String className = type.getClassName();
      if (className.equals(ObjectName.class.getName())) {
        return ((ObjectName) openValue).getCanonicalName();
      } else if (className.equals(Date.class.getName())) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(((Date) openValue).getTime()));
      } else if (className.equals(String.class.getName())) {
        return openValue.toString();
      }
      return openValue;
    }

    @Override
    @SuppressForbiden
    public Object toOpenValue(final SimpleType type, final Object value, final OpenTypeConverterSupplier convSupp) {
      if (value == null) {
        return value;
      }
      String className = type.getClassName();
      if (className.equals(ObjectName.class.getName())) {
        try {
          return new ObjectName(value.toString());
        } catch (MalformedObjectNameException ex) {
          throw new IllegalArgumentException("Invalid ObjectName " + value, ex);
        }
      } else if (className.equals(Date.class.getName())) {
        return new Date(DateTimeFormatter.ISO_INSTANT.parse((String) value, Instant::from).toEpochMilli());
      } else if (className.equals(String.class.getName())) {
        return value.toString();
      }
      return value;
    }

    @Override
    @SuppressForbiden
    public Schema getSchema(final SimpleType type, final OpenTypeConverterSupplier convSupp) {
      String className = type.getClassName();
      if (className.equals(ObjectName.class.getName())) {
        return Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING));
      } else if (className.equals(Date.class.getName())) {
        Schema strType = Schema.create(Schema.Type.STRING);
        strType.addProp(LogicalType.LOGICAL_TYPE_PROP, "instant");
        LogicalType lt = LogicalTypes.fromSchema(strType);
        strType.setLogicalType(lt);
        return Schema.createUnion(Schema.create(Schema.Type.NULL), strType);
      }
      try {
        return Schema.createUnion(Schema.create(Schema.Type.NULL),
                ExtendedReflectData.get().getSchema(Reflections.forName(className)));
      } catch (ClassNotFoundException ex) {
        throw new RuntimeException(ex);
      }
    }
  };

  OpenTypeAvroConverter<ArrayType, Iterable, Object> ARRAY_TYPE =
          new OpenTypeAvroConverter<ArrayType, Iterable, Object>() {
    @Override
    public Iterable fromOpenValue(final ArrayType type, @Nullable final Object openValue,
            final OpenTypeConverterSupplier convSupp) {
      if (openValue == null) {
        return null;
      }
      OpenType elementOpenType = type.getElementOpenType();
      OpenTypeAvroConverter elemConverter = convSupp.getConverter(elementOpenType);
      int length = java.lang.reflect.Array.getLength(openValue);
      List result = new ArrayList(length);
      for (int i = 0; i < length; i++) {
        result.add(elemConverter.fromOpenValue(elementOpenType, java.lang.reflect.Array.get(openValue, i), convSupp));
      }
      return result;
    }

    @Override
    public Object toOpenValue(final ArrayType type, final Iterable value, final OpenTypeConverterSupplier convSupp) {
      if (value == null) {
        return null;
      }
      OpenType elementOpenType = type.getElementOpenType();
      OpenTypeAvroConverter elemConverter = convSupp.getConverter(elementOpenType);
      List result = new ArrayList();
      for (Object elem : value) {
        result.add(elemConverter.toOpenValue(elementOpenType, elem, convSupp));
      }
      try {
        return result.toArray((Object[]) java.lang.reflect.Array
                .newInstance(Reflections.forName(elementOpenType.getClassName()),
                        result.size()));
      } catch (ClassNotFoundException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public Schema getSchema(final ArrayType type, final OpenTypeConverterSupplier convSupp) {
      OpenType elementOpenType = type.getElementOpenType();
      Schema elemSchema = convSupp.getConverter(elementOpenType).getSchema(elementOpenType, convSupp);
      return Schema.createArray(elemSchema);
    }
  };

  OpenTypeAvroConverter<CompositeType, GenericRecord, CompositeData> COMPOSITE_TYPE
          = new OpenTypeAvroConverter<CompositeType, GenericRecord, CompositeData>() {
    @Override
    public GenericRecord fromOpenValue(final CompositeType type, final CompositeData openValue,
            final OpenTypeConverterSupplier convSupp) {
      if (openValue == null) {
        return null;
      }
      GenericData.Record record = new GenericData.Record(getSchema(type, convSupp));
      for (String attribute : type.keySet()) {
        OpenType<?> aType = type.getType(attribute);
        record.put(attribute, convSupp.getConverter(aType).fromOpenValue(aType,
                openValue.get(attribute), convSupp));
      }
      return record;
    }

    @Override
    public CompositeData toOpenValue(final CompositeType type, final GenericRecord value,
            final OpenTypeConverterSupplier convSupp) {
      if (value == null) {
        return null;
      }
      Schema schema = value.getSchema();
      List<Schema.Field> fields = schema.getFields();
      int size = fields.size();
      String[] names = new String[size];
      Object[] values = new Object[size];
      for (Schema.Field field : fields) {
        int pos = field.pos();
        names[pos] = field.name();
        Object val = value.get(pos);
        OpenType<?> fType = type.getType(field.name());
        values[pos] = convSupp.getConverter(fType).toOpenValue(fType, val, convSupp);
      }
      try {
        return new CompositeDataSupport(type, names, values);
      } catch (OpenDataException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public Schema getSchema(final CompositeType type, final OpenTypeConverterSupplier convSupp) {
      Set<String> keySet = type.keySet();
      List<Schema.Field> fields = new ArrayList<>(keySet.size());
      for (String attribute : keySet) {
        OpenType<?> aType = type.getType(attribute);
        Schema schema = convSupp.getConverter(aType).getSchema(aType, convSupp);
        fields.add(new Schema.Field(attribute, schema, "", (Object) null));
      }
      return Schema.createRecord(null, type.getDescription(), "", false, fields);
    }
  };

  OpenTypeAvroConverter<TabularType, List<GenericRecord>, TabularData> TABULAR_TYPE
          = new OpenTypeAvroConverter<TabularType, List<GenericRecord>, TabularData>() {
    @Override
    public List<GenericRecord> fromOpenValue(final TabularType type, final TabularData openValue,
            final OpenTypeConverterSupplier convSupp) {
      if (openValue == null) {
        return null;
      }
      CompositeType rowType = type.getRowType();
      OpenTypeAvroConverter converter = convSupp.getConverter(rowType);
      Collection<CompositeData> data = (Collection<CompositeData>) openValue.values();
      List<GenericRecord> result = new ArrayList<>(data.size());
      for (CompositeData row : data) {
        result.add((GenericRecord) converter.fromOpenValue(rowType, row, convSupp));
      }
      return result;
    }

    @Override
    public TabularData toOpenValue(final TabularType type, final List<GenericRecord> value,
            final OpenTypeConverterSupplier convSupp) {
      if (value == null) {
        return null;
      }
      TabularDataSupport result = new TabularDataSupport(type, value.size(), 1.0f);
      CompositeType rowType = type.getRowType();
      OpenTypeAvroConverter converter = convSupp.getConverter(rowType);
      for (GenericRecord rec : value) {
        result.put((CompositeData) converter.toOpenValue(rowType, rec, convSupp));
      }

      return result;
    }

    @Override
    public Schema getSchema(final TabularType type, final OpenTypeConverterSupplier convSupp) {
      CompositeType rowType = type.getRowType();
      Schema rowSchema = convSupp.getConverter(rowType).getSchema(rowType, convSupp);
      Schema arraySchema = Schema.createArray(rowSchema);
      ArrayNode arrNode = Schema.MAPPER.createArrayNode();
      for (String pkc : type.getIndexNames()) {
        arrNode = arrNode.add(pkc);
      }
      arraySchema.addProp("primaryKey", arrNode);
      return arraySchema;
    }

  };

}
