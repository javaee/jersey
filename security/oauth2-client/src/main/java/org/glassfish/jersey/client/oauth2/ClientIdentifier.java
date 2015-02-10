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

/**
 * Client Identifier that contains information about client id and client secret issues by a
 * Service Provider for application. The class stores client secret as byte array to improve security.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public class ClientIdentifier {
    private final String clientId;
    private final byte[] clientSecret;


    /**
     * Create a new instance initialized with client id and client secret in form of String value.
     *
     * @param clientId Client id.
     * @param clientSecret Client secret id.
     */
    public ClientIdentifier(final String clientId, final String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret.getBytes();
    }

    /**
     * Create a new instance initialized with client id and client secret in form of byte array.
     *
     * @param clientId Client id.
     * @param clientSecret Client secret id as a byte array value in the default encoding.
     */
    public ClientIdentifier(final String clientId, final byte[] clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Get the client id.
     *
     * @return Client id.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get client secret.
     *
     * @return Client secret as a String.
     */
    public String getClientSecret() {
        return new String(clientSecret);
    }

    /**
     * Get client secret as byte array.
     *
     * @return Client secret as a byte array.
     */
    public byte[] getClientSecretAsByteArray() {
        return clientSecret;
    }
}


