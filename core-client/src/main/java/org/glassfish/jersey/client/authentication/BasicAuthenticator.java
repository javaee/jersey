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

package org.glassfish.jersey.client.authentication;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.Base64;

/**
 * Implementation of Basic Http Authentication method (RFC 2617).
 *
 * @author Miroslav Fuksa
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Craig McClanahan
 */
final class BasicAuthenticator {

    private final HttpAuthenticationFilter.Credentials defaultCredentials;

    /**
     * Creates a new instance of basic authenticator.
     *
     * @param defaultCredentials Credentials. Can be {@code null} if no default credentials should be
     *                           used.
     */
    BasicAuthenticator(HttpAuthenticationFilter.Credentials defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }

    private String calculateAuthentication(HttpAuthenticationFilter.Credentials credentials) {
        String username = credentials.getUsername();
        byte[] password = credentials.getPassword();
        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = new byte[0];
        }

        final byte[] prefix = (username + ":").getBytes(HttpAuthenticationFilter.CHARACTER_SET);
        final byte[] usernamePassword = new byte[prefix.length + password.length];

        System.arraycopy(prefix, 0, usernamePassword, 0, prefix.length);
        System.arraycopy(password, 0, usernamePassword, prefix.length, password.length);

        return "Basic " + Base64.encodeAsString(usernamePassword);
    }

    /**
     * Adds authentication information to the request.
     *
     * @param request Request context.
     * @throws RequestAuthenticationException in case that basic credentials missing or are in invalid format
     */
    public void filterRequest(ClientRequestContext request) throws RequestAuthenticationException {
        HttpAuthenticationFilter.Credentials credentials = HttpAuthenticationFilter.getCredentials(request,
                defaultCredentials, HttpAuthenticationFilter.Type.BASIC);
        if (credentials == null) {
            throw new RequestAuthenticationException(LocalizationMessages.AUTHENTICATION_CREDENTIALS_MISSING_BASIC());
        }
        request.getHeaders().add(HttpHeaders.AUTHORIZATION, calculateAuthentication(credentials));
    }

    /**
     * Checks the response and if basic authentication is required then performs a new request
     * with basic authentication.
     *
     * @param request  Request context.
     * @param response Response context (will be updated with newest response data if the request was repeated).
     * @return {@code true} if response does not require authentication or if authentication is required,
     * new request was done with digest authentication information and authentication was successful.
     * @throws ResponseAuthenticationException in case that basic credentials missing or are in invalid format
     */
    public boolean filterResponseAndAuthenticate(ClientRequestContext request, ClientResponseContext response) {
        final String authenticate = response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
        if (authenticate != null && authenticate.trim().toUpperCase().startsWith("BASIC")) {
            HttpAuthenticationFilter.Credentials credentials = HttpAuthenticationFilter
                    .getCredentials(request, defaultCredentials, HttpAuthenticationFilter.Type.BASIC);

            if (credentials == null) {
                throw new ResponseAuthenticationException(null, LocalizationMessages.AUTHENTICATION_CREDENTIALS_MISSING_BASIC());
            }

            return HttpAuthenticationFilter.repeatRequest(request, response, calculateAuthentication(credentials));
        }
        return false;
    }
}
