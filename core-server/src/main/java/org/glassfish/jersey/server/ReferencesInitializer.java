/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;

/**
 * Request/response injection references initialization stage.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ReferencesInitializer implements Function<JerseyContainerRequestContext, JerseyContainerRequestContext> {

    @Inject
    private Services services;
    @Inject
    private Factory<Ref<Request>> requestReference;
    @Inject
    private Factory<Ref<JerseyContainerRequestContext>> requestContextReference;
    @Inject
    private Factory<Ref<HttpHeaders>> httpHeadersReference;
    @Inject
    private Factory<Ref<SecurityContext>> securityContextReference;

    /**
     * Initialize the request references using the incoming request and register
     * a response reference initialization stage in the
     * {@link org.glassfish.jersey.process.internal.ResponseProcessor.RespondingContext
     * responding context}.
     *
     * @param requestContext incoming request context.
     * @return same (unmodified) request context.
     */
    @Override
    public JerseyContainerRequestContext apply(final JerseyContainerRequestContext requestContext) {
        requestReference.get().set(requestContext.getRequest());
        requestContextReference.get().set(requestContext);
        httpHeadersReference.get().set(Requests.httpHeaders(requestContext));
        securityContextReference.get().set(requestContext.getSecurityContext());

        final RequestScopedInitializer requestScopedInitializer = requestContext.getRequestScopedInitializer();
        if (requestScopedInitializer != null) {
            requestScopedInitializer.initialize(services);
        }

        return requestContext;
    }
}
