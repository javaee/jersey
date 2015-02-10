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

package org.glassfish.jersey.client.oauth2;

import java.util.Collection;
import java.util.Map;

/**
 * Class that contains a result of the Authorization Flow including a access token.
 * <p>
 * All result properties can be get by the method {@link #getAllProperties()}. Some of the properties
 * are standardized by the OAuth 2 specification and therefore the class contains getters that extract
 * these properties from the property map.
 * </p>
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public class TokenResult {

    private final Map<String, Object> properties;

    /**
     * Create a new instance initiated from the property map.
     * @param properties Access properties.
     */
    public TokenResult(final Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Get access token.
     *
     * @return Access token.
     */
    public String getAccessToken() {
        return getProperty("access_token");
    }

    /**
     * Get expiration time of the {@link #getAccessToken() access token} in seconds.
     *
     * @return Expiration time in seconds or {@code null} if the value is not provided.
     */
    public Long getExpiresIn() {
        final String expiration = getProperty("expires_in");
        if (expiration == null) {
            return null;
        }
        return Long.valueOf(expiration);
    }

    /**
     * Get the refresh token. Note that the refresh token must not be issued during the authorization flow.
     * Some Service Providers issue refresh token only on first user authorization and some providers
     * does not support refresh token at all and authorization flow must be always performed when token
     * expires.
     *
     * @return Refresh token or {@code null} if the value is not provided.
     */
    public String getRefreshToken() {
        return getProperty("refresh_token");
    }

    /**
     * Get the type of the returned access token. Type is in most cases {@code bearer} (no cryptography is used)
     * but provider might support also other kinds of token like {@code mac}.
     *
     * @return Token type.
     */
    public String getTokenType() {
        return getProperty("token_type");
    }

    /**
     * Get the map of all properties returned in the Access Token Response.
     *
     * @return Map with all token properties.
     */
    public Map<String, Object> getAllProperties() {
        return properties;
    }

    private String getProperty(final String name) {
        final Object property = properties.get(name);

        if (property != null) {
            if (property instanceof Collection) {
                for (final Object value : (Collection) property) {
                    if (value != null) {
                        return value.toString();
                    }
                }
            } else {
                return property.toString();
            }
        }

        return null;
    }
}
