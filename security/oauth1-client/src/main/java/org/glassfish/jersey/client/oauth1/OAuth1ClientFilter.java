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

import java.io.IOException;

import javax.ws.rs.Priorities;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.client.oauth1.internal.LocalizationMessages;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;

/**
 * Client filter that sign requests using OAuth 1 signatures and signature and other OAuth 1
 * parameters to the {@code Authorization} header. The filter can be used to perform authenticated
 * requests to Service Provider but also to perform requests needed for Authorization process (flow).
 *
 * @author Paul C. Bryan
 * @author Martin Matula
 * @author Miroslav Fuksa
 *
 * @since 2.3
 */
@Priority(Priorities.AUTHENTICATION)
class OAuth1ClientFilter implements ClientRequestFilter {

    @Inject
    private Provider<OAuth1Signature> oAuthSignature;

    @Inject
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        final ConsumerCredentials consumerFromProperties
                = (ConsumerCredentials) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_CONSUMER_CREDENTIALS);
        request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_CONSUMER_CREDENTIALS);

        final AccessToken tokenFromProperties
                = (AccessToken) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_ACCESS_TOKEN);
        request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_ACCESS_TOKEN);

        OAuth1Parameters parameters = (OAuth1Parameters) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        if (parameters == null) {
            parameters = (OAuth1Parameters) request.getConfiguration()
                    .getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        } else {
            request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        }

        OAuth1Secrets secrets = (OAuth1Secrets) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS);
        if (secrets == null) {
            secrets = (OAuth1Secrets) request.getConfiguration().getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS);
        } else {
            request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS);
        }

        if (request.getHeaders().containsKey("Authorization")) {
            return;
        }

        // Make modifications to clones.
        final OAuth1Parameters paramCopy = parameters.clone();
        final OAuth1Secrets secretsCopy = secrets.clone();

        checkParametersConsistency(paramCopy, secretsCopy);

        if (consumerFromProperties != null) {
            paramCopy.consumerKey(consumerFromProperties.getConsumerKey());
            secretsCopy.consumerSecret(consumerFromProperties.getConsumerSecret());
        }

        if (tokenFromProperties != null) {
            paramCopy.token(tokenFromProperties.getToken());
            secretsCopy.tokenSecret(tokenFromProperties.getAccessTokenSecret());
        }

        if (paramCopy.getTimestamp() == null) {
            paramCopy.setTimestamp();
        }

        if (paramCopy.getNonce() == null) {
            paramCopy.setNonce();
        }

        try {
            oAuthSignature.get().sign(new RequestWrapper(request, messageBodyWorkers.get()), paramCopy, secretsCopy);
        } catch (OAuth1SignatureException se) {
            throw new ProcessingException(LocalizationMessages.ERROR_REQUEST_SIGNATURE(), se);
        }
    }

    private void checkParametersConsistency(OAuth1Parameters oauth1Parameters, OAuth1Secrets oauth1Secrets) {
        if (oauth1Parameters.getSignatureMethod() == null) {
            oauth1Parameters.signatureMethod("HMAC-SHA1");
        }

        if (oauth1Parameters.getVersion() == null) {
            oauth1Parameters.version();
        }

        if (oauth1Secrets.getConsumerSecret() == null || oauth1Parameters.getConsumerKey() == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_CONSUMER());
        }

        if (oauth1Parameters.getToken() != null && oauth1Secrets.getTokenSecret() == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_TOKEN_SECRET());
        }
    }
}
