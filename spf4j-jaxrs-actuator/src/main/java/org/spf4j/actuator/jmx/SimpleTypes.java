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
package org.spf4j.actuator.jmx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import static javax.management.openmbean.SimpleType.BIGDECIMAL;
import static javax.management.openmbean.SimpleType.BIGINTEGER;
import static javax.management.openmbean.SimpleType.BOOLEAN;
import static javax.management.openmbean.SimpleType.BYTE;
import static javax.management.openmbean.SimpleType.CHARACTER;
import static javax.management.openmbean.SimpleType.DATE;
import static javax.management.openmbean.SimpleType.DOUBLE;
import static javax.management.openmbean.SimpleType.FLOAT;
import static javax.management.openmbean.SimpleType.INTEGER;
import static javax.management.openmbean.SimpleType.LONG;
import static javax.management.openmbean.SimpleType.OBJECTNAME;
import static javax.management.openmbean.SimpleType.SHORT;
import static javax.management.openmbean.SimpleType.STRING;
import static javax.management.openmbean.SimpleType.VOID;
import org.spf4j.base.Reflections;

/**
 *
 * @author Zoltan Farkas
 */
public final class SimpleTypes {

    private static final SimpleType<?>[] TYPE_ARRAY = {
        VOID, BOOLEAN, CHARACTER, BYTE, SHORT, INTEGER, LONG, FLOAT,
        DOUBLE, STRING, BIGDECIMAL, BIGINTEGER, DATE, OBJECTNAME,
    };

    private static final Map<String, SimpleType> MAP = new HashMap<>();

    private static final Map<String, SimpleType> PRIMITIVES = new HashMap<>();


    static {
      for (SimpleType type : TYPE_ARRAY) {
        String className = type.getClassName();
        Class<?> clasz;
        try {
          clasz = Reflections.forName(className);
        } catch (ClassNotFoundException ex) {
          throw new ExceptionInInitializerError(ex);
        }
        MAP.put(clasz.getName(), type);
        Class<?> prim = Reflections.wrapperToPrimitive(clasz);
        if (prim != null && clasz != prim) {
          MAP.put(prim.getName(), type);
          PRIMITIVES.put(prim.getName(), type);
        }
      }
    }

    private SimpleTypes() { }

    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    public static OpenType getOpenType(final String typeName) {
      if (typeName.startsWith("[")) {
        try {
          // arrays
          Class<?> arrayClass = Class.forName(typeName);
          OpenType openType = getOpenType(arrayClass.getComponentType().getName());
          if (openType instanceof ArrayType) {
            ArrayType arrType = (ArrayType) openType;
            return new ArrayType(arrType.getDimension() + 1, arrType.getElementOpenType());
          } else if (openType instanceof SimpleType) {
            return new ArrayType((SimpleType) openType, PRIMITIVES.containsKey(openType.getClassName()));
          }
        } catch (ClassNotFoundException | OpenDataException ex) {
          throw new IllegalArgumentException("Invalid class " + typeName, ex);
        }
      }
      SimpleType st = MAP.get(typeName);
      if (st == null) {
        throw new IllegalArgumentException("Not a simple type: " + typeName);
      }
      return st;
    }

}
