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
package org.spf4j.jaxrs.client.security.providers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.spf4j.base.Base64;

/**
 *
 * @author Zoltan Farkas
 */
public final class BasicAuthorizationUserPassword {

  private static final String AUTH_METHOD = "Basic";

  private final String user;

  private final String password;

  public BasicAuthorizationUserPassword(final String user, final String password) {
    if (user.indexOf(':') >= 0) {
      throw new IllegalArgumentException("Invalid user name: " + user);
    }
    this.user = user;
    this.password = password;
  }

  public BasicAuthorizationUserPassword(final CharSequence headerValue) {
    this(headerValue, StandardCharsets.US_ASCII);
  }

  public BasicAuthorizationUserPassword(final CharSequence headerValue, final Charset charset) {
    int skip = AUTH_METHOD.length() + 1;
    String usrPwd = new String(Base64.decodeBase64(headerValue, skip, headerValue.length() - skip), charset);
    int cIdx = usrPwd.indexOf(':');
    if (cIdx < 0) {
      throw new IllegalArgumentException("Invalid Vasic Auth header " + headerValue + ", charset " + charset);
    }
    this.user = usrPwd.substring(0, cIdx);
    this.password = usrPwd.substring(cIdx + 1);
  }

  @Override
  public int hashCode() {
    int hash = 53 + Objects.hashCode(this.user);
    return 53 * hash + Objects.hashCode(this.password);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final BasicAuthorizationUserPassword other = (BasicAuthorizationUserPassword) obj;
    if (!Objects.equals(this.user, other.user)) {
      return false;
    }
    return Objects.equals(this.password, other.password);
  }

  // when upgrading to latests spf4j remove to use variant from Arrays.
  private static byte[] concat(final byte[]... arrays) {
    if (arrays.length < 2) {
      throw new IllegalArgumentException("You should concatenate at least 2 arrays: "
              + arrays.length);
    }
    int newLength = 0;
    for (byte[] arr : arrays) {
      newLength += arr.length;
    }
    byte[] result = new byte[newLength];
    int destIdx = 0;
    for (byte[] arr : arrays) {
      System.arraycopy(arr, 0, result, destIdx, arr.length);
      destIdx += arr.length;
    }
    return result;
  }

  public String toString() {
    return toString(StandardCharsets.US_ASCII);
  }

  public String toString(final Charset charset) {
    StringBuilder result = new StringBuilder(64);
    result.append(AUTH_METHOD).append(' ');
    writeTo(result, charset);
    return result.toString();
  }

  public void writeTo(final StringBuilder result, final Charset charset) {
    try {
      writeTo((Appendable) result, charset);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void writeTo(final Appendable result, final Charset charset) throws IOException {
    byte[] userBytes = user.getBytes(charset);
    byte[] sclBytes = ":".getBytes(charset);
    byte[] pwdBytes = password.getBytes(charset);
    byte[] concat = concat(userBytes, sclBytes, pwdBytes);
    Base64.encodeBase64(concat, 0, concat.length, result);
  }

}
