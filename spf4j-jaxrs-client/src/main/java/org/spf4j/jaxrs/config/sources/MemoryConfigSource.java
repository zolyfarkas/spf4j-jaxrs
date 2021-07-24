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
package org.spf4j.jaxrs.config.sources;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.spf4j.jaxrs.config.ConfigEvent;
import org.spf4j.jaxrs.config.ConfigWatcher;
import org.spf4j.jaxrs.config.ObservableConfig;
import org.spf4j.jaxrs.config.PropertyWatcher;

/**
 * A simple memory based ObservableConfig.
 * @author Zoltan Farkas
 */
public final class MemoryConfigSource implements ConfigSource, ObservableConfig {

  private class ValueHolder {
    private String value;

    private List<PropertyWatcher> watchers;

    ValueHolder(final String value) {
      this.value = value;
      watchers = null;
    }

    ValueHolder(final PropertyWatcher watcher) {
      this.value = null;
      watchers = new ArrayList<>(2);
      watchers.add(watcher);
    }

    synchronized String getValue() {
      return value;
    }

    synchronized String put(final String key, @Nullable final String pvalue) {
      String existing = this.value;
      this.value = pvalue;
      if (Objects.equals(pvalue, existing)) {
        return existing;
      }
      ConfigEvent event;
      if (existing == null) {
        event = ConfigEvent.ADDED;
      } else if (value == null) {
        event = ConfigEvent.DELETED;
      } else {
        event = ConfigEvent.MODIFIED;
      }
      if (watchers != null) {
        for (PropertyWatcher watcher : watchers) {
          watcher.accept(event);
        }
      }
      for (ConfigWatcher watcher : globalWatchers) {
        watcher.accept(key, event);
      }
      return existing;
    }

    synchronized void addWatcher(final PropertyWatcher watcher) {
      if (watchers == null) {
        watchers = new ArrayList<>(2);
      }
      watchers.add(watcher);
      if (value != null) {
        watcher.accept(ConfigEvent.ADDED);
      }
    }

    synchronized boolean removeWatcher(final PropertyWatcher watcher) {
      if (watchers == null) {
        return false;
      }
      return watchers.remove(watcher);
    }

    synchronized boolean hasWatchers() {
      return watchers != null && !watchers.isEmpty();
    }

    @Override
    public synchronized String toString() {
      return "ValueHolder{" + "value=" + value + ", watchers=" + watchers + '}';
    }

  }

  private final ConcurrentMap<String, ValueHolder>  configs;

  private final List<ConfigWatcher> globalWatchers;


  public MemoryConfigSource() {
    configs = new ConcurrentHashMap<>();
    this.globalWatchers = new CopyOnWriteArrayList<>();
  }

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> result = Maps.newHashMapWithExpectedSize(configs.size());
    for (Map.Entry<String, ValueHolder> entry : configs.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getValue());
    }
    return result;
  }

  @Override
  public int getOrdinal() {
    return 10;
  }

  @Override
  public String getValue(final String propertyName) {
    ValueHolder holder = configs.get(propertyName);
    return holder == null ? null : holder.getValue();
  }

  public void putValue(final String propertyName, final String propertyValue) {
      configs.compute(propertyName, (k, v) -> {
      if (v == null) {
        return new ValueHolder(propertyValue);
      } else {
        v.put(k, propertyValue);
        return v;
      }
    });
  }


  @Override
  public String getName() {
    return MemoryConfigSource.class.getName();
  }

  @Override
  public void addWatcher(final ConfigWatcher consumer) {
    globalWatchers.add(consumer);
    consumer.unknownEvents();
  }

  @Override
  public void addWatcher(final String name, final PropertyWatcher consumer) {
    configs.compute(name, new BiFunction<String, ValueHolder, ValueHolder>() {
      @Override
      @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS")
      public ValueHolder apply(final String k, final ValueHolder v) {
        if (v == null) {
          return new ValueHolder(consumer);
        } else {
          v.addWatcher(consumer);
          return v;
        }
      }
    });
  }

  @Override
  public void removeWatcher(final ConfigWatcher consumer) {
    globalWatchers.remove(consumer);
  }

  @Override
  public void removeWatcher(final String name, final PropertyWatcher consumer) {
    configs.compute(name, (k, v) -> {
      if (v == null) {
        return null;
      } else {
        v.removeWatcher(consumer);
        if (v.hasWatchers()) {
          return v;
        } else {
          return null;
        }
      }
    });
  }

  @Override
  public String toString() {
    return "MemoryConfigSource{" + "configs=" + configs + ", globalWatchers=" + globalWatchers + '}';
  }

}
