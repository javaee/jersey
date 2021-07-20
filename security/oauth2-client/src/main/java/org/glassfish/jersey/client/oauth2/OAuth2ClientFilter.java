/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.client.oauth2;

import java.io.IOException;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import javax.annotation.Priority;

/**
 * Client filter that adds access token to the {@code Authorization} http header. The filter uses {@code bearer}
 * token specification.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
@Priority(Priorities.AUTHENTICATION)
class OAuth2ClientFilter implements ClientRequestFilter {

    private final String accessToken;

    /**
     * Create a new filter with predefined access token.
     *
     * @param accessToken Access token.
     */
    public OAuth2ClientFilter(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Create a new filter with no default access token. The token must be specified with
     * each request using {@link OAuth2ClientSupport#OAUTH2_PROPERTY_ACCESS_TOKEN}.
     */
    public OAuth2ClientFilter() {
        this.accessToken = null;
    }

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        String token = this.accessToken;
        final String propertyToken = (String) request.getProperty(OAuth2ClientSupport.OAUTH2_PROPERTY_ACCESS_TOKEN);
        if (propertyToken != null) {
            token = propertyToken;
        }
        request.removeProperty(OAuth2ClientSupport.OAUTH2_PROPERTY_ACCESS_TOKEN);
        if (token == null) {
            return;
        }
        String authentication = "Bearer " + token;

        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, authentication);
        }

    }
}
