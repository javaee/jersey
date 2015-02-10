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

package org.glassfish.jersey.client.oauth1;

import java.util.Arrays;

/**
 * Access Token class (credentials issued by the Service Provider for the user).
 * The class stores client secret as byte array to improve security.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public final class AccessToken {

    private final String token;
    private final byte[] accessTokenSecret;


    /**
     * Create a new access token.
     *
     * @param token Access token.
     * @param accessTokenSecret Access token secret.
     */
    public AccessToken(final String token, final String accessTokenSecret) {
        this.token = token;
        this.accessTokenSecret = accessTokenSecret.getBytes();
    }

    /**
     * Create a new access token with secret defined as byte array.
     *
     * @param token Access token.
     * @param accessTokenSecret Access token secret as byte array in the default encoding.
     */
    public AccessToken(final String token, final byte[] accessTokenSecret) {
        this.token = token;
        this.accessTokenSecret = accessTokenSecret;
    }

    /**
     * Get the access token.
     *
     * @return Access token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Get the access token secret.
     * @return Secret part of access token.
     */
    public String getAccessTokenSecret() {
        return new String(accessTokenSecret);
    }

    /**
     * Get the access token secret in byte arrays (in default encoding).
     * @return Byte array with access token secret.
     */
    public byte[] getAccessTokenSecretAsByteArray() {
        return accessTokenSecret;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AccessToken that = (AccessToken) o;

        if (!Arrays.equals(accessTokenSecret, that.accessTokenSecret)) {
            return false;
        }
        if (!token.equals(that.token)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + Arrays.hashCode(accessTokenSecret);
        return result;
    }
}
