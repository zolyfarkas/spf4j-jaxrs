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
package org.spf4j.cluster;

import java.net.InetAddress;
import java.util.Set;
import javax.annotation.Nullable;
import org.spf4j.service.avro.NetworkService;

/**
 *
 * @author Zoltan Farkas
 */
public interface ServiceInfo {

  Set<InetAddress> getLocalAddresses();

  Set<NetworkService> getServices();

  @Nullable
  default NetworkService getService(final String name) {
    for (NetworkService svc : getServices()) {
      if (name.equals(svc.getName())) {
        return svc;
      }
    }
    return null;
  }

}
