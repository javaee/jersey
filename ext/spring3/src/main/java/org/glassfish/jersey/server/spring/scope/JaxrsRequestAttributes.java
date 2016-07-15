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

import javax.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.server.spring.LocalizationMessages;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.AbstractRequestAttributes;

/**
 * JAX-RS based Spring RequestAttributes implementation.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class JaxrsRequestAttributes extends AbstractRequestAttributes {

    private final ContainerRequestContext requestContext;

    /**
     * Create a new instance.
     *
     * @param requestContext JAX-RS container request context
     */
    public JaxrsRequestAttributes(ContainerRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    protected void updateAccessedSessionAttributes() {
        // sessions not supported
    }

    @Override
    public Object getAttribute(String name, int scope) {
        return requestContext.getProperty(name);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        requestContext.setProperty(name, value);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        requestContext.removeProperty(name);
    }

    @Override
    public String[] getAttributeNames(int scope) {
        if (!isRequestActive()) {
            throw new IllegalStateException(LocalizationMessages.NOT_IN_REQUEST_SCOPE());
        }
        return StringUtils.toStringArray(requestContext.getPropertyNames());
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
        registerRequestDestructionCallback(name, callback);
    }

    @Override
    public Object resolveReference(String key) {
        if (REFERENCE_REQUEST.equals(key)) {
            return requestContext;
        }
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public Object getSessionMutex() {
        return null;
    }
}
