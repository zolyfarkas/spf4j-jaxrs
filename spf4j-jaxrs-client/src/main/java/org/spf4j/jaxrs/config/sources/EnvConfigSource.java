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

import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author Zoltan Farkas
 */
public final class EnvConfigSource implements ConfigSource {

  @Override
  public Map<String, String> getProperties() {
    return System.getenv();
  }

  @Override
  public int getOrdinal() {
    return 200;
  }

  @Override
  public String getValue(final String propertyName) {
    return System.getenv(propertyName);
  }

  @Override
  public String getName() {
    return EnvConfigSource.class.getName();
  }

}
