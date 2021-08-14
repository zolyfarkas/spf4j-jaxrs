/*
 * Copyright 2021 SPF4J.
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

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.eclipse.microprofile.config.Config;
import org.spf4j.base.Either;

/**
 *
 * @author Zoltan Farkas
 */
public interface ExtendedConfig extends Config, ObservableConfig {

    String PROPERTY_NAME = "_xcfg";

    Object convert(Type propertyType, String value);

    @Nullable
    Object getValue(String propertyName, Type propertyType, @Nullable String defaultValue);

    /**
     * Returns a Observable Value supplier.
     * A get invocation returns a value that is kept up to date asynchronously.
     * due to this asynchronous update, when disposing this object, close method must be invoked on the supplier.
     * @param <T>
     * @param propertyName
     * @param propertyType
     * @param defaultValue
     * @param isNullable
     * @return
     */
    default <T> ObservableSupplier<T> getObservableValueSupplier(String propertyName,
            Class<T> propertyType, String defaultValue, boolean isNullable) {
      return new ObservableRXConfigSupplier(this,  new ConfigurationParam(propertyName, defaultValue, isNullable),
              Either.left(propertyType));
    }

    /**
     * Returns a Observable Value supplier.
     * A get invocation returns a value that is kept up to date asynchronously.
     * the supplied conversion is done asynchronously
     * due to this asynchronous update, when disposing this object, close method must be invoked on the supplier.
     * @param <T>
     * @param propertyName
     * @param propertyType
     * @param defaultValue
     * @param isNullable
     * @param customConverter
     * @return
     */
    default <T> ObservableSupplier<T> getObservableValueSupplier(String propertyName,
             String defaultValue, boolean isNullable, Function<String, T> customConverter) {
      return new ObservableRXConfigSupplier(this,  new ConfigurationParam(propertyName, defaultValue, isNullable),
              Either.right(customConverter));
    }

    /**
     * Returns a simple value supplier. By simple we mean that every get invocation
     * results in a configuration read and conversion.
     * @param <T>
     * @param propertyName
     * @param propertyType
     * @param defaultValue
     * @param isNullable
     * @return
     */
    default <T> Supplier<T> getValueSupplier(String propertyName,
            Class<T> propertyType, String defaultValue, boolean isNullable) {
      return new SimpleConfigSupplier(this, new ConfigurationParam(propertyName, defaultValue, isNullable),
              propertyType);
    }

    /**
     * Returns a simple value Provider. By simple we mean that every get invocation
     * results in a configuration read and conversion.
     * @param <T>
     * @param propertyName
     * @param propertyType
     * @param defaultValue
     * @param isNullable
     * @return
     */
    default <T> Provider<T> getValueProvider(String propertyName,
            Class<T> propertyType, String defaultValue, boolean isNullable) {
      return new SimpleConfigSupplier(this, new ConfigurationParam(propertyName, defaultValue, isNullable),
              propertyType);
    }

}
