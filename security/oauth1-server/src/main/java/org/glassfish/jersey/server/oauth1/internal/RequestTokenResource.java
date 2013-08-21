/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.oauth1.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.server.oauth1.OAuth1Consumer;
import org.glassfish.jersey.server.oauth1.OAuth1Exception;
import org.glassfish.jersey.server.oauth1.OAuth1Provider;
import org.glassfish.jersey.server.oauth1.OAuth1Token;
import org.glassfish.jersey.server.oauth1.TokenResource;

/**
 * Resource handling request token requests.
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Martin Matula
 */

@Path("/requestToken")
public class RequestTokenResource {
    @Inject
    private OAuth1Provider provider;

    @Inject
    private ContainerRequestContext requestContext;

    @Inject
    private OAuth1Signature oAuth1Signature;

    /**
     * POST method for creating a request for a Request Token.
     *
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/x-www-form-urlencoded")
    @TokenResource
    public Response postReqTokenRequest() {
        OAuthServerRequest request = new OAuthServerRequest(requestContext);
        OAuth1Parameters params = new OAuth1Parameters();
        params.readRequest(request);

        String tok = params.getToken();
        if ((tok != null) && (!tok.contentEquals(""))) {
            throw new OAuth1Exception(Response.Status.BAD_REQUEST, null);
        }

        String consKey = params.getConsumerKey();
        if (consKey == null) {
            throw new OAuth1Exception(Response.Status.BAD_REQUEST, null);
        }

        OAuth1Consumer consumer = provider.getConsumer(consKey);
        if (consumer == null) {
            throw new OAuth1Exception(Response.Status.BAD_REQUEST, null);
        }
        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret(consumer.getSecret()).tokenSecret("");

        boolean sigIsOk = false;
        try {
            sigIsOk = oAuth1Signature.verify(request, params, secrets);
        } catch (OAuth1SignatureException ex) {
            Logger.getLogger(RequestTokenResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!sigIsOk) {
            throw new OAuth1Exception(Response.Status.BAD_REQUEST, null);
        }

        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<String, String>();
        for (String n : request.getParameterNames()) {
            parameters.put(n, request.getParameterValues(n));
        }

        OAuth1Token rt = provider.newRequestToken(consKey, params.getCallback(), parameters);

        Form resp = new Form();
        resp.param(OAuth1Parameters.TOKEN, rt.getToken());
        resp.param(OAuth1Parameters.TOKEN_SECRET, rt.getSecret());
        resp.param(OAuth1Parameters.CALLBACK_CONFIRMED, "true");
        return Response.ok(resp).build();
    }
}
