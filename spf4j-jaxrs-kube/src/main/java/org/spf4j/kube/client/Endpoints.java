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
import org.spf4j.base.avro.NetworkProtocol;

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
public final class Endpoints {

  @AvroDefault("[]")
  private List<SubSet> subsets;

  public List<SubSet> getSubsets() {
    return subsets;
  }

  public void setSubsets(final List<SubSet> subsets) {
    this.subsets = subsets;
  }

  public static final class Address {
    private  String ip;


    public String getIp() {
      return ip;
    }

    public void setIp(final String ip) {
      this.ip = ip;
    }

    @Override
    public String toString() {
      return "Address{" + "ip=" + ip + '}';
    }

  }

  public static final class Port {
    private int port;

    @AvroDefault("\"\"")
    private String name;

    private NetworkProtocol protocol;

    public int getPort() {
      return port;
    }

    public void setPort(final int port) {
      this.port = port;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public NetworkProtocol getProtocol() {
      return protocol;
    }

    public void setProtocol(final NetworkProtocol protocol) {
      this.protocol = protocol;
    }

    @Override
    public String toString() {
      return "Port{" + "port=" + port + ", name=" + name + ", protocol=" + protocol + '}';
    }

  }

  public static final class SubSet {

    @AvroDefault("[]")
    private List<Address> addresses;

    @AvroDefault("[]")
    private List<Address> notReadyAddresses;

    private List<Port> ports;

    public List<Address> getAddresses() {
      return addresses;
    }

    public void setAddresses(final List<Address> addresses) {
      this.addresses = addresses;
    }

    public List<Address> getNotReadyAddresses() {
      return notReadyAddresses;
    }

    public void setNotReadyAddresses(final List<Address> notReadyAddresses) {
      this.notReadyAddresses = notReadyAddresses;
    }

    public List<Port> getPorts() {
      return ports;
    }

    public void setPorts(final List<Port> ports) {
      this.ports = ports;
    }

    @Override
    public String toString() {
      return "SubSet{" + "addresses=" + addresses + ", ports=" + ports + '}';
    }

  }

  @Override
  public String toString() {
    return "Endpoints{" + "subsets=" + subsets + '}';
  }


}
