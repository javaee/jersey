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

import org.glassfish.jersey.process.Inflector;
import java.util.Iterator;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Optional;

/**
 * A composite {@link TreeStage hierarchical} request processor.
 * <p/>
 * When {@link #apply(java.lang.Object) invoked}, the supplied request is continuously
 * transformed by the nested {@link TreeStage stage hierarchy} using a depth-first
 * transformation strategy until a request-to-response inflector is
 * {@link Inflecting found on a leaf stage node}, in which case the hierarchical
 * stage transformation is terminated and a continuation with the transformed
 * request on the {@link Pair#left() left side} and the inflector on the
 * {@link Pair#right() right side} is returned.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HierarchicalRequestProcessor implements RequestProcessor {

    private final TreeAcceptor rootStage;
    private final Factory<StagingContext<Request>> contextProvider;

    /**
     * Construct a {@link TreeStage hierarchical} request processor.
     *
     * @param rootStage head of the nested stage hierarchy to be applied.
     * @param contextProvider staging context to be invoked before and after each
     *     stage is applied.
     */
    public HierarchicalRequestProcessor(
            @Inject @Stage.Root TreeAcceptor rootStage,
            @Inject Factory<StagingContext<Request>> contextProvider) {
        this.rootStage = rootStage;
        this.contextProvider = contextProvider;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation applies the nested {@link TreeStage stage hierarchy}
     * using a depth-first transformation strategy until a request-to-response
     * inflector is {@link Inflecting found on a leaf stage node}, in which case
     * the hierarchical stage transformation is terminated and a continuation with
     * the transformed request on the {@link Pair#left() left side} and the inflector
     * on the {@link Pair#right() right side} is returned.
     */
    @Override
    public Pair<Request, Optional<Inflector<Request, Response>>> apply(Request request) {
        return _apply(request, rootStage, contextProvider.get());
    }

    @SuppressWarnings("unchecked")
    private Pair<Request, Optional<Inflector<Request, Response>>> _apply(final Request request, final TreeAcceptor acceptor, final StagingContext<Request> context) {
        context.beforeStage(acceptor, request);
        final Pair<Request, Iterator<TreeAcceptor>> continuation = acceptor.apply(request);

        context.afterStage(acceptor, continuation.left());

        final Iterator<TreeAcceptor> children = continuation.right();
        while (children.hasNext()) {
            final TreeAcceptor child = children.next();
            Pair<Request, Optional<Inflector<Request, Response>>> result = _apply(continuation.left(), child, context);

            if (result.right().isPresent()) {
                // we're done
                return result;
            } // else continue
        }

        if (acceptor instanceof Inflecting) {
            // inflector at terminal stage found
            return Tuples.of(continuation.left(), Optional.of(((Inflecting<Request, Response>) acceptor).inflector()));
        }

        // inflector at terminal stage not found
        return Tuples.of(continuation.left(), Optional.<Inflector<Request, Response>>absent());
    }
}
