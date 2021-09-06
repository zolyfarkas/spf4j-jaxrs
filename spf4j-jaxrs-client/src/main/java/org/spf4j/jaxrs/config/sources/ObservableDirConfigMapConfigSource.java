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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Closeables;
import org.spf4j.jaxrs.config.ConfigEvent;
import org.spf4j.jaxrs.config.ConfigWatcher;
import org.spf4j.jaxrs.config.ObservableConfig;
import org.spf4j.jaxrs.config.PropertyWatcher;

/**
 * appropriate for loading kubernetes config maps:
 *
 * @see https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#add-configmap-data-to-a-volume
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public abstract class ObservableDirConfigMapConfigSource extends BasicDirConfigMapConfigSource
        implements ObservableConfig, Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(ObservableDirConfigMapConfigSource.class);

  private final List<ConfigWatcher> watchers;

  private final ConcurrentMap<String, List<PropertyWatcher>> propertyWatchers;

  private boolean closed;

  ObservableDirConfigMapConfigSource(final Path folder, final Charset charset) {
    super(folder, charset);
    this.watchers = new CopyOnWriteArrayList<>();
    this.propertyWatchers = new ConcurrentHashMap<>();
    this.closed = false;
  }

  /**
   * This initializes the file watch. will be called multiple times.
   */
  abstract void initWatcher();

  final void notifyUnknown() {
    for (ConfigWatcher watcher : watchers) {
      try {
        watcher.unknownEvents();
      } catch (RuntimeException ex) {
        LOG.error("Failed to notify unknown event -> {}", watcher, ex);
      }
    }
    for (List<PropertyWatcher> pws : propertyWatchers.values()) {
      for (PropertyWatcher pw : pws) {
        try {
          pw.unknownEvents();
        } catch (RuntimeException ex) {
        LOG.error("Failed to notify unknown event -> {}", pw, ex);
        }
      }
    }
  }

  final void notify(final String propertyName, final ConfigEvent event) {
    for (ConfigWatcher watcher : watchers) {
      try {
        watcher.accept(propertyName, event);
      } catch (RuntimeException ex) {
        LOG.error("Failed to notify {} -> {}", event, watcher, ex);
      }
    }
    List<PropertyWatcher> pws = propertyWatchers.get(propertyName);
    if (pws != null) {
      for (PropertyWatcher pw : pws) {
        try {
          pw.accept(event);
        } catch (RuntimeException ex) {
        LOG.error("Failed to notify {} -> {}", event, pw, ex);
      }
      }
    }
  }

  @Override
  public final void addWatcher(final ConfigWatcher consumer) {
    initWatcher();
    watchers.add(consumer);
    consumer.unknownEvents();
  }

  @Override
  public final void addWatcher(final String name, final PropertyWatcher consumer) {
    initWatcher();
    propertyWatchers.compute(name, (k, v) -> {
      if (v == null) {
        List<PropertyWatcher> pws = new ArrayList<>(2);
        pws.add(consumer);
        return pws;
      } else {
        v.add(consumer);
        return v;
      }
    });
    consumer.unknownEvents();
  }

  @Override
  public final void removeWatcher(final ConfigWatcher consumer) {
    watchers.remove(consumer);
  }

  @Override
  public final void removeWatcher(final String name, final PropertyWatcher consumer) {
    propertyWatchers.compute(name, (k, v) -> {
      if (v == null) {
        return null;
      } else {
        v.remove(consumer);
        if (v.isEmpty()) {
          return null;
        } else {
          return v;
        }
      }
    });
  }

  public final synchronized boolean isClosed() {
    return closed;
  }

  /**
   * Overwrite in subclass to cleanup any additional resources.
   * @throws IOException
   */
  @Override
  @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
  public synchronized void close() throws IOException {
    if (closed) {
      Exception ex = Closeables.closeAll(null, watchers);
      for (List<PropertyWatcher> pws :  propertyWatchers.values()) {
        ex = Closeables.closeAll(ex, pws);
      }
      watchers.clear();
      propertyWatchers.clear();
      closed = true;
      if (ex instanceof IOException) {
        throw (IOException) ex;
      } else if (ex instanceof RuntimeException) {
        throw (RuntimeException) ex;
      } else {
        throw new RuntimeException(ex);
      }
    }
  }



}
