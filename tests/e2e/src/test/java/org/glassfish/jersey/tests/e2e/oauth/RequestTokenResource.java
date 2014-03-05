/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

import javax.inject.Inject;

import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;
import org.glassfish.jersey.server.oauth1.internal.OAuthServerRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Path("/request_token")
public class RequestTokenResource {

    @Inject
    private OAuth1Signature oAuth1Signature;

    @POST
    @Produces("text/plain")
    public String get(@Context ContainerRequestContext request) {

        OAuthServerRequest osr = new OAuthServerRequest(request);

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret("kd94hf93k423kf44");

        OAuth1Parameters params = new OAuth1Parameters().readRequest(osr);

        // ensure parameters correctly parsed into OAuth parameters object
        assertEquals(params.getConsumerKey(), "dpf43f3p2l4k3l03");
        assertEquals(params.getSignatureMethod(), "PLAINTEXT");
        assertEquals(params.getSignature(), secrets.getConsumerSecret() + "&");
        assertEquals(params.getTimestamp(), "1191242090");
        assertEquals(params.getNonce(), "hsu94j3884jdopsl");
        assertEquals(params.getVersion(), "1.0");

        try {
            // verify the plaintext signature
            assertTrue(oAuth1Signature.verify(osr, params, secrets));
        } catch (OAuth1SignatureException ose) {
            fail(ose.getMessage());
        }

        return "oauth_token=hh5s93j4hdidpola&oauth_token_secret=hdhd0244k9j7ao03";
    }

}

