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
package org.spf4j.kube.client;

import java.util.List;
import org.apache.avro.reflect.AvroDefault;
import org.apache.avro.reflect.Nullable;

/**
 * A token review object.
 *
 * {
 * "kind": "TokenReview",
 * "apiVersion": "authentication.k8s.io/v1",
 * "metadata": {
 *   "creationTimestamp": null
 * },
 * "spec": {
 *   "token": "..."
 * },
 * "status": {
 *   "authenticated": true,
 *   "user": {
 *     "username": "system:serviceaccount:default:default",
 *     "uid": "bf23ffa5-321c-11e9-a996-ae4090695699",
 *     "groups": [
 *       "system:serviceaccounts",
 *       "system:serviceaccounts:default",
 *       "system:authenticated"
 *     ]
 *   }
 * }
 * }
 *
 * @author Zoltan Farkas
 */
public final class TokenReview {

  private Token spec;

  @Nullable
  private Status status;


  public static final class Token {
    private String token;

    public Token(final String token) {
      this.token = token;
    }

    public Token() {
    }

    public String getToken() {
      return token;
    }

    public void setToken(final String token) {
      this.token = token;
    }

    @Override
    public String toString() {
      return "Token{" + "token=" + token + '}';
    }

  }


  public static final class User {

    @Nullable
    private String username;

    @Nullable
    private String uid;

    @AvroDefault("[]")
    private List<String> groups;

    public String getUsername() {
      return username;
    }

    public void setUsername(final String username) {
      this.username = username;
    }

    public String getUid() {
      return uid;
    }

    public void setUid(final String uid) {
      this.uid = uid;
    }

    public List<String> getGroups() {
      return groups;
    }

    public void setGroups(final List<String> groups) {
      this.groups = groups;
    }

    @Override
    public String toString() {
      return "User{" + "username=" + username + ", uid=" + uid + ", groups=" + groups + '}';
    }



  }

  public static final class Status {

    @AvroDefault("false")
    private boolean authenticated;

    @Nullable
    private User user;

    @Nullable
    private String error;

    public boolean isAuthenticated() {
      return authenticated;
    }

    public void setAuthenticated(final boolean authenticated) {
      this.authenticated = authenticated;
    }

    public User getUser() {
      return user;
    }

    public void setUser(final User user) {
      this.user = user;
    }

    public String getError() {
      return error;
    }

    public void setError(final String error) {
      this.error = error;
    }

    @Override
    public String toString() {
      return "Status{" + "authenticated=" + authenticated + ", user=" + user + ", error=" + error + '}';
    }



  }

  public TokenReview(final String token) {
    this.spec = new Token(token);
  }

  public TokenReview() {
  }

  public Token getSpec() {
    return spec;
  }

  public void setSpec(final Token spec) {
    this.spec = spec;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(final Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "TokenReview{" + "spec=" + spec + ", status=" + status + '}';
  }

}
