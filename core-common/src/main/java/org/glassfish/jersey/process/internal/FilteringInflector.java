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
import com.google.common.base.Optional;
import org.glassfish.hk2.Factory;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.jvnet.hk2.annotations.Inject;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Santiago Pericas-Geertsen (santiago.pericasgeertsen at oracle.com)
 */
public class FilteringInflector implements Inflector<Request, Response> {

    // TODO This builder is not very useful!
    public static class Builder {

        @Inject
        private RequestFilterAcceptor requestFilterAcceptor;

        @Inject
        private ResponseFilterResponder responseFilterResponder;

        @Inject
        private Factory<JerseyFilterContext> filterContextFactory;

        @Inject
        private Factory<ResponseProcessor.RespondingContext> respondingContextFactory;

        public Builder() {
            // Injection constructor
        }

        public FilteringInflector build(Inflector<Request, Response> wrapped) {
            return new FilteringInflector(wrapped, requestFilterAcceptor, responseFilterResponder,
                    filterContextFactory, respondingContextFactory);
        }
    }

    private final Inflector<Request, Response> wrapped;
    private final RequestFilterAcceptor requestFilterAcceptor;
    private final ResponseFilterResponder responseFilterResponder;
    private final Factory<JerseyFilterContext> filterContextFactory;
    private final Factory<ResponseProcessor.RespondingContext> respondingContextFactory;

    private FilteringInflector(Inflector<Request, Response> wrapped,
                               RequestFilterAcceptor requestFilterAcceptor,
                               ResponseFilterResponder responseFilterResponder,
                               Factory<JerseyFilterContext> filterContextFactory,
                               Factory<ResponseProcessor.RespondingContext> respondingContextFactory) {
        this.wrapped = wrapped;
        this.requestFilterAcceptor = requestFilterAcceptor;
        this.responseFilterResponder = responseFilterResponder;
        this.filterContextFactory = filterContextFactory;
        this.respondingContextFactory = respondingContextFactory;
    }

    @Override
    public Response apply(Request request) {
        JerseyFilterContext filterContext = filterContextFactory.get();

        respondingContextFactory.get().push(responseFilterResponder);

        filterContext.setResponse(null);
        Pair<Request, Optional<LinearAcceptor>> pair = requestFilterAcceptor.apply(request);
        final Response response = filterContext.getResponse();

        return (response != null) ? response : wrapped.apply(pair.left());
    }
}
