/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.process;

import java.security.Principal;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import javax.inject.Inject;

import org.glassfish.jersey.server.internal.LocalizationMessages;

/**
 * Proxiable wrapper for request scoped {@link SecurityContext} instance.
 *
 * <p>
 * This wrapper must be used and cannot be replaced by {@link org.glassfish.jersey.internal.inject.ReferencingFactory}.
 * The reason is that {@link SecurityContext security context} can be set
 * many times during the request processing. However, the HK2 proxy caches
 * the first value that is injected. So, if for example any filter injects
 * security context, then this security context will be cached and it will
 * never be replaced for the same request. On the other hand, HK2 should
 * probably cache the first value returned in the request scope to prevent
 * that two subsequent calls done on the proxy will be forwarded to different
 * object if the the object changes in the meantime.
 * <p/>
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
class SecurityContextInjectee implements SecurityContext {

    private final ContainerRequestContext requestContext;

    /**
     * Injection constructor.
     *
     * @param requestContext {@code SecurityContext} source.
     */
    @Inject
    public SecurityContextInjectee(ContainerRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    public Principal getUserPrincipal() {
        checkState();
        return requestContext.getSecurityContext().getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        checkState();
        return requestContext.getSecurityContext().isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        checkState();
        return requestContext.getSecurityContext().isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        checkState();
        return requestContext.getSecurityContext().getAuthenticationScheme();
    }

    @Override
    public int hashCode() {
        checkState();
        return 7 * requestContext.getSecurityContext().hashCode();
    }

    @Override
    public boolean equals(Object that) {
        checkState();

        return that instanceof SecurityContext && that.equals(requestContext.getSecurityContext());
    }

    private void checkState() {
        if (requestContext == null) {
            throw new IllegalStateException(LocalizationMessages.SECURITY_CONTEXT_WAS_NOT_SET());
        }
    }
}
