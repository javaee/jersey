/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.internal.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Utility class to print the variety of types, useful in {@code toString} method.
 *
 * @author John Wells (john.wells at oracle.com)
 */
public class Pretty {

    private static final String DOT = ".";
    private static final String NULL_STRING = "null";
    private static final String CONSTRUCTOR_NAME = "<init>";

    /**
     * Private constructor to forbid the creation of Pretty instance.
     */
    private Pretty() {
    }

    /**
     * Makes a nice, pretty class (without the package name).
     *
     * @param clazz Make me a pretty class.
     * @return A nice string of the class, with no package.
     */
    public static String clazz(Class<?> clazz) {
        if (clazz == null) {
            return NULL_STRING;
        }

        String cn = clazz.getName();

        int index = cn.lastIndexOf(DOT);
        if (index < 0) {
            return cn;
        }

        // If this fails, the class name somehow ends in dot, which should be illegal
        return cn.substring(index + 1);
    }

    /**
     * Prints a pretty parameterized type.
     *
     * @param pType The parameterized type to print. May not be null.
     * @return A pretty string version of the parameterized type.
     */
    public static String pType(ParameterizedType pType) {
        StringBuffer sb = new StringBuffer();

        sb.append(clazz(ReflectionHelper.getRawClass(pType)) + "<");

        boolean first = true;
        for (Type t : pType.getActualTypeArguments()) {
            if (first) {
                first = false;
                sb.append(type(t));
            } else {
                sb.append("," + type(t));
            }
        }

        sb.append(">");

        return sb.toString();
    }

    /**
     * Returns a pretty string for the given type.
     *
     * @param t A possibly null type.
     * @return A pretty string representing the type.
     */
    public static String type(Type t) {
        if (t == null) {
            return NULL_STRING;
        }

        if (t instanceof Class) {
            return clazz((Class<?>) t);
        }

        if (t instanceof ParameterizedType) {
            return pType((ParameterizedType) t);
        }

        return t.toString();
    }

    /**
     * Make a nice pretty string out of the constructor and all its parameters.
     *
     * @param constructor The constructor to make pretty.
     * @return A nice pretty string.
     */
    public static String constructor(Constructor<?> constructor) {
        if (constructor == null) {
            return NULL_STRING;
        }

        return CONSTRUCTOR_NAME + prettyPrintParameters(constructor.getParameterTypes());
    }

    /**
     * Makes a nice pretty string of the method, with the method name and all parameters.
     *
     * @param method The method to make pretty.
     * @return A nice pretty string.
     */
    public static String method(Method method) {
        if (method == null) {
            return NULL_STRING;
        }

        return method.getName() + prettyPrintParameters(method.getParameterTypes());
    }

    /**
     * Returns a pretty string representing a Field.
     *
     * @param field The possibly null field.
     * @return A pretty string representing the field.
     */
    public static String field(Field field) {
        if (field == null) {
            return NULL_STRING;
        }

        Type t = field.getGenericType();

        String baseString;
        if (t instanceof Class) {
            baseString = clazz((Class<?>) t);
        } else {
            baseString = type(t);
        }

        return "field(" + baseString + " " + field.getName() + " in " + field.getDeclaringClass().getName() + ")";
    }

    /**
     * Returns a pretty string for the given array.
     *
     * @param array The possibly null array to represent.
     * @return A pretty string representation of the array.
     */
    public static String array(Object[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        StringBuffer sb = new StringBuffer("{");

        boolean first = true;
        for (Object item : array) {
            if (item != null && (item instanceof Class)) {
                item = Pretty.clazz((Class<?>) item);
            }

            if (first) {
                first = false;

                sb.append((item == null) ? "null" : item.toString());
            } else {
                sb.append("," + ((item == null) ? "null" : item.toString()));
            }
        }

        sb.append("}");

        return sb.toString();
    }

    /**
     * Returns a pretty string representing the collection.
     *
     * @param collection A possibly null collection to represent.
     * @return A pretty string representing the collection.
     */
    public static String collection(Collection<?> collection) {
        if (collection == null) {
            return NULL_STRING;
        }
        return array(collection.toArray(new Object[collection.size()]));
    }

    private static String prettyPrintParameters(Class<?> params[]) {
        if (params == null) {
            return NULL_STRING;
        }

        StringBuffer sb = new StringBuffer("(");

        boolean first = true;
        for (Class<?> param : params) {
            if (first) {
                sb.append(clazz(param));
                first = false;
            } else {
                sb.append("," + clazz(param));
            }
        }

        sb.append(")");

        return sb.toString();
    }

}
