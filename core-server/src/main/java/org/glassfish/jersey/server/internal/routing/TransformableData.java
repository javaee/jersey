/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import org.glassfish.jersey.process.Inflector;

/**
 * Contains the reference to {@link #data() data} as well as an
 * ({@link #hasInflector() optional}) inflector that may be used
 * to transform the data into a result.
 *
 * @param <DATA>  data type.
 * @param <RESULT> result type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class TransformableData<DATA, RESULT> {
    private final DATA data;
    private final Inflector<DATA, RESULT> inflector;

    /**
     * Create a new transformable data with an inflector instance.
     *
     * @param <REQUEST>  data type.
     * @param <RESPONSE> result type.
     * @param data       transformable data.
     * @param inflector  data to result data transformation. May be {code null} if not available.
     * @return new transformable data with an inflector instance.
     */
    public static <REQUEST, RESPONSE> TransformableData<REQUEST, RESPONSE> of(
            final REQUEST data, final Inflector<REQUEST, RESPONSE> inflector) {
        return new TransformableData<REQUEST, RESPONSE>(data, inflector);
    }

    /**
     * Create a new transformable data instance, but with the inflector not present.
     *
     * @param <REQUEST>  data type.
     * @param <RESPONSE> result type.
     * @param data       transformable data.
     * @return new transformable data with {@code null} inflector instance.
     */
    public static <REQUEST, RESPONSE> TransformableData<REQUEST, RESPONSE> of(final REQUEST data) {
        return new TransformableData<REQUEST, RESPONSE>(data, null);
    }

    private TransformableData(final DATA data, final Inflector<DATA, RESULT> inflector) {
        this.data = data;
        this.inflector = inflector;
    }

    /**
     * Get the transformable data.
     *
     * @return transformable data.
     */
    public DATA data() {
        return data;
    }

    /**
     * Get the inflector transforming the data to the result. May return {@code null} if
     * the inflector is not {@link #hasInflector() available}.
     *
     * @return data to result transformation or {@code null} if not available.
     */
    public Inflector<DATA, RESULT> inflector() {
        return inflector;
    }

    /**
     * Check if there is an inflector available to process the data.
     * <p>
     * The absence of an inflector indicates that there was no suitable inflector found
     * for the data.
     * </p>
     *
     * @return {@code true} if there is a transforming inflector available for the data,
     *         {@code false} otherwise.
     */
    public boolean hasInflector() {
        return inflector != null;
    }
}
