/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.util.collection;

import java.lang.reflect.Type;

import org.glassfish.hk2.TypeLiteral;

import org.jvnet.tiger_types.Types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * A pair of instance and the public (official) Java type of the instance.
 *
 * @param <T> instance type.
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class InstanceTypePair<T> implements Pair<T, Type> {

    private final T t;
    private final Type type;

    private InstanceTypePair(T t) {
        this.t = t;
        this.type = t.getClass();
    }

    private InstanceTypePair(T t, TypeLiteral<T> type) {
        this.t = t;
        this.type = type.getType();
    }

    private InstanceTypePair(T t, Type type) {
        checkArgument(Types.isSubClassOf(t.getClass(), type), "Supplied type not compatible with instance.");
        this.t = t;
        this.type = type;
    }

    @Override
    public T left() {
        return t;
    }

    @Override
    public Type right() {
        return type;
    }

    /**
     * Get stored instance.
     *
     * @return stored instance.
     */
    public T instance() {
        return left();
    }

    /**
     * Get the public type of the stored instance.
     *
     * @return public type of the stored instance.
     */
    public Type type() {
        return right();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).
                add("instance", t).
                add("type", type).toString();
    }

    /**
     * Create new instance-type pair from an instance.
     *
     * @param <T> instance type.
     * @param instance instance.
     * @return new instance-type pair.
     */
    public static <T> InstanceTypePair<T> of(final T instance) {
        return new InstanceTypePair<T>(instance);
    }

    /**
     * Create new instance-type pair from an instance.
     *
     * @param <T> instance type.
     * @param instance instance.
     * @param type type literal representing the generic public type of the instance.
     * @return new instance-type pair.
     */
    public static <T> InstanceTypePair<T> of(final T instance, final TypeLiteral<T> type) {
        return new InstanceTypePair<T>(instance, type);
    }

    /**
     * Create new instance-type pair from an instance.
     *
     * @param <T> instance type.
     * @param instance instance.
     * @param type public type of the instance.
     * @return new instance-type pair.
     */
    public static <T> InstanceTypePair<T> of(final T instance, final Type type) {
        return new InstanceTypePair<T>(instance, type);
    }
}
