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
package org.glassfish.jersey.process.internal;

import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.process.Inflector;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

/**
 * A stage-related collection of utility methods.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Stages {

    /**
     * Prevents instantiation.
     */
    private Stages() {
    }

    /**
     * Create a terminal hierarchical continuation with the supplied request on
     * the left side of the continuation.
     *
     * @param request transformed request to be returned as part of the terminal
     *     continuation.
     * @return terminal hierarchical continuation.
     */
    public static Pair<Request, Iterator<TreeAcceptor>> terminalTreeContinuation(final Request request) {
        return Tuples.<Request, Iterator<TreeAcceptor>>of(request, Iterators.<TreeAcceptor>emptyIterator());
    }

    /**
     * Create a hierarchical continuation with the supplied request on the left
     * side of the continuation and a singleton iterator containing the single
     * next stage on the right side of the continuation.
     *
     * @param request transformed request to be returned as part of the terminal
     *     continuation.
     * @param nextAcceptor next tree acceptor stage.
     * @return singleton hierarchical continuation.
     */
    public static Pair<Request, Iterator<TreeAcceptor>> singletonTreeContinuation(
            final Request request, TreeAcceptor nextAcceptor) {
        return Tuples.<Request, Iterator<TreeAcceptor>>of(request, Iterators.<TreeAcceptor>singletonIterator(nextAcceptor));
    }

    /**
     * Create a terminal linear continuation with the supplied request on
     * the left side of the continuation.
     *
     * @param request transformed request to be returned as part of the terminal
     *     continuation.
     * @return terminal linear continuation.
     */
    public static Pair<Request, Optional<LinearAcceptor>> terminalLinearContinuation(final Request request) {
        return Tuples.of(request, Optional.<LinearAcceptor>absent());
    }

    /**
     * Creates a leaf-node {@link TreeAcceptor} that implements {@link Inflecting}
     * interface and returns the provided {@link Inflector} instance
     * when the {@link Inflecting#inflector()} method is called.
     * {@link TreeAcceptor#apply(javax.ws.rs.core.Request)} method of the created
     * hierarchical acceptor returns the unchanged request and an empty
     * continuation iterator.
     *
     * @param inflector a request to response transformation to be wrapped in an
     *     {@code TreeAcceptor} instance.
     * @return an {@code TreeAcceptor} that wraps the supplied {@code Inflector}.
     */
    @SuppressWarnings("unchecked")
    public static TreeAcceptor asTreeAcceptor(final Inflector<Request, Response> inflector) {
        return new InflectingTreeAcceptor(inflector);
    }

    private static class InflectingTreeAcceptor implements TreeAcceptor, Inflecting<Request, Response> {

        private final Inflector<Request, Response> inflector;

        public InflectingTreeAcceptor(final Inflector<Request, Response> inflector) {
            this.inflector = inflector;
        }

        @Override
        public Inflector<Request, Response> inflector() {
            return inflector;
        }

        @Override
        public Pair<Request, Iterator<TreeAcceptor>> apply(Request request) {
            return terminalTreeContinuation(request);
        }
    }

    /**
     * Creates a terminal {@link LinearAcceptor} that implements {@link Inflecting}
     * interface and returns the provided {@link Inflector} instance
     * when the {@link Inflecting#inflector()} method is called.
     * {@link LinearAcceptor#apply(javax.ws.rs.core.Request)} method of the created
     * linear acceptor returns the unchanged supplied request and a continuation
     * referring to an {@link Optional#absent() absent} next linear acceptor.
     *
     * @param inflector a request to response transformation to be wrapped in an
     *     {@code TreeAcceptor} instance.
     * @return an {@code TreeAcceptor} that wraps the supplied {@code Inflector}.
     */
    @SuppressWarnings("unchecked")
    public static LinearAcceptor asLinearAcceptor(final Inflector<Request, Response> inflector) {
        return new InflectingLinearAcceptor(inflector);
    }

    private static class InflectingLinearAcceptor implements LinearAcceptor, Inflecting<Request, Response> {

        private final Inflector<Request, Response> inflector;

        public InflectingLinearAcceptor(Inflector<Request, Response> inflector) {
            this.inflector = inflector;
        }

        @Override
        public Inflector<Request, Response> inflector() {
            return inflector;
        }

        @Override
        public Pair<Request, Optional<LinearAcceptor>> apply(Request request) {
            return terminalLinearContinuation(request);
        }
    }

    /**
     * (Optionally) extracts an {@link Inflector inflector} from a  processing stage,
     * provided the stage implements {@link Inflecting} interface. Otherwise method
     * returns an {@link Optional#absent() absent} inflector.
     *
     * @param <DATA> data type transformable by the stage and returned inflector.
     * @param <RESULT> type of result produced by a successful inflector data transformation.
     * @param stage a stage to extract the inflector from.
     * @return extracted inflector, if present, {@link Optional#absent() absent} inflector
     *     otherwise.
     */
    @SuppressWarnings("unchecked")
    public static <DATA, RESULT> Optional<Inflector<DATA, RESULT>> extractInflector(Object stage) {
        if (stage instanceof Inflecting) {
            return Optional.fromNullable(((Inflecting<DATA, RESULT>) stage).inflector());
        }

        return Optional.absent();
    }

    /**
     * Start building a hierarchical accepting tree.
     *
     * @param transformation root transformation function.
     * @return hierarchical accepting tree builder.
     */
    public static TreeAcceptor.Builder acceptingTree(Function<Request, Request> transformation) {
        return new TreeAcceptorBuilder(transformation);
    }

    private static class TreeAcceptorBuilder implements TreeAcceptor.Builder {

        private final Function<Request, Request> transformation;
        private List<TreeAcceptor> children;

        public TreeAcceptorBuilder(Function<Request, Request> transformation) {
            this.transformation = transformation;
        }

        @Override
        public TreeAcceptor.Builder child(TreeAcceptor child) {
            if (children == null) {
                children = new LinkedList<TreeAcceptor>();
            }
            children.add(child);

            return this;
        }

        @Override
        public TreeAcceptor build() {
            return (children == null)
                    ? new LinkedTreeAcceptor(transformation) : new LinkedTreeAcceptor(transformation, children);
        }
    }

    private static class LinkedTreeAcceptor implements TreeAcceptor {

        private final Function<Request, Request> transformation;
        private final List<TreeAcceptor> children;

        public LinkedTreeAcceptor(Function<Request, Request> transformation, List<TreeAcceptor> children) {
            this.transformation = transformation;
            this.children = children;
        }

        public LinkedTreeAcceptor(Function<Request, Request> transformation) {
            this.transformation = transformation;
            this.children = Collections.emptyList();
        }

        @Override
        public Pair<Request, Iterator<TreeAcceptor>> apply(Request data) {
            return Tuples.of(transformation.apply(data), children.iterator());
        }
    }

    /**
     * Start building a linear accepting chain.
     *
     * @param transformation root transformation function.
     * @return linear accepting chain builder.
     */
    public static LinearAcceptor.Builder acceptingChain(Function<Request, Request> transformation) {
        return new LinearAcceptorChainBuilder(transformation);
    }

    /**
     * Start building a linear accepting chain.
     *
     * @param rootStage root {@link ChainableAcceptor chainable linear stage}.
     * @return linear accepting chain builder.
     */
    public static LinearAcceptor.Builder acceptingChain(ChainableAcceptor rootStage) {
        return new LinearAcceptorChainBuilder(rootStage);
    }

    private static class LinearAcceptorChainBuilder implements LinearAcceptor.Builder {

        private final Deque<Function<Request, Request>> transformations = new LinkedList<Function<Request, Request>>();
        private LinearAcceptor rootStage;
        private ChainableAcceptor lastStage;

        private LinearAcceptorChainBuilder(Function<Request, Request> transformation) {
            transformations.push(transformation);
        }

        private LinearAcceptorChainBuilder(ChainableAcceptor rootStage) {
            this.rootStage = rootStage;
            this.lastStage = rootStage;
        }

        @Override
        public LinearAcceptorChainBuilder to(Function<Request, Request> transformation) {
            transformations.push(transformation);
            return this;
        }

        @Override
        public LinearAcceptor.Builder to(final ChainableAcceptor stage) {
            addTailStage(stage);
            lastStage = stage;

            return this;
        }

        private void addTailStage(LinearAcceptor lastStage) {
            LinearAcceptor tail = lastStage;
            if (!transformations.isEmpty()) {
                tail = convertTransformations(lastStage);
            }
            if (rootStage != null) {
                this.lastStage.setDefaultNext(tail);
            } else {
                rootStage = tail;
            }
        }

        @Override
        public LinearAcceptor build(LinearAcceptor stage) {
            addTailStage(stage);

            return rootStage;
        }

        @Override
        public LinearAcceptor build() {
            return build(null);
        }

        private LinearAcceptor convertTransformations(LinearAcceptor successor) {
            LinearAcceptor stage;
            if (successor == null) {
                stage = new LinkedLinearAcceptor(transformations.poll());
            } else {
                stage = new LinkedLinearAcceptor(transformations.poll(), successor);
            }

            Function<Request, Request> t;
            while ((t = transformations.poll()) != null) {
                stage = new LinkedLinearAcceptor(t, stage);
            }

            return stage;
        }
    }

    private static class LinkedLinearAcceptor implements LinearAcceptor {

        private final Optional<LinearAcceptor> nextStage;
        private final Function<Request, Request> transformation;

        /**
         * Create a new linear stage that will return the supplied stage in the
         * continuation.
         *
         * @param transformation Request transformation function to be applied in the stage.
         * @param nextStage next stage returned in the continuation.
         */
        public LinkedLinearAcceptor(Function<Request, Request> transformation, LinearAcceptor nextStage) {
            this.nextStage = Optional.of(nextStage);
            this.transformation = transformation;
        }

        /**
         * Create a new terminal linear stage with an {@link Optional#absent() absent}
         * next stage.
         *
         * @param transformation Request transformation function to be applied in the stage.
         */
        public LinkedLinearAcceptor(Function<Request, Request> transformation) {
            this.nextStage = Optional.absent();
            this.transformation = transformation;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This implementation invokes the underlying transformation function on the
         * supplied Request and returns the transformed result on the left side of the
         * returned continuation.
         * The {@link #LinkedLinearAcceptor(com.google.common.base.Function, LinearAcceptor)
         * registered next stage} is returned on the right side of the returned continuation.
         */
        @Override
        public Pair<Request, Optional<LinearAcceptor>> apply(Request request) {
            return Tuples.of(transformation.apply(request), nextStage);
        }
    }

    /**
     * Start building a linear accepting chain.
     *
     * @param transformation root transformation function.
     * @return linear accepting chain builder.
     */
    public static Responder.Builder respondingChain(Function<Response, Response> transformation) {
        return new ResponderChainBuilder(transformation);
    }

    private static class ResponderChainBuilder implements Responder.Builder {

        private final Deque<Function<Response, Response>> transformations = new LinkedList<Function<Response, Response>>();

        private ResponderChainBuilder(Function<Response, Response> transformation) {
            transformations.push(transformation);
        }

        @Override
        public ResponderChainBuilder to(Function<Response, Response> transformation) {
            transformations.push(transformation);
            return this;
        }

        @Override
        public Responder build(Responder stage) {
            Function<Response, Response> t;
            while ((t = transformations.poll()) != null) {
                stage = new LinkedResponder(t, stage);
            }

            return stage;
        }

        @Override
        public Responder build() {
            Responder stage = new LinkedResponder(transformations.poll());

            Function<Response, Response> t;
            while ((t = transformations.poll()) != null) {
                stage = new LinkedResponder(t, stage);
            }

            return stage;
        }
    }

    private static class LinkedResponder implements Responder {

        private final Optional<Responder> nextStage;
        private final Function<Response, Response> transformation;

        /**
         * Create a new linear stage that will return the supplied stage in the
         * continuation.
         *
         * @param transformation Response transformation function to be applied in the stage.
         * @param nextStage next stage returned in the continuation.
         */
        public LinkedResponder(Function<Response, Response> transformation, Responder nextStage) {
            this.nextStage = Optional.of(nextStage);
            this.transformation = transformation;
        }

        /**
         * Create a new terminal linear stage with an {@link Optional#absent() absent}
         * next stage.
         *
         * @param transformation Response transformation function to be applied in the stage.
         */
        public LinkedResponder(Function<Response, Response> transformation) {
            this.nextStage = Optional.absent();
            this.transformation = transformation;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This implementation invokes the underlying transformation function on the
         * supplied Response and returns the transformed result on the left side of the
         * returned continuation.
         * The {@link #LinkedResponder(com.google.common.base.Function, Responder)
         * registered next stage} is returned on the right side of the returned continuation.
         */
        @Override
        public Pair<Response, Optional<Responder>> apply(Response Response) {
            return Tuples.of(transformation.apply(Response), nextStage);
        }
    }
}
