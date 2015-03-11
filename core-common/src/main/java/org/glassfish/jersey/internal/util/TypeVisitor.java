/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Type visitor contract.
 *
 * @param <T> type visiting result type.
 * @author Kohsuke Kawaguchi
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class TypeVisitor<T> {

    /**
     * Visit the type and a given parameter.
     *
     * @param type visited type.
     * @return visiting result.
     */
    public final T visit(final Type type) {
        assert type != null;

        if (type instanceof Class) {
            return onClass((Class) type);
        }
        if (type instanceof ParameterizedType) {
            return onParameterizedType((ParameterizedType) type);
        }
        if (type instanceof GenericArrayType) {
            return onGenericArray((GenericArrayType) type);
        }
        if (type instanceof WildcardType) {
            return onWildcard((WildcardType) type);
        }
        if (type instanceof TypeVariable) {
            return onVariable((TypeVariable) type);
        }

        // covered all the cases
        assert false;

        throw createError(type);
    }

    /**
     * Visit class.
     *
     * @param clazz visited class.
     * @return visit result.
     */
    protected abstract T onClass(Class clazz);

    /**
     * Visit parameterized type.
     *
     * @param type visited parameterized type.
     * @return visit result.
     */
    protected abstract T onParameterizedType(ParameterizedType type);

    /**
     * Visit generic array type.
     *
     * @param type visited parameterized type.
     * @return visit result.
     */
    protected abstract T onGenericArray(GenericArrayType type);

    /**
     * Visit type variable.
     *
     * @param type visited parameterized type.
     * @return visit result.
     */
    protected abstract T onVariable(TypeVariable type);

    /**
     * Visit wildcard type.
     *
     * @param type visited parameterized type.
     * @return visit result.
     */
    protected abstract T onWildcard(WildcardType type);

    /**
     * Create visiting error (in case the visitor could not recognize the visit type.
     *
     * @param type visited parameterized type.
     * @return visit result.
     */
    protected RuntimeException createError(final Type type) {
        throw new IllegalArgumentException();
    }
}
