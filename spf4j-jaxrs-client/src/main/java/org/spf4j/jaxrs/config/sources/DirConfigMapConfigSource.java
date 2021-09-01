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
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Env;
import org.spf4j.jaxrs.config.ConfigEvent;

/**
 * Not working wing volume mapped config maps, since all notifs come to sime ..data and other temp folders.
 * The config volume does not play well with Linux inotify (at least currently)
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public final class DirConfigMapConfigSource extends ObservableDirConfigMapConfigSource
        implements Closeable, Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(DirConfigMapConfigSource.class);

  private WatchService watchService;

  private Thread watchThread;

  public DirConfigMapConfigSource(final Path folder, final Charset charset) {
    super(folder, charset);
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN") //comming from trusted config.
  public DirConfigMapConfigSource() {
    this(Paths.get(Env.getValue("APP_CONFIG_MAP_DIR", "/etc/config")), StandardCharsets.UTF_8);
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  synchronized void initWatcher() {
    if (watchService == null) {
      try {
        Path folder = getFolder();
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
        watchService = null;
      }
    }
  }

  private synchronized WatchService getWatchService() {
    return watchService;
  }

  @Override
  public void run() {
    boolean alive = true;
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
            Path context = ((WatchEvent<Path>) event).context();
            LOG.info("config event: {} for {}", event, context);
            String fileName = context.getFileName().toString();
            notify(fileName, ConfigEvent.ADDED);
          } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            Path context = ((WatchEvent<Path>) event).context();
            LOG.info("config event: {} for {}", event, context);
            String fileName = context.getFileName().toString();
            notify(fileName, ConfigEvent.DELETED);
          } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            Path context = ((WatchEvent<Path>) event).context();
            LOG.info("config event: {} for {}", event, context);
            String fileName = context.getFileName().toString();
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
      } catch (RuntimeException ex) {
        LOG.error("Failure in log watcher", ex);
      }
    }
  }
}
