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

import com.sun.nio.file.SensitivityWatchEventModifier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.hash.THashSet;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.spf4j.base.Env;
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
public final class DirConfigMapConfigSource implements ObservableConfig, ConfigSource, Closeable, Runnable {

  private final Charset charset;

  private final Path folder;

  private WatchService watchService;

  private final List<ConfigWatcher> watchers;

  private final ConcurrentMap<String, List<PropertyWatcher>> propertyWatchers;

  private Thread watchThread;

  public DirConfigMapConfigSource(final Path folder, final Charset charset) {
    this.folder = folder;
    this.charset = charset;
    this.watchers = new CopyOnWriteArrayList<>();
    this.propertyWatchers = new ConcurrentHashMap<>();
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN") //comming from trusted config.
  public DirConfigMapConfigSource() {
    this(Paths.get(Env.getValue("APP_CONFIG_MAP_DIR", "/etc/config")), StandardCharsets.UTF_8);
  }

  private synchronized void initWatcher() {
    if (watchService == null) {
      try {
        watchService = folder.getFileSystem().newWatchService();
        folder.register(watchService, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
          StandardWatchEventKinds.OVERFLOW
        }, SensitivityWatchEventModifier.HIGH);
        watchThread = new Thread(this, "dir-config-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (watchService != null) {
      watchThread.interrupt();
      try {
        watchThread.join(5000);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IOException(ex);
      } finally {
        watchService.close();
      }
    }
  }

  private synchronized WatchService getWatchService() {
    return watchService;
  }

  @Override
  public void run() {
    boolean alive = true;
    try {
      while (alive) {
        try {
          WatchKey key = getWatchService().poll(5, TimeUnit.SECONDS);
          if (key == null) {
            continue;
          }
          if (!key.isValid()) {
            key.cancel();
            break;
          }
          List<WatchEvent<?>> events = key.pollEvents();
          for (WatchEvent<?> event : events) {
            Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
              String fileName = ((WatchEvent<Path>) event).context().getFileName().toString();
              notify(fileName, ConfigEvent.ADDED);
            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
              String fileName = ((WatchEvent<Path>) event).context().getFileName().toString();
              notify(fileName, ConfigEvent.DELETED);
            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
              String fileName = ((WatchEvent<Path>) event).context().getFileName().toString();
              notify(fileName, ConfigEvent.MODIFIED);
            } else { // overflow, etc...
              notifyUnknown();
            }
          }
          if (!key.reset()) {
            key.cancel();
            break;
          }
        } catch (InterruptedException ex) {
          alive = false;
        }
      }
    } catch (RuntimeException ex) {
      Logger.getLogger(DirConfigMapConfigSource.class.getName())
              .log(Level.SEVERE, "Failure in log watcher", ex);
    }
  }

  private void notifyUnknown() {
    for (ConfigWatcher watcher : watchers) {
      watcher.unknownEvents();
    }
    for (List<PropertyWatcher> pws : propertyWatchers.values()) {
      for (PropertyWatcher pw : pws) {
        pw.unknownEvents();
      }
    }
  }

  private void notify(final String propertyName, final ConfigEvent event) {
    for (ConfigWatcher watcher : watchers) {
      watcher.accept(propertyName, event);
    }
    List<PropertyWatcher> pws = propertyWatchers.get(propertyName);
    if (pws != null) {
      for (PropertyWatcher pw : pws) {
          pw.accept(event);
      }
    }
  }

  @Override
  public void addWatcher(final ConfigWatcher consumer) {
    initWatcher();
    watchers.add(consumer);
    consumer.unknownEvents();
  }

  @Override
  public void addWatcher(final String name, final PropertyWatcher consumer) {
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
  public void removeWatcher(final ConfigWatcher consumer) {
    watchers.remove(consumer);
  }

  @Override
  public void removeWatcher(final String name, final PropertyWatcher consumer) {
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


  @Override
  @SuppressFBWarnings({"PATH_TRAVERSAL_IN"}) //intentional
  public Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<>();
    try (Stream<Path> list = Files.list(folder)) {
      Iterator<Path> it = list.iterator();
      while (it.hasNext()) {
        Path p = it.next();
        if (Files.isReadable(p)) {
          try {
            result.put(p.getFileName().toString(), new String(Files.readAllBytes(p), charset));
          } catch (NoSuchFileException ex) {
          }
        }
      }
    } catch (NoSuchFileException ex) {
      return Collections.emptyMap();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return result;
  }

  @Override
  public Set<String> getPropertyNames() {
    Set<String> result = new THashSet<>();
    try (Stream<Path> list = Files.list(folder)) {
      Iterator<Path> it = list.iterator();
      while (it.hasNext()) {
        Path p = it.next();
        if (Files.isReadable(p)) {
          result.add(p.getFileName().toString());
        }
      }
    } catch (NoSuchFileException ex) {
      return Collections.emptySet();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return result;
  }

  @Override
  public int getOrdinal() {
    return 10;
  }

  @Override
  @Nullable
  public String getValue(final String propertyName) {
    if (propertyName.indexOf(File.separatorChar) >= 0) {
      throw new IllegalArgumentException("Invalid Property name: " + propertyName);
    }
    Path p = folder.resolve(propertyName);
    try {
      if (Files.isReadable(p)) {
        try {
          return new String(Files.readAllBytes(p), charset);
        } catch (NoSuchFileException ex) {
          return null;
        }
      } else {
        return null;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String getName() {
    return DirConfigMapConfigSource.class.getName() + '(' + folder + ')';
  }

  @Override
  public String toString() {
    return "DirConfigMapConfigSource{" + "charset=" + charset + ", folder=" + folder + '}';
  }

}
