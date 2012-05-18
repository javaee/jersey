/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.process.internal;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.process.Inflector;

import com.google.common.base.Optional;

/**
 * Applies all request transformations and returns a continuation with the transformed
 * request on the {@link Pair#left() left side} and an ({@link Optional optional})
 * request-to-response inflector on the {@link Pair#right() right side}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface RequestProcessor extends Stage<Request, Optional<Inflector<Request, Response>>> {
    /**
     * Request accepting context.
     */
    public static interface AcceptingContext {
        /**
         * Set the request to response inflector.
         *
         * This method can be used in a non-terminal stage to set the inflector that
         * can be retrieved and processed by a subsequent stage.
         *
         * @param inflector request to response inflector. Must not be {@code null}.
         */
        public void setInflector(Inflector<Request, Response> inflector);

        /**
         * Set the request to response inflector optionally.
         *
         * This method can be used in a non-terminal stage to optionally set the
         * inflector that can be retrieved and processed by a subsequent stage.
         *
         * @param inflector optional request to response inflector. Must not be
         *                  {@code null}.
         */
        public void setInflector(Optional<Inflector<Request, Response>> inflector);

        /**
         * Get the (optional) request to response inflector.
         *
         * @return optional request to response inflector.
         */
        public Optional<Inflector<Request, Response>> getInflector();
    }

    /**
     * Traverse through the nested request stages and apply request transformations
     * until a terminal stage {@link Inflecting providing a request-to-response inflector}
     * is reached. If the terminal stage does not provide an inflector, the inflector
     * returned on the {@link Pair#right() right side} of the continuation will
     * be {@link Optional#absent() absent}.
     *
     * @param request request data to be transformed
     * @return continuation with the transformed request on the {@link Pair#left()
     *         left side} and the (optional) request-to-response inflector on the
     *         {@link Pair#right() right side}.
     */
    @Override
    public Pair<Request, Optional<Inflector<Request, Response>>> apply(Request request);
}
