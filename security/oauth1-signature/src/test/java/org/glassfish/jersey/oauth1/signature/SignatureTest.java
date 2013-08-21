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

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.uri.UriComponent;

import org.glassfish.hk2.api.ServiceLocator;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
public class SignatureTest {

    // values from OAuth specification appendices to demonstrate protocol operation
    private static final String REALM = "http://photos.example.net/";
    private static final String CONSUMER_KEY = "dpf43f3p2l4k3l03";
    private static final String ACCESS_TOKEN = "nnch734d00sl2jdk";
    private static final String SIGNATURE_METHOD = HmaSha1Method.NAME;
    private static final String TIMESTAMP = "1191242096";
    private static final String NONCE = "kllo9940pd9333jh";
    private static final String VERSION = "1.0";
    private static final String SIGNATURE = "tR3+Ty81lMeYAr/Fid0kMTYa/WM=";

    private static final String RSA_PRIVKEY =
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V" +
                    "A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d" +
                    "7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ" +
                    "hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H" +
                    "X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm" +
                    "uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw" +
                    "rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z" +
                    "zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn" +
                    "qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG" +
                    "WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno" +
                    "cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+" +
                    "3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8" +
                    "AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54" +
                    "Lw03eHTNQghS0A==";
    private static final String RSA_CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n" +
                    "MIIBpjCCAQ+gAwIBAgIBATANBgkqhkiG9w0BAQUFADAZMRcwFQYDVQQDDA5UZXN0\n" +
                    "IFByaW5jaXBhbDAeFw03MDAxMDEwODAwMDBaFw0zODEyMzEwODAwMDBaMBkxFzAV\n" +
                    "BgNVBAMMDlRlc3QgUHJpbmNpcGFsMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n" +
                    "gQC0YjCwIfYoprq/FQO6lb3asXrxLlJFuCvtinTF5p0GxvQGu5O3gYytUvtC2JlY\n" +
                    "zypSRjVxwxrsuRcP3e641SdASwfrmzyvIgP08N4S0IFzEURkV1wp/IpH7kH41Etb\n" +
                    "mUmrXSwfNZsnQRE5SYSOhh+LcK2wyQkdgcMv11l4KoBkcwIDAQABMA0GCSqGSIb3\n" +
                    "DQEBBQUAA4GBAGZLPEuJ5SiJ2ryq+CmEGOXfvlTtEL2nuGtr9PewxkgnOjZpUy+d\n" +
                    "4TvuXJbNQc8f4AMWL/tO9w0Fk80rWKp9ea8/df4qMq5qlFWlx6yOLQxumNOmECKb\n" +
                    "WpkUQDIDJEoFUzKMVuJf4KO/FJ345+BNLGgbJ6WujreoM1X/gYfdnJ/J\n" +
                    "-----END CERTIFICATE-----";
    private static final String RSA_SIGNATURE_METHOD = RsaSha1Method.NAME;
    private static final String RSA_SIGNATURE = "jvTp/wX1TYtByB1m+Pbyo0lnCOLI" +
            "syGCH7wke8AUs3BpnwZJtAuEJkvQL2/9n4s5wUmUl4aCI4BwpraNx4RtEXMe5qg5" +
            "T1LVTGliMRpKasKsW//e+RinhejgCuzoH26dyF8iY2ZZ/5D1ilgeijhV/vBka5tw" +
            "t399mXwaYdCwFYE=";
    private static final String RSA_SIGNATURE_ENCODED =
            "jvTp%2FwX1TYtByB1m%2BPbyo0lnCOLIsyGCH7wke8AUs3BpnwZJtAuEJkvQL2%2" +
                    "F9n4s5wUmUl4aCI4BwpraNx4RtEXMe5qg5T1LVTGliMRpKasKsW%2F%2Fe%2BRin" +
                    "hejgCuzoH26dyF8iY2ZZ%2F5D1ilgeijhV%2FvBka5twt399mXwaYdCwFYE%3D";

    private static final String RSA_NONCE = "13917289812797014437";
    private static final String RSA_TIMESTAMP = "1196666512";


