/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.mvc.jsp;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;

/**
 * {@link RequestDispatcher Request dispatcher wrapper} for setting attributes (e.g. {@code it}).
 *
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
final class RequestDispatcherWrapper implements RequestDispatcher {

    static final String BASE_PATH_ATTRIBUTE_NAME = "_basePath";
    static final String OLD_MODEL_ATTRIBUTE_NAME = "it";
    static final String MODEL_ATTRIBUTE_NAME = "model";
    static final String RESOLVING_CLASS_ATTRIBUTE_NAME = "resolvingClass";
    static final String REQUEST_ATTRIBUTE_NAME = "_request";
    static final String RESPONSE_ATTRIBUTE_NAME = "_response";

    private final RequestDispatcher dispatcher;

    private final String basePath;

    private final ResolvedViewable viewable;

    /**
     * Creates new {@code RequestDispatcherWrapper} responsible for setting request attributes and forwarding the processing to
     * the given dispatcher.
     *
     * @param dispatcher dispatcher processing the request after all the request attributes were set.
     * @param basePath base path of all JSP set to {@value #BASE_PATH_ATTRIBUTE_NAME} request attribute.
     * @param viewable viewable to obtain model and resolving class from.
     */
    public RequestDispatcherWrapper(
            final RequestDispatcher dispatcher, final String basePath, final ResolvedViewable viewable) {
        this.dispatcher = dispatcher;
        this.basePath = basePath;
        this.viewable = viewable;
    }

    @Override
    public void forward(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
        final Object oldIt = request.getAttribute(MODEL_ATTRIBUTE_NAME);
        final Object oldResolvingClass = request.getAttribute(RESOLVING_CLASS_ATTRIBUTE_NAME);

        request.setAttribute(RESOLVING_CLASS_ATTRIBUTE_NAME, viewable.getResolvingClass());

        request.setAttribute(OLD_MODEL_ATTRIBUTE_NAME, viewable.getModel());
        request.setAttribute(MODEL_ATTRIBUTE_NAME, viewable.getModel());

        request.setAttribute(BASE_PATH_ATTRIBUTE_NAME, basePath);
        request.setAttribute(REQUEST_ATTRIBUTE_NAME, request);
        request.setAttribute(RESPONSE_ATTRIBUTE_NAME, response);

        dispatcher.forward(request, response);

        request.setAttribute(RESOLVING_CLASS_ATTRIBUTE_NAME, oldResolvingClass);
        request.setAttribute(MODEL_ATTRIBUTE_NAME, oldIt);
    }

    @Override
    public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

}
