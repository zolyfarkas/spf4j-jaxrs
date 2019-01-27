package org.spf4j.avro;

import com.google.common.annotations.Beta;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.spi.json.JsonProvider;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.IndexedRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ExtendedJsonEncoder;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.apache.avro.reflect.ReflectData;

@Beta
public final class AvroPathProvider implements JsonProvider {

    @Override
    public Object parse(String json) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object parse(InputStream jsonStream, String charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toJson(Object obj) {
      try {
        Schema schema = ReflectData.get().getSchema(obj.getClass());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DatumWriter writer = new ExtendedReflectDatumWriter(schema);
        Encoder encoder = new ExtendedJsonEncoder(schema, bos);
        writer.write(obj, encoder);
        encoder.flush();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    @Override
    public Object createArray() {
        return new ArrayList<>();
    }

    @Override
    public Object createMap() {
      // will have to change to generic record...
      return new HashMap();
    }

    @Override
    public boolean isArray(final Object obj) {
        return obj instanceof List;
    }

    @Override
    public int length(final Object obj) {
        if (isArray(obj)) {
            return ((List) obj).size();
        } else if (isMap(obj)) {
            throw new InvalidJsonException("Not yet implemented: length(obj) if obj is Map: " + obj);
        } else if (obj instanceof String) {
            return ((String) obj).length();
        }
        throw new JsonPathException("Cannot get length for class: " + obj.getClass().getName());
    }

    @Override
    public Iterable<?> toIterable(final Object obj) {
        if (isArray(obj))
            return ((Iterable) obj);
        else
            throw new JsonPathException("Cannot iterate over " + obj.getClass().getName());
    }

    @Override
    public Collection<String> getPropertyKeys(final Object obj) {
        if (isArray(obj)) {
            throw new UnsupportedOperationException("Cannot get property keys for an array");
        } else if (obj instanceof GenericContainer){
            GenericContainer rec = (GenericContainer) obj;
            return rec.getSchema().getFields().stream().map(Schema.Field::name).collect(Collectors.toList());
        } else {
            throw new JsonPathException("Cannot interpret class " + obj.getClass().getName());
        }
    }

    @Override
    public Object getArrayIndex(final Object obj, final int idx) {
        return ((List) obj).get(idx);
    }

    @Override
    public Object getArrayIndex(final Object obj, final int idx, final boolean unwrap) {
        return getArrayIndex(obj, idx);
    }

    @Override
    public void setArrayIndex(final Object array, final int idx, final Object newValue) {
        if (!isArray(array)) {
            throw new UnsupportedOperationException();
        } else {
            List l = (List) array;
            if (idx == l.size()){
                l.add(newValue);
            }else {
                l.set(idx, newValue);
            }
        }
    }

    @Override
    public Object getMapValue(final Object obj, final String key) {
        if (obj instanceof IndexedRecord) {
            IndexedRecord rec = (IndexedRecord) obj;
            Schema.Field f = rec.getSchema().getField(key);
            if (f == null) return JsonProvider.UNDEFINED;
            else return rec.get(f.pos());
        } else {
            return JsonProvider.UNDEFINED;
        }
    }

    @Override
    public void setProperty(final Object obj, final Object key, final Object value) {
        if (isMap(obj)) {
            IndexedRecord rec = (IndexedRecord) obj;
            Schema.Field field = rec.getSchema().getField(String.valueOf(key));
            if (field == null) throw new InvalidJsonException("No such field in schema: " + key);
            rec.put(field.pos(), value);
        } else if (isArray(obj)) {
            setArrayIndex(obj, key instanceof Integer ? (Integer) key : Integer.parseInt(key.toString()), value);
        } else if (obj instanceof Map) {
           ((Map) obj).put(key, value);
        }
    }

    public void removeProperty(final Object obj, final Object key) {
        throw new UnsupportedOperationException("Cannot remove fields.");
    }

    @Override
    public boolean isMap(final Object obj) {
        return obj instanceof IndexedRecord;
    }

    @Override
    public Object unwrap(final Object obj) {
        return obj;
    }

}