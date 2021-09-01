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
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Env;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.jaxrs.config.ConfigEvent;

/**
 * appropriate for loading kubernetes config maps:
 *
 * @see https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#add-configmap-data-to-a-volume
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"CFS_CONFUSING_FUNCTION_SEMANTICS", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"})
public final class PollingDirConfigMapConfigSource extends ObservableDirConfigMapConfigSource
        implements Closeable, Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(PollingDirConfigMapConfigSource.class);

  private ScheduledFuture<?> poll;

  private boolean closed;

  private int tick;

  private final Map<Path, LastState> lastState;

  private final int pollSeconds;

  private static class LastState {

    private int tick;
    private long lastModified;

    LastState(final long lastModified, final int tick) {
      this.tick = tick;
      this.lastModified = lastModified;
    }

    public int getTick() {
      return tick;
    }

    public long getLastModified() {
      return lastModified;
    }

    public void setLastModifiedAndTick(final long plastModified, final int ptick) {
      this.lastModified = plastModified;
      this.tick = ptick;
    }

    public void setTick(final int tick) {
      this.tick = tick;
    }

  }

  public PollingDirConfigMapConfigSource(final Path folder, final Charset charset, final int pollSeconds) {
    super(folder, charset);
    this.tick = 0;
    this.closed = false;
    this.poll = null;
    this.lastState = new HashMap<>();
    this.pollSeconds = pollSeconds;
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN") //comming from trusted config.
  public PollingDirConfigMapConfigSource() {
    this(Paths.get(Env.getValue("APP_CONFIG_MAP_DIR", "/etc/config")), StandardCharsets.UTF_8,
            Env.getValue("APP_CONFIG_MAP_DIR_POLL_SECONDS", 5));
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  synchronized void initWatcher() {
    if (poll == null) {
      initState();
      poll = DefaultScheduler.instance().scheduleWithFixedDelay(this, pollSeconds, pollSeconds, TimeUnit.SECONDS);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      try {
        poll.cancel(true);
      } finally {
        closed = true;
      }
    }
  }

  private synchronized void initState() {
    try (Stream<Path> stream = Files.list(getFolder())) {
      stream.forEach(path -> {
        lastState.compute(path, (k, v) -> {
          long lastModifiedTime;
          try {
            BasicFileAttributes attrs
                    = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            lastModifiedTime = attrs.lastModifiedTime().toMillis();
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
          if (v == null) {
            return new LastState(lastModifiedTime, tick);
          } else {
            if (lastModifiedTime > v.getLastModified()) {
              v.setLastModifiedAndTick(lastModifiedTime, tick);
            } else {
              v.setTick(tick);
            }
            return v;
          }
        });
      });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public synchronized void run() {
    tick++;
    Path folder = getFolder();
    LOG.debug("polling folder for changes: {}", folder);
    try (Stream<Path> stream = Files.list(folder)) {
      stream.forEach(path -> {
        lastState.compute(path, (k, v) -> {
          long lastModifiedTime;
          try {
            BasicFileAttributes attrs
                    = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            lastModifiedTime = attrs.lastModifiedTime().toMillis();
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
          if (v == null) {
            LOG.info("config event: ADDED for {}", path);
            notify(path.getFileName().toString(), ConfigEvent.ADDED);
            return new LastState(lastModifiedTime, tick);
          } else {
            if (lastModifiedTime > v.getLastModified()) {
              LOG.info("config event: MODIFIED for {}", path);
              notify(path.getFileName().toString(), ConfigEvent.MODIFIED);
              v.setLastModifiedAndTick(lastModifiedTime, tick);
            } else {
              v.setTick(tick);
            }
            return v;
          }
        });
      });
    } catch (IOException | RuntimeException ex) {
      LOG.error("Exception while logging for config changes in {}", this, ex);
    }
    try {
      Iterator<Map.Entry<Path, LastState>> it = lastState.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<Path, LastState> entry = it.next();
        if (entry.getValue().getTick() != tick) {
          Path path = entry.getKey();
          LOG.info("config event: REMOVED for {}", path);
          notify(path.getFileName().toString(), ConfigEvent.DELETED);
          it.remove();
        }
      }
    } catch (RuntimeException ex) {
      LOG.error("Exception while logging for config changes in {}", this, ex);
    }
  }
}
