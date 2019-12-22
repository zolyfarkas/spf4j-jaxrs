/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.actuator.profiles;

import com.github.jknack.handlebars.Handlebars;

/**
 * Visualization page (FlameGraph.html) parameters.
 * @author Zoltan Farkas
 */
public final class FlameGraphParams {

  private final String title;
  private final Handlebars.SafeString dataUrl;

  public FlameGraphParams(final String title, final CharSequence dataUrl) {
    this.title = title;
    this.dataUrl = new Handlebars.SafeString(dataUrl);
  }

  public String getTitle() {
    return title;
  }

  public Handlebars.SafeString getDataUrl() {
    return dataUrl;
  }

  @Override
  public String toString() {
    return "FlameGraphParams{" + "title=" + title + ", dataUrl=" + dataUrl + '}';
  }

}
