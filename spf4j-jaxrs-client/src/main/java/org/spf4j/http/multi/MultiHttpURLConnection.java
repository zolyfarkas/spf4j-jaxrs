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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import static org.spf4j.http.multi.MultiURLs.parseURLs;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"URLCONNECTION_SSRF_FD", "INCONSISTENT_SYNC",  "IS2_INCONSISTENT_SYNC"})
public final class MultiHttpURLConnection extends HttpURLConnection {

  private final URL[] urls;

  private int choice;

  private HttpURLConnection current;

  private boolean isConnected;

  public MultiHttpURLConnection(final URL url, final int choiceCount) throws MalformedURLException  {
    super(url);
    this.urls = parseURLs(url);
    choice = choiceCount % urls.length;
    try {
      this.current = (HttpURLConnection) this.urls[choice].openConnection();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    isConnected = false;
  }

  @Override
  public synchronized void connect() throws IOException {
    if (!isConnected) {
      connect(0, null);
      isConnected = true;
    }
  }


  private void connect(final int attempt, final IOException ex) throws IOException {
    if (attempt >= urls.length) {
      if (ex != null) {
        throw ex;
      } else {
        throw new IllegalStateException("Illegal state " + this);
      }
    }
    try {
      current.setConnectTimeout(super.getConnectTimeout());
      current.setReadTimeout(super.getReadTimeout());
      Map<String, List<String>> requestProperties = super.getRequestProperties();
      for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
        String key = entry.getKey();
        for (String val : entry.getValue()) {
          current.addRequestProperty(key, val);
        }
      }
      if (this.chunkLength > 0) {
        current.setChunkedStreamingMode(this.chunkLength);
      }
      if (this.fixedContentLengthLong >= 0L) {
        current.setFixedLengthStreamingMode(this.fixedContentLengthLong);
      }
      current.setRequestMethod(super.getRequestMethod());
      current.setInstanceFollowRedirects(super.getInstanceFollowRedirects());
      current.setDoInput(super.getDoInput());
      current.setDoOutput(super.getDoOutput());
      current.setIfModifiedSince(super.getIfModifiedSince());
      current.setDefaultUseCaches(super.getDefaultUseCaches());
      current.setUseCaches(super.getUseCaches());
      current.setAllowUserInteraction(super.getAllowUserInteraction());
      current.connect();
    } catch (IOException e) {
       nextUrl();
       connect(attempt + 1, e);
    }
  }

  public synchronized void nextUrl() throws IOException {
    this.current = (HttpURLConnection) urls[ ++choice % urls.length].openConnection();
  }

  @Override
  public synchronized OutputStream getOutputStream() throws IOException {
    if (!isConnected) {
      connect();
    }
    return current.getOutputStream();
  }

  @Override
  public synchronized InputStream getInputStream() throws IOException {
    if (!isConnected) {
      connect();
    }
    return current.getInputStream();
  }

  @Override
  public synchronized void disconnect() {
    if (isConnected) {
      try {
        current.disconnect();
      } finally {
        isConnected = false;
      }
    }
  }

  @Override
  public synchronized boolean usingProxy() {
    return current.usingProxy();
  }

  @Override
  @Nullable
  public synchronized InputStream getErrorStream() {
    return current.getErrorStream();
  }

  @Override
  public synchronized Permission getPermission() throws IOException {
    return current.getPermission();
  }

  @Override
  public synchronized String getRequestMethod() {
    if (isConnected) {
      return current.getRequestMethod();
    } else {
      return super.getRequestMethod();
    }
  }

  @Override
  public  synchronized void setRequestMethod(final String method) throws ProtocolException {
    if (isConnected) {
      current.setRequestMethod(method);
    } else {
      super.setRequestMethod(method);
    }
  }

  @Override
  public synchronized boolean getInstanceFollowRedirects() {
    if (isConnected) {
      return current.getInstanceFollowRedirects();
    } else {
      return super.getInstanceFollowRedirects();
    }
  }

  @Override
  public synchronized void setInstanceFollowRedirects(final boolean followRedirects) {
    if (isConnected) {
      current.setInstanceFollowRedirects(followRedirects);
    } else {
      super.setInstanceFollowRedirects(followRedirects);
    }
  }


  @Override
  public synchronized void setChunkedStreamingMode(final int chunklen) {
    if (isConnected) {
      current.setChunkedStreamingMode(chunklen);
    } else {
      super.setChunkedStreamingMode(chunklen);
    }
  }

  @Override
  public synchronized void setFixedLengthStreamingMode(final long contentLength) {
    if (isConnected) {
      current.setFixedLengthStreamingMode(contentLength);
    } else {
      super.setFixedLengthStreamingMode(contentLength);
    }
  }

  @Override
  public synchronized void setFixedLengthStreamingMode(final int contentLength) {
    if (isConnected) {
      current.setFixedLengthStreamingMode(contentLength);
    } else {
      super.setFixedLengthStreamingMode(contentLength);
    }
  }

  @Override
  @Nullable
  public synchronized String getHeaderFieldKey(final int n) {
    return current.getHeaderFieldKey(n);
  }

