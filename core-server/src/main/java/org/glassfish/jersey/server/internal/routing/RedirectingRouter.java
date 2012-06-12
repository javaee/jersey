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
package org.glassfish.jersey.server.internal.routing;

import java.net.URI;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.process.Inflector;

import org.glassfish.hk2.Factory;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.collect.Lists;

/**
 * TODO javadoc.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class RedirectingRouter implements Router {

    /**
     * "Assisted injection" factory interface for {@link RedirectingRouter}.
     *
     * See also <a href="http://code.google.com/p/google-guice/wiki/AssistedInject">
     * assisted injection in Guice</a>.
     */
    public static class Builder {

        private final Factory<RoutingContext> contextProvider;
        private final Factory<UriInfo> uriInfoProvider;

        public Builder(@Inject Factory<RoutingContext> contextProvider, @Inject Factory<UriInfo> uriInfoProvider) {
            this.contextProvider = contextProvider;
            this.uriInfoProvider = uriInfoProvider;
        }

        public RedirectingRouter build(boolean redirect, boolean patternEndsInSlash) {
            return new RedirectingRouter(contextProvider, uriInfoProvider, redirect, patternEndsInSlash);
        }
    }

    private final Factory<RoutingContext> contextProvider;
    private final Factory<UriInfo> uriInfoProvider;
    // TODO implement config injection
    private final boolean redirect;
    private final boolean patternEndsInSlash;

    private RedirectingRouter(
            final Factory<RoutingContext> contextProvider,
            final Factory<UriInfo> uriInfoProvider,
            final boolean redirect,
            final boolean patternEndsInSlash) {

        this.contextProvider = contextProvider;
        this.uriInfoProvider = uriInfoProvider;
        this.redirect = redirect;
        this.patternEndsInSlash = patternEndsInSlash;
    }

    @Override
    public Continuation apply(Request request) {
        String rhPath = getLastMatch();

        final int rhPathLength = rhPath.length();
        if (rhPathLength == 0) {
            // Redirect to path ending with a '/' if pattern
            // ends in '/' and redirect is true
            if (patternEndsInSlash && redirect) {
                return redirect(request); // was return true
            }
            // TODO: deal with or just remove: context.pushRightHandPathLength(0);
        } else if (rhPath.length() == 1) {
            // Path is '/';
            // no match if pattern does not end in a '/' and redirect is true
            if (!patternEndsInSlash && redirect) {
                return Continuation.of(request);
            }

            // Consume the '/'
            // TODO: deal with or just remove: lastMatch = "";
            // TODO: deal with or just remove: context.pushRightHandPathLength(0);
        } else {
            if (patternEndsInSlash) {
                // TODO: deal with or just remove: context.pushRightHandPathLength(lastMatch.length() - 1);
            } else {
                // TODO: deal with or just remove: context.pushRightHandPathLength(lastMatch.length());
            }
        }

        // Accept using the right hand path
        return Continuation.of(request);
    }

    /**
     * Get the right hand path from the match result. The right hand path is
     * the last group (if present).
     *
     * @return the right hand path, or the empty string if there is no last
     *         group.
     */
    private String getLastMatch() {
        String match = contextProvider.get().getFinalMatchingGroup();
        return (match == null) ? "" : match;
    }

    /**
     * Redirect to a URI that ends in a slash.
     *
     * TODO use the complete URI.
     */
    private Continuation redirect(Request request) {
        return Continuation.of(request, Lists.newArrayList(Routers.asTreeAcceptor(
                new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request request) {
                        final URI redirectPath = uriInfoProvider.get().getRequestUriBuilder().path("/").build();
                        return Response.temporaryRedirect(redirectPath).build();
                    }
                })));
    }
}
