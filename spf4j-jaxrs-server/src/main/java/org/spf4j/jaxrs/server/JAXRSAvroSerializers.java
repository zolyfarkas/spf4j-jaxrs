/*
 * Copyright 2020 SPF4J.
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
package org.spf4j.jaxrs.server;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import org.apache.avro.Schema;
import org.glassfish.jersey.client.ClientConfig;

/**
 * @author Zoltan Farkas
 */
public final class JAXRSAvroSerializers {

  private JAXRSAvroSerializers() { }

  public static void registerJaxRsObjectSerializers() {
    SimpleModule module = new SimpleModule("jax-rs");
    module.addSerializer(new ToStringSerializer(AsyncResponse.class));
    module.addSerializer(new ToStringSerializer(ServletRequest.class));
    module.addSerializer(new ToStringSerializer(ServletResponse.class));
    module.addSerializer(new ToStringSerializer(InputStream.class));
    module.addSerializer(new ToStringSerializer(OutputStream.class));
    module.addSerializer(new ToStringSerializer(Reader.class));
    module.addSerializer(new ToStringSerializer(Writer.class));
    module.addSerializer(new ToStringSerializer(ContainerRequestContext.class));
    module.addSerializer(new ToStringSerializer(ContainerResponseContext.class));
    module.addSerializer(new ToStringSerializer(SecurityContext.class));
    module.addSerializer(new ToStringSerializer(StreamingOutput.class));
    module.addSerializer(new ToStringSerializer(Client.class));
    module.addSerializer(new ToStringSerializer(ClientConfig.class));
    module.addSerializer(new ToStringSerializer(WebTarget.class));
    module.addSerializer(new ToStringSerializer(Response.class));
    module.addSerializer(new ToStringSerializer(Request.class));

    Schema.MAPPER.registerModules(module);
  }


}
