/*
 * Copyright 2020 SPF4J.
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
package org.spf4j.actuator.metrics;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import io.prometheus.client.Collector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.avro.Schema;
import org.spf4j.base.Pair;
import org.spf4j.perf.TimeSeriesRecord;
import org.spf4j.perf.impl.Quanta;
import org.spf4j.tsdb2.TSDBQuery;
import org.spf4j.tsdb2.avro.MeasurementType;

/**
 *
 * @author Zoltan Farkas
 */
public final class PrometheusUtils {

  private PrometheusUtils() {
  }

  public static Collector.Type convert(final MeasurementType mtype) {
    switch (mtype) {
      case COUNTER:
        return Collector.Type.COUNTER;
      case GAUGE:
        return Collector.Type.GAUGE;
      case HISTOGRAM:
        return Collector.Type.HISTOGRAM;
      case SUMMARY:
        return Collector.Type.SUMMARY;
      default:
        return Collector.Type.UNTYPED;
    }

  }

  private static List<Collector.MetricFamilySamples.Sample> convertValues(
          final Schema schema,
          final List<String> labels,
          final List<String> labelValues,
          final Iterable<TimeSeriesRecord> recs) {
    List<Collector.MetricFamilySamples.Sample> samples = new ArrayList<>();
    for (TimeSeriesRecord rec : recs) {
      long ts = rec.getTimeStamp().toEpochMilli();
      Iterator<Schema.Field> it = schema.getFields().iterator();
      it.next();
      while (it.hasNext()) {
        Schema.Field f = it.next();
        Schema fSchema = f.schema();
        String fName = f.name();
        String unit = fSchema.getProp("unit");
        if (unit != null) {
          fName = unit + '_' + fName;  // https://prometheus.io/docs/instrumenting/exposition_formats/
        }
        switch (fSchema.getType()) {
          case LONG:
            samples.add(new Collector.MetricFamilySamples.Sample(fName, labels, labelValues,
                    (Long) rec.get(f.pos()), ts));
            break;
          case DOUBLE:
            samples.add(new Collector.MetricFamilySamples.Sample(fName, labels, labelValues,
                    (Double) rec.get(f.pos()), ts));
            break;
          default:
            throw new UnsupportedOperationException("Unsupporrted metric type " + f);
        }
      }
    }
    return samples;
  }

  private static List<Collector.MetricFamilySamples.Sample> convertHistogram(
          final Schema schema,
          final List<String> labels,
          final List<String> labelValues,
          final Iterable<TimeSeriesRecord> recs) {
    List<Schema.Field> fields = schema.getFields();
    List<Pair<String, String>> les = new ArrayList<>();
    for (Schema.Field field : fields) {
      String name = field.name();
      if (name.startsWith("Q")) {
        Quanta q = new Quanta(name);
        les.add(Pair.of(name, Long.toString(q.getIntervalEnd())));
      }
    }
    List<Collector.MetricFamilySamples.Sample> samples = new ArrayList<>();
    for (TimeSeriesRecord rec : recs) {
      long ts = rec.getTimeStamp().toEpochMilli();
      for (Pair<String, String> le : les) {
        List<String> l = new ArrayList<>(2);
        l.addAll(labels);
        l.add("le");
        List<String> lv = new ArrayList<>(2);
        l.addAll(labelValues);
        lv.add(le.getSecond());
        samples.add(new Collector.MetricFamilySamples.Sample("count", l,
                lv, rec.getLongValue(le.getFirst()), ts));
      }
    }
    return samples;
  }

  @Nullable
  public static Collector.MetricFamilySamples convert(final Iterable<TimeSeriesRecord> recs) {
    Iterator<TimeSeriesRecord> iterator = recs.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    PeekingIterator<TimeSeriesRecord> pi = Iterators.peekingIterator(iterator);
    TimeSeriesRecord peek = pi.peek();
    return convert(peek.getSchema(), () -> pi);
  }

  public static Collector.MetricFamilySamples convert(final Schema schema, final Iterable<TimeSeriesRecord> recs) {
    String name = schema.getName();
    Pair<String, String> composite = Pair.from(name);
    List<String> labels;
    List<String> labelValues;
    if (composite == null) {
      labels = new ArrayList<>(1);
      labelValues = new ArrayList<>(1);
    } else {
      name = composite.getFirst();
      labels = new ArrayList<>(2);
      labels.add("group");
      labelValues = new ArrayList<>(2);
      labelValues.add(composite.getSecond());
    }
    MeasurementType measurementType = TSDBQuery.getMeasurementType(schema);
    switch (measurementType) {
      case COUNTER:
        return new Collector.MetricFamilySamples(name, Collector.Type.COUNTER, schema.getDoc(),
                convertValues(schema, labels, labelValues, recs));
      case GAUGE:
        return new Collector.MetricFamilySamples(name, Collector.Type.GAUGE, schema.getDoc(),
                convertValues(schema, labels, labelValues, recs));
      case SUMMARY:
        return new Collector.MetricFamilySamples(name, Collector.Type.SUMMARY, schema.getDoc(),
                convertValues(schema, labels, labelValues, recs));
      case HISTOGRAM:
        return new Collector.MetricFamilySamples(name, Collector.Type.HISTOGRAM, schema.getDoc(),
                convertHistogram(schema, labels, labelValues, recs));
      default:
        throw new UnsupportedOperationException("Unsupported type " + measurementType);
    }
  }

}
