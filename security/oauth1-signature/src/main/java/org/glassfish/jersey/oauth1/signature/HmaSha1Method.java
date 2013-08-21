/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.oauth1.signature;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.glassfish.jersey.uri.UriComponent;


/**
 * An OAuth signature method that implements HMAC-SHA1.
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Paul C. Bryan <pbryan@sun.com>
 */
public final class HmaSha1Method implements OAuth1SignatureMethod {

    public static final String NAME = "HMAC-SHA1";

    private static final String SIGNATURE_ALGORITHM = "HmacSHA1";

    @Override
    public String name() {
        return NAME;
    }

    /**
     * Generates the HMAC-SHA1 signature of OAuth request elements.
     *
     * @param baseString the combined OAuth elements to sign.
     * @param secrets the shared secrets used to sign the request.
     * @return the OAuth signature, in base64-encoded form.
     */
    @Override
    public String sign(String baseString, OAuth1Secrets secrets) {

        Mac mac;

        try {
            mac = Mac.getInstance(SIGNATURE_ALGORITHM);
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae);
        }

        StringBuilder buf = new StringBuilder();

        // null secrets are interpreted as blank per OAuth specification
        String secret = secrets.getConsumerSecret();
        if (secret != null) {
            buf.append(UriComponent.encode(secret, UriComponent.Type.UNRESERVED));
        }

        buf.append('&');

        secret = secrets.getTokenSecret();
        if (secret != null) {
            buf.append(UriComponent.encode(secret, UriComponent.Type.UNRESERVED));
        }

        byte[] key;

        try {
            key = buf.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee);
        }

        SecretKeySpec spec = new SecretKeySpec(key, SIGNATURE_ALGORITHM);

        try {
            mac.init(spec);
        } catch (InvalidKeyException ike) {
            throw new IllegalStateException(ike);
        }

        return Base64.encode(mac.doFinal(baseString.getBytes()));
    }

    /**
     * Verifies the HMAC-SHA1 signature of OAuth request elements.
     *
     * @param elements OAuth elements signature is to be verified against.
     * @param secrets the shared secrets for verifying the signature.
     * @param signature base64-encoded OAuth signature to be verified.
     */
    @Override
    public boolean verify(String elements, OAuth1Secrets secrets, String signature) {
        // with symmetric cryptography, simply sign again and compare
        return sign(elements, secrets).equals(signature);
    }
}

