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
import gnu.trove.set.hash.THashSet;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.spf4j.base.Env;

/**
 * appropriate for loading kubernetes config maps:
 * @see https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#add-configmap-data-to-a-volume
 * @author Zoltan Farkas
 */
public final class DirConfigMapConfigSource implements ConfigSource {

  private final Charset charset;

  private final Path folder;

  public DirConfigMapConfigSource(final Path folder, final Charset charset) {
    this.folder = folder;
    this.charset = charset;
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN") //comming from trusted config.
  public DirConfigMapConfigSource() {
    this(Paths.get(Env.getValue("APP_CONFIG_MAP_DIR", "/etc/config")), StandardCharsets.UTF_8);
  }

  @Override
  @SuppressFBWarnings({ "PATH_TRAVERSAL_IN", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" }) //intentional
  public Map<String, String> getProperties() {
    if (!Files.exists(folder)) {
      return Collections.EMPTY_MAP;
    }
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
      return Collections.EMPTY_MAP;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return result;
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public Set<String> getPropertyNames() {
    if (!Files.exists(folder)) {
      return Collections.EMPTY_SET;
    }
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
      return Collections.EMPTY_SET;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return result;
  }

  @Override
  public int getOrdinal() {
    return 500;
  }

  @Override
  public String getValue(final String propertyName) {
    if (propertyName.indexOf(File.separatorChar) >= 0) {
      throw new IllegalArgumentException("Invalid Property name: " + propertyName);
    }
    Path p  = folder.resolve(propertyName);
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
