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
package org.spf4j.servlet;

import com.google.common.collect.Iterators;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.MultivaluedMap;
import org.spf4j.base.Wrapper;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class HeaderOverwriteHttpServletRequest extends HttpServletRequestWrapper
        implements Wrapper<HttpServletRequest> {

  private final MultivaluedMap<String, String> overwrites;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public HeaderOverwriteHttpServletRequest(final HttpServletRequest request,
          final MultivaluedMap<String, String> overwrites) {
    super(request);
    this.overwrites = overwrites;
  }

  @Override
  public int getIntHeader(final String name) {
    List<String> values = overwrites.get(name);
    if (values == null || values.isEmpty()) {
      return super.getIntHeader(name);
    } else {
      return Integer.parseInt(values.get(0));
    }
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    Set<String> result = new HashSet<>();
    result.addAll(overwrites.keySet());
    Enumeration<String> hEnum = super.getHeaderNames();
    while (hEnum.hasMoreElements()) {
      result.add(hEnum.nextElement());
    }
    return Iterators.asEnumeration(result.iterator());
  }

  @Override
  public Enumeration<String> getHeaders(final String name) {
    List<String> values = overwrites.get(name);
    if (values == null || values.isEmpty()) {
      return super.getHeaders(name);
    } else {
      return Iterators.asEnumeration(values.iterator());
    }
  }

  @Override
  public String getHeader(final String name) {
    List<String> values = overwrites.get(name);
    if (values == null || values.isEmpty()) {
      return super.getHeader(name);
    } else {
      return values.get(0);
    }
  }

  @Override
  public long getDateHeader(final String name) {
    List<String> values = overwrites.get(name);
    if (values == null || values.isEmpty()) {
      return super.getIntHeader(name);
    } else {
      return DateTimeFormatter.RFC_1123_DATE_TIME.parse(values.get(0), Instant::from).toEpochMilli();
    }
  }


  @Override
  public HttpServletRequest getWrapped() {
    return (HttpServletRequest) super.getRequest();
  }

  @Override
  public String toString() {
    return "HeaderOverwriteHttpServletRequest{" + "overwrites=" + overwrites + '}';
  }

}
