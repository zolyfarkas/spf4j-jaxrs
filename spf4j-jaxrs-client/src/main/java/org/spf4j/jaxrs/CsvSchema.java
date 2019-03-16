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
package org.spf4j.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for Query,Path parameters that are comma (or other char) separated values.
 *
 * the values will be automatically parsed correctly into a collection.
 *
 * Example:
 *
 * <li>
 * public List&lt;Integer&gt; echoList(@CsvParam @QueryParam("lp") List&lt;Integer&gt; param)
 * </li>
 *
 * @author Zoltan Farkas
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CsvSchema {
    /** the separator character */
    Class<? extends FieldSchemaSource> value();
}
