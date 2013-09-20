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

package org.glassfish.jersey.tests.e2e.oauth;


import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.oauth1.AccessToken;
import org.glassfish.jersey.client.oauth1.ConsumerCredentials;
import org.glassfish.jersey.client.oauth1.OAuth1Builder;
import org.glassfish.jersey.client.oauth1.OAuth1ClientSupport;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureFeature;
import org.glassfish.jersey.oauth1.signature.PlaintextMethod;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class OauthClientAuthorizationFlowTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(AccessTokenResource.class, PhotosResource.class, RequestTokenResource.class,
                OAuth1SignatureFeature.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyTestContainerFactory();
    }

    /**
     * Tests mainly the client functionality. The test client registers {@link org.glassfish.jersey.client.oauth1.OAuthClientFilter}
     * and uses the filter only to sign requests. So, it does not use the filter to perform
     * authorization flow. However, each request that this test performs is actually a request used
     * during the authorization flow.
     * <p/>
     * The server side of this test extracts header authorization values and tests that signatures are
     * correct for each request type.
     */
    @Test
    public void testOauthClient() {
        final URI baseUri = getBaseUri();

        // baseline for requests
        final OAuth1Builder oAuth1Builder = OAuth1ClientSupport.builder(
                new ConsumerCredentials("dpf43f3p2l4k3l03", "kd94hf93k423kf44"))
                .timestamp("1191242090")
                .nonce("hsu94j3884jdopsl")
                .signatureMethod(PlaintextMethod.NAME).version("1.0");
        final Feature feature = oAuth1Builder
                .feature().build();


        final Client client = client();
        client.register(new LoggingFilter());
        final WebTarget target = client.target(baseUri);

        // simulate request for Request Token (temporary credentials)
        String responseEntity = target.path("request_token").register(feature)
                .request().post(Entity.entity("entity", MediaType.TEXT_PLAIN_TYPE), String.class);
        assertEquals(responseEntity, "oauth_token=hh5s93j4hdidpola&oauth_token_secret=hdhd0244k9j7ao03");


        final Feature feature2 = oAuth1Builder.timestamp("1191242092").nonce("dji430splmx33448").feature()
                .accessToken(new AccessToken("hh5s93j4hdidpola", "hdhd0244k9j7ao03")).build();

        // simulate request for Access Token
        responseEntity = target.path("access_token").register(feature2)
                .request().post(Entity.entity("entity", MediaType.TEXT_PLAIN_TYPE), String.class);
        assertEquals(responseEntity, "oauth_token=nnch734d00sl2jdk&oauth_token_secret=pfkkdhi9sl3r4s00");

        final Feature feature3 = oAuth1Builder.nonce("kllo9940pd9333jh").signatureMethod("HMAC-SHA1").timestamp("1191242096")
                .feature().accessToken(new AccessToken("nnch734d00sl2jdk", "pfkkdhi9sl3r4s00")).build();

        // based on Access Token
        responseEntity = target.path("/photos").register(feature3)
                .queryParam("file", "vacation.jpg")
                .queryParam("size", "original")
                .request().get(String.class);

        assertEquals(responseEntity, "PHOTO");
    }
}