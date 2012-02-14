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

import org.glassfish.jersey.internal.util.collection.Pair;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Linear request acceptor.
 * <p/>
 * A continuation of a linear acceptor is represented by an ({@link Optional optional})
 * single next linear acceptor resulting in a linear request transformation processing.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface LinearAcceptor extends Stage<Request, Optional<LinearAcceptor>> {

    /**
     * Linear acceptor chain builder.
     */
    public static interface Builder {

        /**
         * Add a transformation function as a next stage to the linear acceptor chain.
         * <p/>
         * The order of the {@code add(...)} method invocations matches the order
         * of the acceptor execution in the {@link LinearRequestProcessor request processor}.
         *
         * @param transformation a transformation function to be added as a next
         *     acceptor to the linear acceptor chain.
         * @return updated builder instance.
         */
        public Builder to(Function<Request, Request> transformation);

        /**
         * Build a acceptor chain.
         *
         * @return built acceptor chain.
         */
        public LinearAcceptor build();

        /**
         * Add terminal acceptor to the acceptor chain and build the chain.
         *
         * @param terminal last acceptor to be added to the acceptor chain.
         * @return built acceptor chain.
         */
        public LinearAcceptor build(LinearAcceptor terminal);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The returned continuation is ({@link Optional optionally}) the next linear
     * acceptor that should be invoked. A {@link Optional#isPresent() present}
     * continuation indicates the processing is expected to continue further, while
     * {@link Optional#absent() absence} of a continuation indicates that the
     * unidirectional request transformation passed its final stage.
     */
    @Override
    public Pair<Request, Optional<LinearAcceptor>> apply(Request data);
}
