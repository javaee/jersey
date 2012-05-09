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
package org.glassfish.jersey.server.internal.routing;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.uri.ExtendedUriInfo;
import org.glassfish.jersey.uri.PathPattern;
import org.glassfish.jersey.uri.UriTemplate;

import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.TypeLiteral;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;

/**
 * Provides routing configuration functionality.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RouterModule extends AbstractModule {

    /**
     * Transformation function that converts methodAcceptorPair provider into provider value
     */
    static final Function<Factory<TreeAcceptor>, TreeAcceptor> FACTORY_TO_ACCEPTOR_TRANSFORMATION =
            new Function<Factory<TreeAcceptor>, TreeAcceptor>() {

                @Override
                public TreeAcceptor apply(Factory<TreeAcceptor> f) {
                    return f.get();
                }
            };

    public interface RoutingContext {

        /**
         * Push the result of the successful request URI routing pattern match.
         *
         * @param matchResult successful request URI routing pattern
         *     {@link MatchResult match result}.
         */
        public void pushMatchResult(MatchResult matchResult);

        /**
         * Push the resource that matched the request URI.
         *
         * @param resource instance of the resource that matched the request URI.
         */
        public void pushMatchedResource(Object resource);

        /**
         * Peek the last resource object that successfully matched the request URI.
         *
         * @return last resource matched as previously set by {@link #pushMatchedResource}
         */
        public Object peekMatchedResource();

        /**
         * Peek at the last successful request URI routing pattern
         * {@link MatchResult match result}.
         *
         * @return last successful request URI routing pattern match result.
         */
        public MatchResult peekMatchResult();

        /**
         * Push matched request URI routing pattern {@link UriTemplate URI template}.
         *
         * @param template URI template of the matched request URI routing pattern.
         */
        public void pushTemplate(UriTemplate template);

        /**
         * Get the final matching group of the last successful request URI routing
         * pattern {@link MatchResult match result}. Also known as right-hand path.
         * <p>
         * May be empty but is never {@code null}.
         * </p>
         *
         * @return final matching group of the last successful request URI routing
         *     pattern match result.
         */
        public String getFinalMatchingGroup();

        /**
         * Get a read-only list of {@link MatchResult match results} for matched
         * request URI routing patterns. Entries are ordered in reverse request URI
         * matching order, with the root request URI routing pattern match result
         * last.
         *
         * @return a read-only reverse list of request URI routing pattern match
         *     results.
         */
        public List<MatchResult> getMatchedResults();

        /**
         * Add currently matched left-hand side part of request path to the list of
         * matched paths returned by {@link UriInfo#getMatchedURIs()}.
         * <p/>
         * Left-hand side request path is the request path excluding the suffix
         * part of the path matched by the {@link #getFinalMatchingGroup() final
         * matching group} of the last successful request URI routing pattern.
         */
        public void pushLeftHandPath();

        /**
         * Get the effective content type of the response calculated from the
         * request {@code Accept} header values and {@link Consumes @Consumes}
         * value of the matched resource method. May return {@code null} if no
         * method was yet matched.
         *
         * @return effective accepted media type. May return {@code null} if no
         * resource method was yet matched.
         */
        @Nullable
        public MediaType getEffectiveAcceptableType();

        /**
         * Set the effective content type of the response calculated from the
         * request {@code Accept} header values and {@link Consumes @Consumes}
         * value of the matched resource method. The input value must not be
         * {@code null} otherwise a {@link NullPointerException} is thrown.
         *
         * @param type new effective accepted media type.
         * @throws NullPointerException in case the input parameter is {@code null}.
         */
        public void setEffectiveAcceptableType(@Nonnull MediaType type) throws NullPointerException;

        /**
         * Set the response type of the method ultimately used to process the request.
         *
         * @param responseType
         * @throws NullPointerException in case the input parameter is {@code null}.
         */
        public void setResponseMethodType(Type responseType) throws NullPointerException;

        /**
         * Get the response type of the method ultimately used to process the request.
         *
         * @return processing method response type.
         */
        public Type getResponseMethodType();

        /**
         * Set the annotations on the method ultimately used to process the request.
         *
         * @param annotations
         * @throws NullPointerException in case the input parameter is {@code null}.
         */
        public void setResponseMethodAnnotations(Annotation[] annotations) throws NullPointerException;

        /**
         * Get the annotations on the method ultimately used to process the request.
         *
         * @return processing method annotations.
         */
        public Annotation[] getResponseMethodAnnotations();
    }

    public interface RootRouteBuilder<T> extends RouteBuilder<T> {

        TreeAcceptor root(TreeAcceptor routingRoot);
    }

    public interface RouteBuilder<T> {

        RouteToBuilder<T> route(String pattern);

        RouteToBuilder<T> route(T pattern);
    }

    public interface RouteToBuilder<T> {

        RouteToPathBuilder<T> to(TreeAcceptor.Builder ab);

        RouteToPathBuilder<T> to(TreeAcceptor a);

//        RouteToPathBuilder<T> to(Key<? extends Acceptor> ka);
        RouteToPathBuilder<T> to(Class<? extends TreeAcceptor> ca);

        RouteToPathBuilder<T> to(Factory<? extends TreeAcceptor> pa);
    }

    public interface RouteToPathBuilder<T> extends RouteBuilder<T>, RouteToBuilder<T>, TreeAcceptor.Builder {
    }

    @Override
    public void configure() {
        bind(new TypeLiteral<RootRouteBuilder<Pattern>>() {
        }).to(PatternRouteBuilder.class);
        bind(new TypeLiteral<RootRouteBuilder<PathPattern>>() {
        }).to(PathPatternRouteBuilder.class);

        /**
         * Note: Bellow bindings work because UriRoutingontextFactory is bound via class,
         * which causes HK2 to instantiate and inject it properly whenever it needs to be
         * used (typically once per run-time request scope instance).
         */
        bind(UriRoutingContext.class).to(UriRoutingContext.class).in(RequestScope.class);
        bind(RoutingContext.class).toFactory(UriRoutingContextFactory.class).in(RequestScope.class);
        bind(ExtendedUriInfo.class).toFactory(UriRoutingContextFactory.class).in(RequestScope.class);
        bind(UriInfo.class).toFactory(UriRoutingContextFactory.class).in(RequestScope.class);

//        bind(UriRoutingContext.class).to(UriRoutingContext.class).in(RequestScope.class);
//        bind(RoutingContext.class).toFactory(new TypeLiteral<TF<UriRoutingContext>>(){});
//        bind(ExtendedUriInfo.class).toFactory(new TypeLiteral<TF<UriRoutingContext>>(){});
//        bind(UriInfo.class).toFactory(new TypeLiteral<TF<UriRoutingContext>>(){});

        // "Assisted" bindings
        bind().to(MatchResultInitializerAcceptor.Builder.class);
        bind().to(PatternRouteAcceptor.Builder.class);
        bind().to(PathPatternRouteAcceptor.Builder.class);
        bind().to(RedirectingAcceptor.Builder.class);
    }

    private static class UriRoutingContextFactory implements Factory<UriRoutingContext> {

        @Inject
        private UriRoutingContext ctx;

        @Override
        public UriRoutingContext get() throws ComponentException {
            return ctx;
        }
    }
//    private static class TF<T> implements Factory<T> {
//        @Inject private T t;
//
//        @Override
//        public T get() throws ComponentException {
//            return t;
//        }
//    }
}
