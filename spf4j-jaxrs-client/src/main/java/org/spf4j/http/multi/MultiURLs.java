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
package org.spf4j.http.multi;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.glassfish.jersey.uri.UriComponent;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.http.RequestContextTags;

/**
 *
 * @author Zoltan Farkas
 */
public final class MultiURLs {

  private MultiURLs() { }

  public enum Protocol {
    mhttp, mhttps
  }

  public static  URL[] parseURLs(final URL url1) throws MalformedURLException {
    String path = url1.getPath().substring(1);
    int indexOf = path.indexOf('/');
    String rest;
    if (indexOf >= 0) {
      rest = path.substring(indexOf);
      path = path.substring(0, indexOf);
    } else {
      rest = "";
    }
    String[] multipleUrls = path.split(";"); //matrix params
    if (multipleUrls.length <= 0) {
      throw new MalformedURLException("Invalid URL" + url1);
    }
    URL[] urls = new URL[multipleUrls.length];
    for (int i = 0; i < multipleUrls.length; i++) {
      try {
        urls[i] = new URL(URLDecoder.decode(multipleUrls[i], StandardCharsets.UTF_8.name()) + rest);
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }
    return urls;
  }

  public static URL newURL(final Protocol protocol, final URL... urls) throws MalformedURLException {
    String[] surls = new String[urls.length];
    for (int i = 0; i < urls.length; i++) {
      surls[i] = urls[i].toString();
    }
    return newURL(protocol, surls);
  }


  public static URL newURL(final Protocol protocol, final String... urls) throws MalformedURLException {
    StringBuilder path = new StringBuilder(32 * urls.length);
    path.append('/');
    String url = urls[0];
    path.append(UriComponent.encode(url, UriComponent.Type.PATH_SEGMENT));
    for (int i = 1; i < urls.length; i++) {
      url = urls[i];
      path.append(';');
      path.append(UriComponent.encode(url, UriComponent.Type.PATH_SEGMENT));
    }
    path.append('/');
    return new URL(protocol.toString(), null, path.toString());
  }

  public static int getChoiceCount() {
    ExecutionContext current = ExecutionContexts.current();
    if (current != null) {
      Integer tryCount = current.get(RequestContextTags.TRY_COUNT);
      if (tryCount != null) {
        return Math.abs(tryCount - 1);
      }
    }
    return 0;
  }

}
