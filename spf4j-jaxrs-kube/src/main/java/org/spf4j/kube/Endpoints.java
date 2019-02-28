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
package org.spf4j.kube;

import java.util.List;

/**
 * kubernetes endpoints:
 *
 * {
 * "kind": "Endpoints",
 * "apiVersion": "v1",
 * "metadata": {
 *   "name": "jaxrs-spf4j-demo",
 *   "namespace": "default",
 *   "selfLink": "/api/v1/namespaces/default/endpoints/jaxrs-spf4j-demo",
 *   "uid": "0f71954b-32d3-11e9-b289-ae4090695699",
 *   "resourceVersion": "28558",
 *   "creationTimestamp": "2019-02-17T16:42:49Z",
 *   "labels": {
 *     "app": "jaxrs-spf4j-demo",
 *     "version": "0.5-SNAPSHOT"
 *   }
 * },
 * "subsets": [
 *   {
 *     "addresses": [
 *       {
 *         "ip": "10.244.2.5",
 *         "nodeName": "kube-node-1",
 *         "targetRef": {
 *           "kind": "Pod",
 *           "namespace": "default",
 *           "name": "jaxrs-spf4j-demo-567c44c4dd-wl4sx",
 *           "uid": "26226639-32d0-11e9-b289-ae4090695699",
 *           "resourceVersion": "26801"
 *         }
 *       },
 *       {
 *         "ip": "10.244.3.6",
 *         "nodeName": "kube-node-2",
 *         "targetRef": {
 *           "kind": "Pod",
 *           "namespace": "default",
 *           "name": "jaxrs-spf4j-demo-567c44c4dd-hnb6t",
 *           "uid": "26210009-32d0-11e9-b289-ae4090695699",
 *           "resourceVersion": "26804"
 *         }
 *       }
 *     ],
 *     "ports": [
 *       {
 *         "port": 8080,
 *         "protocol": "TCP"
 *       }
 *     ]
 *   }
 * ]
 * }
 *
 * @author Zoltan Farkas
 */
public class Endpoints {

  private List<SubSet> subsets;

  public List<SubSet> getSubsets() {
    return subsets;
  }

  public void setSubsets(List<SubSet> subsets) {
    this.subsets = subsets;
  }

  public static final class Address {
    private  String ip;


    public String getIp() {
      return ip;
    }

    public void setIp(String ip) {
      this.ip = ip;
    }
  }

  public static final class Port {
    private int port;

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

  }

  public static final class SubSet {

    private List<Address> addresses;

    private List<Port> ports;

    public List<Address> getAddresses() {
      return addresses;
    }

    public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
    }

    public List<Port> getPorts() {
      return ports;
    }

    public void setPorts(List<Port> ports) {
      this.ports = ports;
    }

  }

  @Override
  public String toString() {
    return "Endpoints{" + "subsets=" + subsets + '}';
  }


}
