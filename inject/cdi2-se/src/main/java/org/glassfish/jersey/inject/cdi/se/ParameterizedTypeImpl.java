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

package org.glassfish.jersey.inject.cdi.se;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Simple implementation of {@link ParameterizedType}.
 * <p>
 * John Wells (john.wells at oracle.com)
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private final Type rawType;
    private final Type actualTypeArguments[];

    /**
     * A new parameterized type.
     *
     * @param rawType             The raw type of this type.
     * @param actualTypeArguments The actual type arguments.
     */
    public ParameterizedTypeImpl(Type rawType, Type... actualTypeArguments) {
        this.rawType = rawType;
        this.actualTypeArguments = actualTypeArguments;
    }

    /* (non-Javadoc)
     * @see java.lang.reflect.ParameterizedType#getActualTypeArguments()
     */
    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments;
    }

    /* (non-Javadoc)
     * @see java.lang.reflect.ParameterizedType#getRawType()
     */
    @Override
    public Type getRawType() {
        return rawType;
    }

    /* (non-Javadoc)
     * @see java.lang.reflect.ParameterizedType#getOwnerType()
     * This is only used for top level types
     */
    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public int hashCode() {
        int retVal = Arrays.hashCode(actualTypeArguments);
        if (rawType == null) {
            return retVal;
        }
        return retVal ^ rawType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType other = (ParameterizedType) o;

        if (!rawType.equals(other.getRawType())) {
            return false;
        }

        Type otherActuals[] = other.getActualTypeArguments();

        if (otherActuals.length != actualTypeArguments.length) {
            return false;
        }

        for (int lcv = 0; lcv < otherActuals.length; lcv++) {
            if (!actualTypeArguments[lcv].equals(otherActuals[lcv])) {
                return false;
            }
        }

        return true;
    }
}
