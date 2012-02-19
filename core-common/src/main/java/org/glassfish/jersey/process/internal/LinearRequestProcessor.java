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
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * A composite {@link LinearAcceptor linear acceptor} request processor.
 * <p/>
 * When {@link #apply(java.lang.Object) invoked}, the supplied data are first
 * transformed by the nested linear stage chain. Once a terminal stage is reached
 * a continuation is returned with the transformed request on the {@link Pair#left() left side}
 * and the inflector (optionally) {@link Inflecting provided by the terminal stage}
 * on the {@link Pair#right() right side}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class LinearRequestProcessor implements RequestProcessor {

    private final LinearAcceptor rootAcceptor;
    private final Factory<StagingContext<Request>> contextProvider;

    /**
     * Construct a {@link LinearAcceptor linear acceptor} request processor.
     *
     * @param rootAcceptor head of the nested linear stage chain to be run.
     * @param contextProvider provider of the staging context to be invoked
     *     before and after each stage is applied.
     */
    public LinearRequestProcessor(
            @Inject @Stage.Root LinearAcceptor rootAcceptor,
            @Inject Factory<StagingContext<Request>> contextProvider) {
        this.rootAcceptor = rootAcceptor;
        this.contextProvider = contextProvider;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation transforms the request using a nested linear stage chain.
     * Once a terminal stage is reached a continuation is returned with the transformed
     * request on the {@link Pair#left() left side} and the inflector (optionally)
     * {@link Inflecting provided by the terminal stage} on the {@link Pair#right() right side}.
     */
    @Override
    public Pair<Request, Optional<Inflector<Request, Response>>> apply(Request request) {
        final StagingContext<Request> context = contextProvider.get();

        LinearAcceptor stage = rootAcceptor;
        Pair<Request, Optional<LinearAcceptor>> continuation = Tuples.of(request, Optional.fromNullable(stage));
        while (continuation.right().isPresent()) {
            stage = continuation.right().get();
            context.beforeStage(stage, continuation.left());
            continuation = stage.apply(continuation.left());
            context.afterStage(stage, continuation.left());
        }

        Request processed = continuation.left();

        final Optional<Stage<Request, ?>> lastStage = contextProvider.get().lastStage();
        Preconditions.checkState(lastStage.isPresent(),
                "No stage has been invoked as part of the processing.");

        return Tuples.of(processed, Stages.<Request, Response>extractInflector(lastStage.get()));
    }
}