    /**
     * Perform the test.
     */
    @Test
    public void testHMACSHA1() {
        final OAuth1Signature oauth1Signature = getoAuthSignature();

        DummyRequest request = new DummyRequest().requestMethod("GET").
                requestURL("http://photos.example.net/photos").
                parameterValue("file", "vacation.jpg").parameterValue("size", "original");

        OAuth1Parameters params = new OAuth1Parameters().realm(REALM).
                consumerKey(CONSUMER_KEY).token(ACCESS_TOKEN).
                signatureMethod(SIGNATURE_METHOD).timestamp(TIMESTAMP).
                nonce(NONCE).version(VERSION);

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret("kd94hf93k423kf44").
                tokenSecret("pfkkdhi9sl3r4s00");

        // generate digital signature; ensure it matches the OAuth spec
        String signature = null;

        try {
            signature = oauth1Signature.generate(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            se.printStackTrace();
            fail(se.getMessage());

        }

        assertEquals(signature, SIGNATURE);

        OAuth1Parameters saved = (OAuth1Parameters) params.clone();

        try {
            // sign the request; clear params; parse params from request; ensure they match original
            oauth1Signature.sign(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            fail(se.getMessage());
        }

        // signing the request should not have modified the original parameters
        assertTrue(params.equals(saved));
        assertTrue(params.getSignature() == null);

        params = new OAuth1Parameters();
        params.readRequest(request);
        assertEquals(params.getRealm(), REALM);
        assertEquals(params.getConsumerKey(), CONSUMER_KEY);
        assertEquals(params.getToken(), ACCESS_TOKEN);
        assertEquals(params.getSignatureMethod(), SIGNATURE_METHOD);
        assertEquals(params.getTimestamp(), TIMESTAMP);
        assertEquals(params.getNonce(), NONCE);
        assertEquals(params.getVersion(), VERSION);
        assertEquals(params.getSignature(), SIGNATURE);

        try {
            // verify signature using request that was just signed
            assertTrue(oauth1Signature.verify(request, params, secrets));
        } catch (OAuth1SignatureException se) {
            fail(se.getMessage());
        }
    }

    private OAuth1Signature getoAuthSignature() {
        ServiceLocator serviceLocator = Injections.createLocator(new OAuth1SignatureFeature.Binder());
        return serviceLocator.getService(OAuth1Signature.class);
    }


    @Test
    public void testRSASHA1() {
        final OAuth1Signature oauth1Signature = getoAuthSignature();
        DummyRequest request = new DummyRequest().requestMethod("GET").
                requestURL("http://photos.example.net/photos").
                parameterValue("file", "vacaction.jpg").parameterValue("size", "original");

        OAuth1Parameters params = new OAuth1Parameters().realm(REALM).
                consumerKey(CONSUMER_KEY).
                signatureMethod(RSA_SIGNATURE_METHOD).timestamp(RSA_TIMESTAMP).
                nonce(RSA_NONCE).version(VERSION);

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret(RSA_PRIVKEY);

        // generate digital signature; ensure it matches the OAuth spec
        String signature = null;

        try {
            signature = oauth1Signature.generate(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            se.printStackTrace();
            fail(se.getMessage());
        }
        assertEquals(signature, RSA_SIGNATURE);

        OAuth1Parameters saved = (OAuth1Parameters) params.clone();

        try {
            // sign the request; clear params; parse params from request; ensure they match original
            oauth1Signature.sign(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            fail(se.getMessage());
        }

        // signing the request should not have modified the original parameters
        assertTrue(params.equals(saved));
        assertTrue(params.getSignature() == null);

        params = new OAuth1Parameters();
        params.readRequest(request);
        assertEquals(params.getRealm(), REALM);
        assertEquals(params.getConsumerKey(), CONSUMER_KEY);
//        assertEquals(params.getToken(), ACCESS_TOKEN);
        assertEquals(params.getSignatureMethod(), RSA_SIGNATURE_METHOD);
        assertEquals(params.getTimestamp(), RSA_TIMESTAMP);
        assertEquals(params.getNonce(), RSA_NONCE);
        assertEquals(params.getVersion(), VERSION);
        assertEquals(params.getSignature(), RSA_SIGNATURE);

        // perform the same encoding as done by OAuth1Parameters.writeRequest
        // to see if the encoded signature will match
        assertEquals(UriComponent.encode(params.getSignature(), UriComponent.Type.UNRESERVED), RSA_SIGNATURE_ENCODED);

        secrets = new OAuth1Secrets().consumerSecret(RSA_CERTIFICATE);
        try {
            // verify signature using request that was just signed
            assertTrue(oauth1Signature.verify(request, params, secrets));
        } catch (OAuth1SignatureException se) {
            fail(se.getMessage());
        }
    }


    /**
     * Test a Twitter status update.
     *
     * Specifically, this test includes some characters (spaces) in one of the
     * parameters which were incorrectly encoded (as '+' instead of "%20") with
     * the original encoding routine.
     */
    @Test
    public void testTwitterSig() {
        final OAuth1Signature oauth1Signature = getoAuthSignature();
        final String TWITTERTEST_SIGNATURE = "yfrn/p/4Hnp+XcwUBVfW0cSgc+o=";
        final String TWITTERTEST_SIGNATURE_ENC =
                "yfrn%2Fp%2F4Hnp%2BXcwUBVfW0cSgc%2Bo%3D";

        DummyRequest request = new DummyRequest().requestMethod("POST").
                requestURL("http://twitter.com/statuses/update.json").
                parameterValue("status", "Hello Twitter World");

        OAuth1Parameters params = new OAuth1Parameters().
                consumerKey(CONSUMER_KEY).token(ACCESS_TOKEN).
                signatureMethod(SIGNATURE_METHOD).timestamp(TIMESTAMP).
                nonce(NONCE).version(VERSION);

        OAuth1Secrets secrets =
                new OAuth1Secrets().consumerSecret("kd94hf93k423kf44").
                        tokenSecret("pfkkdhi9sl3r4s00");

        // generate digital signature; ensure it matches the OAuth spec
        String signature = null;

        try {
            signature = oauth1Signature.generate(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            se.printStackTrace();
            fail(se.getMessage());
        }

        assertEquals(signature, TWITTERTEST_SIGNATURE);


        OAuth1Parameters saved = (OAuth1Parameters) params.clone();

        try {
            // sign the request; clear params; parse params from request;
            // ensure they match original
            oauth1Signature.sign(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            fail(se.getMessage());
        }

        // signing the request should not have modified the original parameters
        assertTrue(params.equals(saved));
        assertTrue(params.getSignature() == null);

        params = new OAuth1Parameters();
        params.readRequest(request);
        assertEquals(params.getConsumerKey(), CONSUMER_KEY);
        assertEquals(params.getToken(), ACCESS_TOKEN);
        assertEquals(params.getSignatureMethod(), SIGNATURE_METHOD);
        assertEquals(params.getTimestamp(), TIMESTAMP);
        assertEquals(params.getNonce(), NONCE);
        assertEquals(params.getVersion(), VERSION);
        assertEquals(params.getSignature(), TWITTERTEST_SIGNATURE);

        try {
            // verify signature using request that was just signed
            assertTrue(oauth1Signature.verify(request, params, secrets));
        } catch (OAuth1SignatureException se) {
            fail(se.getMessage());
        }
    }
}
