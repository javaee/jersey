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

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.uri.UriComponent;

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
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALRiMLAh9iimur8V"
                    + "A7qVvdqxevEuUkW4K+2KdMXmnQbG9Aa7k7eBjK1S+0LYmVjPKlJGNXHDGuy5Fw/d"
                    + "7rjVJ0BLB+ubPK8iA/Tw3hLQgXMRRGRXXCn8ikfuQfjUS1uZSatdLB81mydBETlJ"
                    + "hI6GH4twrbDJCR2Bwy/XWXgqgGRzAgMBAAECgYBYWVtleUzavkbrPjy0T5FMou8H"
                    + "X9u2AC2ry8vD/l7cqedtwMPp9k7TubgNFo+NGvKsl2ynyprOZR1xjQ7WgrgVB+mm"
                    + "uScOM/5HVceFuGRDhYTCObE+y1kxRloNYXnx3ei1zbeYLPCHdhxRYW7T0qcynNmw"
                    + "rn05/KO2RLjgQNalsQJBANeA3Q4Nugqy4QBUCEC09SqylT2K9FrrItqL2QKc9v0Z"
                    + "zO2uwllCbg0dwpVuYPYXYvikNHHg+aCWF+VXsb9rpPsCQQDWR9TT4ORdzoj+Nccn"
                    + "qkMsDmzt0EfNaAOwHOmVJ2RVBspPcxt5iN4HI7HNeG6U5YsFBb+/GZbgfBT3kpNG"
                    + "WPTpAkBI+gFhjfJvRw38n3g/+UeAkwMI2TJQS4n8+hid0uus3/zOjDySH3XHCUno"
                    + "cn1xOJAyZODBo47E+67R4jV1/gzbAkEAklJaspRPXP877NssM5nAZMU0/O/NGCZ+"
                    + "3jPgDUno6WbJn5cqm8MqWhW1xGkImgRk+fkDBquiq4gPiT898jusgQJAd5Zrr6Q8"
                    + "AO/0isr/3aa6O6NLQxISLKcPDk2NOccAfS/xOtfOz4sJYM3+Bs4Io9+dZGSDCA54"
                    + "Lw03eHTNQghS0A==";
    private static final String RSA_CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n"
                    + "MIIBpjCCAQ+gAwIBAgIBATANBgkqhkiG9w0BAQUFADAZMRcwFQYDVQQDDA5UZXN0\n"
                    + "IFByaW5jaXBhbDAeFw03MDAxMDEwODAwMDBaFw0zODEyMzEwODAwMDBaMBkxFzAV\n"
                    + "BgNVBAMMDlRlc3QgUHJpbmNpcGFsMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
                    + "gQC0YjCwIfYoprq/FQO6lb3asXrxLlJFuCvtinTF5p0GxvQGu5O3gYytUvtC2JlY\n"
                    + "zypSRjVxwxrsuRcP3e641SdASwfrmzyvIgP08N4S0IFzEURkV1wp/IpH7kH41Etb\n"
                    + "mUmrXSwfNZsnQRE5SYSOhh+LcK2wyQkdgcMv11l4KoBkcwIDAQABMA0GCSqGSIb3\n"
                    + "DQEBBQUAA4GBAGZLPEuJ5SiJ2ryq+CmEGOXfvlTtEL2nuGtr9PewxkgnOjZpUy+d\n"
                    + "4TvuXJbNQc8f4AMWL/tO9w0Fk80rWKp9ea8/df4qMq5qlFWlx6yOLQxumNOmECKb\n"
                    + "WpkUQDIDJEoFUzKMVuJf4KO/FJ345+BNLGgbJ6WujreoM1X/gYfdnJ/J\n"
                    + "-----END CERTIFICATE-----";
    private static final String RSA_SIGNATURE = "jvTp/wX1TYtByB1m+Pbyo0lnCOLI"
            + "syGCH7wke8AUs3BpnwZJtAuEJkvQL2/9n4s5wUmUl4aCI4BwpraNx4RtEXMe5qg5"
            + "T1LVTGliMRpKasKsW//e+RinhejgCuzoH26dyF8iY2ZZ/5D1ilgeijhV/vBka5tw"
            + "t399mXwaYdCwFYE=";
    private static final String RSA_SIGNATURE_ENCODED =
            "jvTp%2FwX1TYtByB1m%2BPbyo0lnCOLIsyGCH7wke8AUs3BpnwZJtAuEJkvQL2%2"
                    + "F9n4s5wUmUl4aCI4BwpraNx4RtEXMe5qg5T1LVTGliMRpKasKsW%2F%2Fe%2BRin"
                    + "hejgCuzoH26dyF8iY2ZZ%2F5D1ilgeijhV%2FvBka5twt399mXwaYdCwFYE%3D";

    private static final String RSA_DOCS_PRIVKEY =
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCdiSpLJm3uMcZv"
                    + "T3jUn3SThIb0SxFZTg+Iw5IlLR2B5xHprAni3SEhY0fvpXUEl/oWDIlk++0ni/wt"
                    + "uJ2fdjeB1mT35kNdaKBojmyLMHwTc1zv1zaldXAACkhEraA0tUFSmudExDJ9pngB"
                    + "ZuIkF/7fs6jhWnqoHGERvV9a+/imwoRKgYUrrN+tNO4CU68hJqReDKwDWS0hTrDV"
                    + "wjECBcSHeZ92FrlBp5C+JFL56QvjdYdpN4QWBWVKD+t3YISdUiObl9f6zbGYeMcd"
                    + "omNLlY0Yz9+yMRJhGZJJIx6ftQbg/Y9rWtsgKvHPgjSS+SCRBp8llZt8QZwAAEHP"
                    + "qZeU4jBVAgMBAAECggEBAIXDNP0weUE6VHKpOUDY0CR98BU4NFfu9zO6U7prT1XA"
                    + "vS37XT2bY4k1ApzSkcS/wqK8LGFppBnjO9IaowQGXeVKBNuVUffbYfDFlhatqXze"
                    + "hLhFtenSCLKQCIMAJjr6+KdKTjrOmMyl79nI4RWiplCN/US0Ddf2mNl2QaRAtQXq"
                    + "aTps02qMn0DrebMg3u8Fb7VGCaZ60LKafhDkeLrh9cdbMYZGy0sqZXxkQp/ygjE0"
                    + "lc19fil6qPVF8s45+/BjeBHYFH3hvYRsPYoADsR63rkhP81ALqHZ1AMomvYP0d4w"
                    + "d9KUfEgzcDRiv42Y7dZQB6acImJPl+nWppPQxJaJ61UCgYEAzqrxyxtRr8psILWZ"
                    + "BeaEk6btEIM6t995hHHq6XYsRDmhg30r5V/lJ7u2rrKRWJaiaMPJUmK2autDAZkI"
                    + "/HuhyOaOKMU8FJz+LIfWQD+UCDAdF737ywBctwGzq9nypi0xKj1OYQCTzxT1haTP"
                    + "GVaDvcgHhwaqzT5ru+ITcTFuJIsCgYEAwyPdaQ2gwp5eOcCE791PyuCoRKDSPxzg"
                    + "aL7vplFs9KW+oyKldIdsGA6GI4/23YdbYTbMSc1cjnQre/MffkHiqZk1ixiYICYj"
                    + "yhMCVLz9m53fXmUTOO6w5WU77Ej9+gL02MPxbC0k/0dT07DVakiq2Vk7STqR/8kZ"
                    + "ae6s3EPWOp8CgYA+VzByvP3qEhyFzWGodv288FiIZ515w66LNjXPULdPqTbATCzG"
                    + "lyOv1Z7ombLgTygUhCKheGdgXzEqNTiRuNV3nZx6TeyupyDA3ATUApmr0p+j/soK"
                    + "VUSia6AAEdFxMSaogC+5cQwlJkRdmPZjxUYeJE2o/GjfWpny5eJJfcikuwKBgALO"
                    + "CyqtZXgmqpgN6ltARRtXa1PBNARwN9GJnQw482X40+qoXtRz9dvKqabtNNEuVuPo"
                    + "07rj1sa9aLqZXgSEket6Jkjfi6A6rB0FdO0e4k5QUJucvE//Lk+9ysS0r+HeFQLg"
                    + "niG97GA2+D98tTSX4szI+Y8t5ldU3qalJZrs5rFlAoGBAMZNURtw6Cx+X4zGxiLp"
                    + "wfovxxBkWNjQcssjYpInke/rLDHMQ2dwDTv/DSe9SFXYMh7+pDAppCoKRIFoXmfL"
                    + "50CBFi0ZaL/1EqDFOyVQyLdOPVLv665JcoKWeZj8b+7H25NGCo7nDA6GwVOWq2Ej"
                    + "d3RqXk6nnaeEMcKMBsZQXPqS";
    private static final String RSA_DOCS_CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n"
                    + "MIIDBjCCAe4CCQDqb5RRJpQJ7DANBgkqhkiG9w0BAQUFADBFMQswCQYDVQQGEwJB\n"
                    + "VTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50ZXJuZXQgV2lkZ2l0\n"
                    + "cyBQdHkgTHRkMB4XDTE0MDMwNTE0MDAyNVoXDTI0MDMwMjE0MDAyNVowRTELMAkG\n"
                    + "A1UEBhMCQVUxEzARBgNVBAgTClNvbWUtU3RhdGUxITAfBgNVBAoTGEludGVybmV0\n"
                    + "IFdpZGdpdHMgUHR5IEx0ZDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n"
                    + "AJ2JKksmbe4xxm9PeNSfdJOEhvRLEVlOD4jDkiUtHYHnEemsCeLdISFjR++ldQSX\n"
                    + "+hYMiWT77SeL/C24nZ92N4HWZPfmQ11ooGiObIswfBNzXO/XNqV1cAAKSEStoDS1\n"
                    + "QVKa50TEMn2meAFm4iQX/t+zqOFaeqgcYRG9X1r7+KbChEqBhSus36007gJTryEm\n"
                    + "pF4MrANZLSFOsNXCMQIFxId5n3YWuUGnkL4kUvnpC+N1h2k3hBYFZUoP63dghJ1S\n"
                    + "I5uX1/rNsZh4xx2iY0uVjRjP37IxEmEZkkkjHp+1BuD9j2ta2yAq8c+CNJL5IJEG\n"
                    + "nyWVm3xBnAAAQc+pl5TiMFUCAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAW0d3FHgy\n"
                    + "qS06BS3DXYoBZZPwgKUiiGDkTYaaDJtvMOojCE5k5ZRYii1odNEI27vqVtiMWxr0\n"
                    + "Qn5v1jGRVs15bEPFGIZveqKmetCmZZf6ImIDD1CJjyXT4ZSCwsNqvb11UaOSyzh/\n"
                    + "lCY/GGU8w6RbdPruqflco2UyWldbiKKBtsZS4oOxqmBtdJQyRzOkrmQOyEGaG3m/\n"
                    + "dvU1+rql60QQ3GQww5SOJpMJSooJKB/4ozgHkRCEkZrPTeWTlNj/Gr06mMl5seSS\n"
                    + "ROrHz7//kqgiWnMmo4MWAlycF/Qd7mXLitssk/9utQDWrUYfk7AGgsQ5kHJfvYEE\n"
                    + "n9fmFaDvq29IxA==\n"
                    + "-----END CERTIFICATE-----";
    private static final String RSA_DOCS_SIGNATURE =
            "XqS+Jn/xkY61CwqGtqjkWbaGq5Bdpza5pUgYPOFSmKRAmw2DSF3xofGsy4tUm7S851i91IdMMi"
                    + "G1/1QiKvei5r7j85hsJeZDT7ZtbvmsCbDqclevvfGNm5go5pQnGxP9wkTyiFrAxPQX"
                    + "moVmVEuC+yq1XI/hgSobqCnQTcCPVKMomNNoYC2s+13S9DqGs7JyDBnoo0kKcLoiA/"
                    + "r24a2g11Hmp0n9sSU49lWVpcFi5UX/iu+9M2QL1qz50Sl722j8r/uOfMAB1XBP4LPz"
                    + "G97OFHPKmLHvNo3ppEJmQah0pn43E9Mn9t4jhAXXwKuBgI9A/q5Xx/W27/p7i1CuHc"
                    + "uYQw==";
    private static final String RSA_DOCS_SIGNATURE_ENCODED =
            "XqS%2BJn%2FxkY61CwqGtqjkWbaGq5Bdpza5pUgYPOFSmKRAmw2DSF3xofGsy4tUm7S851i91I"
                    + "dMMiG1%2F1QiKvei5r7j85hsJeZDT7ZtbvmsCbDqclevvfGNm5go5pQnGxP9wkTyiF"
                    + "rAxPQXmoVmVEuC%2Byq1XI%2FhgSobqCnQTcCPVKMomNNoYC2s%2B13S9DqGs7JyDB"
                    + "noo0kKcLoiA%2Fr24a2g11Hmp0n9sSU49lWVpcFi5UX%2Fiu%2B9M2QL1qz50Sl722"
                    + "j8r%2FuOfMAB1XBP4LPzG97OFHPKmLHvNo3ppEJmQah0pn43E9Mn9t4jhAXXwKuBgI"
                    + "9A%2Fq5Xx%2FW27%2Fp7i1CuHcuYQw%3D%3D";

    private static final String RSA_SIGNATURE_METHOD = RsaSha1Method.NAME;
    private static final String RSA_NONCE = "13917289812797014437";
    private static final String RSA_TIMESTAMP = "1196666512";

    /**
     * Perform the test.
     */
    @Test
    public void testHMACSHA1() {
        final OAuth1Signature oauth1Signature = getoAuthSignature();

        DummyRequest request = new DummyRequest().requestMethod("GET")
                .requestURL("http://photos.example.net/photos")
                .parameterValue("file", "vacation.jpg").parameterValue("size", "original");

        OAuth1Parameters params = new OAuth1Parameters().realm(REALM)
                .consumerKey(CONSUMER_KEY).token(ACCESS_TOKEN)
                .signatureMethod(SIGNATURE_METHOD).timestamp(TIMESTAMP)
                .nonce(NONCE).version(VERSION);

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret("kd94hf93k423kf44")
                .tokenSecret("pfkkdhi9sl3r4s00");

        // generate digital signature; ensure it matches the OAuth spec
        String signature = null;

        try {
            signature = oauth1Signature.generate(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            se.printStackTrace();
            fail(se.getMessage());

        }

        assertEquals(signature, SIGNATURE);

        OAuth1Parameters saved = params.clone();

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
        InjectionManager injectionManager = Injections.createInjectionManager(new OAuth1SignatureFeature.Binder());
        injectionManager.completeRegistration();
        return injectionManager.getInstance(OAuth1Signature.class);
    }

    @Test
    public void testRSASHA1() {
        testRSASHA1(RSA_PRIVKEY, RSA_CERTIFICATE, RSA_SIGNATURE, RSA_SIGNATURE_ENCODED);
    }

    @Test
    public void testDocsRsaSha1() {
        testRSASHA1(RSA_DOCS_PRIVKEY, RSA_DOCS_CERTIFICATE, RSA_DOCS_SIGNATURE, RSA_DOCS_SIGNATURE_ENCODED);
    }

    public void testRSASHA1(final String rsaPrivKey, final String rsaCertificate,
                            final String rsaSignature, final String rsaSignatureEncoded) {

        final OAuth1Signature oauth1Signature = getoAuthSignature();
        DummyRequest request = new DummyRequest().requestMethod("GET")
                .requestURL("http://photos.example.net/photos")
                .parameterValue("file", "vacaction.jpg").parameterValue("size", "original");

        OAuth1Parameters params = new OAuth1Parameters().realm(REALM)
                .consumerKey(CONSUMER_KEY)
                .signatureMethod(RSA_SIGNATURE_METHOD).timestamp(RSA_TIMESTAMP)
                .nonce(RSA_NONCE).version(VERSION);

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret(rsaPrivKey);

        // generate digital signature; ensure it matches the OAuth spec
        String signature = null;

        try {
            signature = oauth1Signature.generate(request, params, secrets);
        } catch (OAuth1SignatureException se) {
            se.printStackTrace();
            fail(se.getMessage());
        }
        assertEquals(rsaSignature, signature);

        OAuth1Parameters saved = params.clone();

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
        assertEquals(params.getSignatureMethod(), RSA_SIGNATURE_METHOD);
        assertEquals(params.getTimestamp(), RSA_TIMESTAMP);
        assertEquals(params.getNonce(), RSA_NONCE);
        assertEquals(params.getVersion(), VERSION);
        assertEquals(params.getSignature(), rsaSignature);

        // perform the same encoding as done by OAuth1Parameters.writeRequest
        // to see if the encoded signature will match
        assertEquals(rsaSignatureEncoded, UriComponent.encode(params.getSignature(), UriComponent.Type.UNRESERVED));

        secrets = new OAuth1Secrets().consumerSecret(rsaCertificate);
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

        DummyRequest request = new DummyRequest().requestMethod("POST")
                .requestURL("http://twitter.com/statuses/update.json")
                .parameterValue("status", "Hello Twitter World");

        OAuth1Parameters params = new OAuth1Parameters()
                .consumerKey(CONSUMER_KEY).token(ACCESS_TOKEN)
                .signatureMethod(SIGNATURE_METHOD).timestamp(TIMESTAMP)
                .nonce(NONCE).version(VERSION);

        OAuth1Secrets secrets =
                new OAuth1Secrets().consumerSecret("kd94hf93k423kf44")
                        .tokenSecret("pfkkdhi9sl3r4s00");

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
