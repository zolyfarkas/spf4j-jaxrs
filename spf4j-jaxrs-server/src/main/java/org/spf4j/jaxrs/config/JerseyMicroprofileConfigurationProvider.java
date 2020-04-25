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
package org.spf4j.jaxrs.config;

import java.util.Map;
import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.jersey.spi.ExternalConfigurationModel;
import org.glassfish.jersey.spi.ExternalConfigurationProvider;

/**
 * @author Zoltan Farkas
 */
@Provider
@Priority(0)
@Singleton
public final class JerseyMicroprofileConfigurationProvider implements ExternalConfigurationProvider {

  private JerseyMicroprofileConfigurationModel configModel;

  public JerseyMicroprofileConfigurationProvider() {
    configModel = new JerseyMicroprofileConfigurationModel((ConfigImpl) ConfigProvider.getConfig());
  }

  public ObjectConverters getConverters() {
    return configModel.getConverters();
  }

  @Override
  public Map<String, Object> getProperties() {
    return configModel.getProperties();
  }

  @Override
  public ExternalConfigurationModel getConfiguration() {
    return configModel;
  }

  @Override
  public ExternalConfigurationModel merge(final ExternalConfigurationModel otherModel) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "JerseyMicroprofileConfigurationProvider{" + "configModel=" + configModel + '}';
  }

}
