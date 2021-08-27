/*
 * Copyright 2021 SPF4J.
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
package org.spf4j.kube.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.spf4j.base.CharArrayConsumer;
import org.spf4j.io.CharArrayBuilder;


/**
 * appropriate for reading secrets out of ramfs.
 * @author Zoltan Farkas
 */
public final class FileTokenProvider implements TokenProvider {

  private final Path filePath;

  private final Charset charSet;

  public FileTokenProvider(final Path filePath) {
    this(filePath, StandardCharsets.UTF_8);
  }

  public FileTokenProvider(final Path filePath, final Charset charSet) {
    this.filePath = filePath;
    this.charSet = charSet;
  }

  @Override
  @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
  public void access(final CharArrayConsumer consumer) {
    try (CharArrayBuilder builder = new CharArrayBuilder()) {
      try (InputStream is = java.nio.file.Files.newInputStream(filePath);
              Reader reader = new InputStreamReader(is, charSet);) {
        builder.readFrom(reader);
        char[] buffer = builder.getBuffer();
        int size = builder.size();
        consumer.accept(buffer, 0, size);
        org.spf4j.base.Arrays.fill(buffer, 0, size, (char) 0);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public String toString() {
    return "FileTokenProvider{" + "filePath=" + filePath + '}';
  }
}
