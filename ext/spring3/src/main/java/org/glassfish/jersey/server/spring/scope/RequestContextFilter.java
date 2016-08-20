/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spring.scope;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.glassfish.hk2.api.ServiceLocator;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.AbstractRequestAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Spring filter to provide a bridge between JAX-RS and Spring request attributes.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Provider
@PreMatching
public final class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String REQUEST_ATTRIBUTES_PROPERTY = RequestContextFilter.class.getName() + ".REQUEST_ATTRIBUTES";

    private final SpringAttributeController attributeController;

    private static final SpringAttributeController EMPTY_ATTRIBUTE_CONTROLLER = new SpringAttributeController() {
        @Override
        public void setAttributes(final ContainerRequestContext requestContext) {
        }

        @Override
        public void resetAttributes(final ContainerRequestContext requestContext) {
        }
    };

    private interface SpringAttributeController {

        void setAttributes(final ContainerRequestContext requestContext);

        void resetAttributes(final ContainerRequestContext requestContext);
    }

    /**
     * Create a new request context filter instance.
     *
     * @param locator HK2 service locator.
     */
    @Inject
    public RequestContextFilter(final ServiceLocator locator) {
        final ApplicationContext appCtx = locator.getService(ApplicationContext.class);
        final boolean isWebApp = appCtx instanceof WebApplicationContext;

        attributeController = appCtx != null ? new SpringAttributeController() {

            @Override
            public void setAttributes(final ContainerRequestContext requestContext) {
                final RequestAttributes attributes;
                if (isWebApp) {
                    final HttpServletRequest httpRequest = locator.getService(HttpServletRequest.class);
                    attributes = new JaxrsServletRequestAttributes(httpRequest, requestContext);
                } else {
                    attributes = new JaxrsRequestAttributes(requestContext);
                }
                requestContext.setProperty(REQUEST_ATTRIBUTES_PROPERTY, attributes);
                RequestContextHolder.setRequestAttributes(attributes);
            }

            @Override
            public void resetAttributes(final ContainerRequestContext requestContext) {
                final AbstractRequestAttributes attributes =
                        (AbstractRequestAttributes) requestContext.getProperty(REQUEST_ATTRIBUTES_PROPERTY);
                RequestContextHolder.resetRequestAttributes();
                attributes.requestCompleted();
            }
        } : EMPTY_ATTRIBUTE_CONTROLLER;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        attributeController.setAttributes(requestContext);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        attributeController.resetAttributes(requestContext);
    }
}
