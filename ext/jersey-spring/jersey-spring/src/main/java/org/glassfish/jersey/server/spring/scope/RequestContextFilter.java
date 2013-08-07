/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.hk2.api.ServiceLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

import javax.inject.Inject;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * JAX-RS filter that implements Spring request scope for non-servlet JAX-RS containers.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Provider
@PreMatching
public class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOGGER = Logger.getLogger(RequestContextFilter.class.getName());
    private static final String REQUEST_ATTRIBUTES_PROPERTY =
            RequestContextFilter.class.getName() + ".REQUEST_ATTRIBUTES";
    private static boolean enabled;

    @Inject
    public RequestContextFilter(ServiceLocator locator) {
        // TODO: implement a better way to dynamically configure RequestContextFilter
        ApplicationContext ctx = locator.getService(ApplicationContext.class);
        if(ctx instanceof ClassPathXmlApplicationContext) {
            enabled = true;
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if(!enabled) {
            return;
        }
        JaxrsRequestAttributes attributes = new JaxrsRequestAttributes(requestContext);
        requestContext.setProperty(REQUEST_ATTRIBUTES_PROPERTY, attributes);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if(!enabled) {
            return;
        }
        JaxrsRequestAttributes attributes = (JaxrsRequestAttributes)requestContext.getProperty(REQUEST_ATTRIBUTES_PROPERTY);
        RequestContextHolder.resetRequestAttributes();
        attributes.requestCompleted();
    }
}