  @Override
  public synchronized Map<String, List<String>> getRequestProperties() {
    if (isConnected) {
      return current.getRequestProperties();
    } else {
      return super.getRequestProperties();
    }
  }

  @Override
  @Nullable
  public synchronized String getRequestProperty(final String key) {
    if (isConnected) {
      return current.getRequestProperty(key);
    } else {
      return super.getRequestProperty(key);
    }
  }

  @Override
  public synchronized void addRequestProperty(final String key, final String value) {
    if (isConnected) {
      current.addRequestProperty(key, value);
    } else {
      super.addRequestProperty(key, value);
    }
  }

  @Override
  public  synchronized void setRequestProperty(final String key, final String value) {
    if (isConnected) {
      current.setRequestProperty(key, value);
    } else {
      super.setRequestProperty(key, value);
    }
  }

  @Override
  public synchronized void setDefaultUseCaches(final boolean defaultusecaches) {
    if (isConnected) {
      current.setDefaultUseCaches(defaultusecaches);
    } else {
      super.setDefaultUseCaches(defaultusecaches);
    }
  }

  @Override
  public synchronized boolean getDefaultUseCaches() {
    if (isConnected) {
      return current.getDefaultUseCaches();
    } else {
      return super.getDefaultUseCaches();
    }
  }

  @Override
  public synchronized long getIfModifiedSince() {
    if (isConnected) {
      return current.getIfModifiedSince();
    } else {
      return super.getIfModifiedSince();
    }
  }

  @Override
  public synchronized void setIfModifiedSince(final long ifmodifiedsince) {
    if (isConnected) {
      current.setIfModifiedSince(ifmodifiedsince);
    } else {
      super.setIfModifiedSince(ifmodifiedsince);
    }
  }

  @Override
  public synchronized boolean getUseCaches() {
    if (isConnected) {
      return current.getUseCaches();
    } else {
      return super.getUseCaches();
    }
  }

  @Override
  public synchronized void setUseCaches(final boolean usecaches) {
    if (isConnected) {
      current.setUseCaches(usecaches);
    } else {
      super.setUseCaches(usecaches);
    }
  }

  @Override
  public synchronized boolean getAllowUserInteraction() {
    if (isConnected) {
      return current.getAllowUserInteraction();
    } else {
      return super.getAllowUserInteraction();
    }
  }

  @Override
  public synchronized void setAllowUserInteraction(final boolean allowuserinteraction) {
    if (isConnected) {
      current.setAllowUserInteraction(allowuserinteraction);
    } else {
      super.setAllowUserInteraction(allowuserinteraction);
    }
  }

  @Override
  public synchronized boolean getDoOutput() {
    if (isConnected) {
      return current.getDoOutput();
    } else {
      return super.getDoOutput();
    }
  }

  @Override
  public synchronized void setDoOutput(final boolean dooutput) {
    if (isConnected) {
      current.setDoOutput(dooutput);
    } else {
      super.setDoOutput(dooutput);
    }
  }

  @Override
  public synchronized boolean getDoInput() {
    if (isConnected) {
      return current.getDoInput();
    } else {
      return super.getDoInput();
    }
  }

  @Override
  public synchronized void setDoInput(final boolean doinput) {
    if (isConnected) {
      current.setDoInput(doinput);
    } else {
      super.setDoInput(doinput);
    }
  }


  @Override
  public synchronized Object getContent(final Class[] classes) throws IOException {
    return current.getContent(classes);
  }

  @Override
  public synchronized Object getContent() throws IOException {
    return current.getContent();
  }

  @Override
  public  synchronized long getHeaderFieldLong(final String name, final long defaultp) {
    return current.getHeaderFieldLong(name, defaultp);
  }

  @Override
  public  synchronized int getHeaderFieldInt(final String name, final int defaultp) {
    return current.getHeaderFieldInt(name, defaultp);
  }

  @Override
  public synchronized Map<String, List<String>> getHeaderFields() {
    return current.getHeaderFields();
  }

  @Override
  @Nullable
  public synchronized String getHeaderField(final String name) {
    return current.getHeaderField(name);
  }

  @Override
  public synchronized long getLastModified() {
    return current.getLastModified();
  }

  @Override
  public synchronized long getDate() {
    return current.getDate();
  }

  @Override
  public synchronized long getExpiration() {
    return current.getExpiration();
  }

  @Override
  public synchronized String getContentEncoding() {
    return current.getContentEncoding();
  }

  @Override
  public synchronized String getContentType() {
    return current.getContentType();
  }

  @Override
  public synchronized long getContentLengthLong() {
    return current.getContentLengthLong();
  }

  @Override
  public synchronized int getContentLength() {
    return current.getContentLength();
  }

  @Nullable
  @Override
  public synchronized String getHeaderField(final int n) {
    return current.getHeaderField(n);
  }

  @Override
  public long getHeaderFieldDate(final String name, final long pDefault) {
    return current.getHeaderFieldDate(name, pDefault);
  }




  @Override
  public String toString() {
    return "MultiHttpURLConnection{" + "urls=" + Arrays.toString(urls) + ", choice=" + choice + ", current="
            + current + ", isConnected=" + isConnected + '}';
  }


}
