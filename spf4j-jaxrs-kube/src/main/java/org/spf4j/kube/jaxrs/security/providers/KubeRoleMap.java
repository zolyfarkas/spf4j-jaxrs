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
package org.spf4j.kube.jaxrs.security.providers;

import com.google.common.base.Suppliers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.hash.THashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.kube.client.Client;
import org.spf4j.kube.client.RoleBindings;
import org.spf4j.kube.client.RoleBindings.Subject;

/**
 * @author Zoltan Farkas
 */
public final class KubeRoleMap {

  private final Supplier<Map<String, Set<String>>> roleMapSupplier;


  @Inject
  public KubeRoleMap(final Client kubeClient,
          @ConfigProperty(name = "jaxrs.service.auth.roleCacheTimeMillis", defaultValue = "10000")
          final long cacheMillis) {
    this.roleMapSupplier = Suppliers.memoizeWithExpiration(() -> {
      Map<String, Set<String>> result = new HashMap<>();
      putRoleBindings(kubeClient.getClusterRoleBindings(), result);
      putRoleBindings(kubeClient.getRoleBindings(), result);
      return result;
    }, cacheMillis, TimeUnit.MILLISECONDS);
  }

  @SuppressFBWarnings("STT_TOSTRING_MAP_KEYING") // this is the string that the tokenreview method will return.
  private static void putRoleBindings(final RoleBindings rb, final Map<String, Set<String>> result) {
    for (RoleBindings.RoleBinding item : rb.getItems()) {
      RoleBindings.RoleRef roleRef = item.getRoleRef();
      String roleName = roleRef.getName();
      for (Subject subj : item.getSubjects()) {
        String namespace = subj.getNamespace();
        String subjName = "system:" + subj.getKind().toLowerCase(Locale.getDefault())
                + (namespace == null ? "" : ':' + namespace)
                + ':' + subj.getName();
        Set<String> roles = result.get(subjName);
        if (roles == null) {
          roles = new THashSet<>(4);
          result.put(subjName, roles);
        }
        roles.add(roleName);
      }
    }
  }

  public Set<String> getRoles(final String subjectName) {
    return roleMapSupplier.get().get(subjectName);
  }

  @Override
  public String toString() {
    return "KubeRoleMap{" + "roleMapSupplier=" + roleMapSupplier + '}';
  }

}
