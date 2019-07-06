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
 *
 * @author Zoltan Farkas
 * {
 * "kind": "ClusterRoleBindingList",
 * "apiVersion": "rbac.authorization.k8s.io/v1",
 * "metadata":{
 * "selfLink": "/apis/rbac.authorization.k8s.io/v1/clusterrolebindings",
 * "resourceVersion": "48722"
 * },
 * "items":[
 * {
 * "metadata":{"name": "add-on-cluster-admin",
 * "selfLink": "/apis/rbac.authorization.k8s.io/v1/clusterrolebindings/add-on-cluster-admin",…},
 * "subjects":[
 * {"kind": "ServiceAccount", "name": "default", "namespace": "kube-system"}
 * ],
 * "roleRef":{
 * "apiGroup": "rbac.authorization.k8s.io",
 * "kind": "ClusterRole",
 * "name": "cluster-admin"
 * }
 * },
 *
 */
public final class RoleBindings {

  /** ClusterRoleBindingList or RoleBindingList */
  private String kind;

  private MetaData metadata;

  private List<ClusterRoleBinding> items;

  public String getKind() {
    return kind;
  }

  public void setKind(final String kind) {
    this.kind = kind;
  }

  public MetaData getMetadata() {
    return metadata;
  }

  public void setMetadata(final MetaData metadata) {
    this.metadata = metadata;
  }

  public List<ClusterRoleBinding> getItems() {
    return items;
  }

  public void setItems(final List<ClusterRoleBinding> items) {
    this.items = items;
  }

  @Override
  public String toString() {
    return "ClusterRoleBindings{" + "metadata=" + metadata + ", items=" + items + '}';
  }



  public static final class MetaData {
    private String resourceVersion;

    public String getResourceVersion() {
      return resourceVersion;
    }

    public void setResourceVersion(final String resourceVersion) {
      this.resourceVersion = resourceVersion;
    }

    @Override
    public String toString() {
      return "MetaData{" + "resourceVersion=" + resourceVersion + '}';
    }


  }

  public static final class ClusterRoleBinding {

    @AvroDefault("[]")
    private List<Subject> subjects;
    private RoleRef roleRef;

    public List<Subject> getSubjects() {
      return subjects;
    }

    public void setSubjects(final List<Subject> subjects) {
      this.subjects = subjects;
    }

    public RoleRef getRoleRef() {
      return roleRef;
    }

    public void setRoleRef(final RoleRef roleRef) {
      this.roleRef = roleRef;
    }

    @Override
    public String toString() {
      return "ClusterRoleBinding{" + "subjects=" + subjects + ", roleRef=" + roleRef + '}';
    }


  }

  public static final class Subject {

    private String kind;

    private String name;

    @Nullable
    private String namespace;

    public String getKind() {
      return kind;
    }

    public void setKind(final String kind) {
      this.kind = kind;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(final String namespace) {
      this.namespace = namespace;
    }

    @Override
    public String toString() {
      return "Subject{" + "kind=" + kind + ", name=" + name + ", namespace=" + namespace + '}';
    }

  }

  public static final class RoleRef {

    private String kind;

    private String name;

    public String getKind() {
      return kind;
    }

    public void setKind(final String kind) {
      this.kind = kind;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "RoleRef{" + "kind=" + kind + ", name=" + name + '}';
    }

  }

}
