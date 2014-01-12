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

package org.glassfish.jersey.server.oauth1;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;

/**
 * Security request that gets injected into the context by the OAuth filter
 * based on the access token attached to the request.
 *
 * @author Martin Matula
 */
class OAuth1SecurityContext implements SecurityContext {
    private final OAuth1Consumer consumer;
    private final OAuth1Token token;
    private final boolean isSecure;

    /**
     * Create a new OAuth security context from {@link OAuth1Consumer consumer}.
     *
     * @param consumer OAuth consumer for which the context will be created.
     * @param isSecure {@code true} if the request is secured over SSL (HTTPS).
     */
    public OAuth1SecurityContext(OAuth1Consumer consumer, boolean isSecure) {
        this.consumer = consumer;
        this.token = null;
        this.isSecure = isSecure;
    }

    /**
     * Create a new OAuth security context from {@link OAuth1Token Access Token}.
     * @param token Access Token.
     * @param isSecure {@code true} if the request is secured over SSL (HTTPS).
     */
    public OAuth1SecurityContext(OAuth1Token token, boolean isSecure) {
        this.consumer = null;
        this.token = token;
        this.isSecure = isSecure;
    }

    @Override
    public Principal getUserPrincipal() {
        return consumer == null ? token.getPrincipal() : consumer.getPrincipal();
    }

    @Override
    public boolean isUserInRole(String string) {
        return consumer == null ? token.isInRole(string) : consumer.isInRole(string);
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public String getAuthenticationScheme() {
        return OAuth1Parameters.SCHEME;
    }

}
