/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Michal Gajdos
 */
public class JsonTestHelper {

    public static String getResourceAsString(String prefix, String resource) throws IOException {
        return getEntityAsString(Thread.currentThread().getContextClassLoader().getResourceAsStream(prefix + resource));
    }

    public static String getEntityAsString(InputStream inputStream) throws IOException {
        Reader reader = new InputStreamReader(inputStream);
        StringBuilder sb = new StringBuilder();
        char[] c = new char[1024];
        int l;
        while ((l = reader.read(c)) != -1) {
            sb.append(c, 0, l);
        }
        return sb.toString();
    }

    public static <T> T createTestInstance(Class<T> clazz) {
        try {
            Method createMethod = clazz.getDeclaredMethod("createTestInstance");
            //noinspection unchecked
            return (T) createMethod.invoke(clazz);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean isCollectionEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean areCollectionsEqual(final Collection<T> collection1, final Collection<T> collection2) {
        return collection1 == collection2
                || (isCollectionEmpty(collection1) && isCollectionEmpty(collection2))
                || (collection1 != null && collection1.equals(collection2));
    }

    public static boolean isArrayEmpty(final Object[] array) {
        return array == null || array.length == 0;
    }

    public static <T> boolean areArraysEqual(final T[] array1, final T[] array2) {
        return array1 == array2
                || (isArrayEmpty(array1) && isArrayEmpty(array2))
                || Arrays.equals(array1, array2);
    }

}
