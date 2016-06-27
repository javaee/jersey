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

package org.glassfish.jersey.client.oauth2.workflows;

import org.glassfish.jersey.client.oauth2.OAuth2Parameters;
import org.glassfish.jersey.client.oauth2.ClientIdentifier;
import org.glassfish.jersey.client.oauth2.internal.LocalizationMessages;
import org.glassfish.jersey.client.oauth2.workflows.steps.AuthorizationRequest;
import org.glassfish.jersey.client.oauth2.workflows.steps.AuthorizationResponse;
import org.glassfish.jersey.client.oauth2.workflows.steps.AwaitingAuthorizationResponse;
import org.glassfish.jersey.client.oauth2.workflows.steps.OAuth2WorkflowStep;

import javax.ws.rs.client.Client;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * * Implementation of {@link OAuth2InteractiveWorkflow} for OAuth2
 * <a href="https://tools.ietf.org/html/rfc6749#section-4.1">authorization_code grant</a>
 *
 * <p>
 *     Authorization code flow expects client to be capable of interacting with user(Resource owner)
 *     to get the authorization grant.
 *     <list>
 *         <li>
 *              The implementation sets initial step during construction to be {@link AuthorizationRequest}
 *              which upon {@link #execute()} executes prepares {@code authorizationUri} which is available
 *              to the client by calling {@code #getAuthorizationRequestUri} which can be used to handle
 *              user redirection to authorization grant page
 *          </li>
 *          <li>
 *              The client receives {@code authorizationCode} after request is authorized which can be
 *              passed along with {@code state} to {@link #resume(String, String)} the flow
 *          </li>
 *     </list>
 * </p>
 * @see org.glassfish.jersey.client.oauth2.OAuth2ClientSupport which provides convinient method
 * to get instance
 * @author Deepak Pol on 6/14/16.
 */
public class AuthorizationCodeFlow extends AbstractOAuth2InteractiveWorkflow implements OAuth2InteractiveWorkflow {

    private OAuth2WorkflowStep currentWorkflowStep;

    public AuthorizationCodeFlow(ClientIdentifier clientIdentifier,
                                 Client client,
                                 String authorizationUri,
                                 String accessTokenUri, String refreshTokenUri,
                                 String redirectUri, String scope) {
        this(clientIdentifier, client,
                authorizationUri, accessTokenUri,
                refreshTokenUri, redirectUri, scope,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>());

    }

    public AuthorizationCodeFlow(ClientIdentifier clientIdentifier,
                                 Client client,
                                 String authorizationUri, String accessTokenUri,
                                 String refreshTokenUri, String redirectUri,
                                 String scope,
                                 Map<String, String> authorizationProperties,
                                 Map<String, String> accessTokenProperties,
                                 Map<String, String> refreshTokenProperties) {
        super(client, clientIdentifier, authorizationUri,
                accessTokenUri, refreshTokenUri, redirectUri, scope,
                authorizationProperties, accessTokenProperties, refreshTokenProperties);

        currentWorkflowStep = new AuthorizationRequest(this);

    }

    /**
     * Authorization request URI that is available after {@link AuthorizationRequest} step is
     * complete during flow execution.
     * @return authorization request URI to be used by client for resource owner redirection
     */
    @Override
    public String getAuthorizationRequestUri(){
        return authorizationProperties.get(OAuth2Parameters.AUTHORIZATION_REQUEST_URI);
    }

    /**
     * Execute the current step in the workflow, defaults to start with {@link AuthorizationRequest}
     * during instance construction
     * @return flow instance for further use - to {@code resume} the flow and to get {@link org.glassfish.jersey.client.oauth2.TokenResult}
     */
    @Override
    public OAuth2InteractiveWorkflow execute() {
        currentWorkflowStep.execute();
        return this;
    }

    /**
     * Resume the workflow once user(resource owner) grants authority to the client.
     * Note: Must be called on existing {@code OAuth2InteractiveWorkflow} instance since it
     * holds the flow state
     * @param authorizationCode authorizationCode received from Authorization Server after auth grant
     * @param state optional workflow state interchanged between server and client
     */
    @Override
    public void resume(final String authorizationCode, final String state) {
        if (!(currentWorkflowStep instanceof AwaitingAuthorizationResponse)){
           throw new IllegalStateException(LocalizationMessages.ERROR_FLOW_WRONG_STATE());
        }

        setState(new AuthorizationResponse(this, authorizationCode, state));
    }

    /**
     * Allow workflow steps and clients to set/reset current state
     * <b>Note:</b> Setting the state to a step also executes the step
     * @param workflowStep {@link OAuth2WorkflowStep} to be set (and optionally executed) as current step
     */
    @Override
    public void setState(OAuth2WorkflowStep workflowStep) {
        currentWorkflowStep = workflowStep;
        execute();
    }

    /**
     * Initialize {@code #authorizationProperties, #accessTokenProperties, #refreshTokenProperties} maps with
     * properties relevant to the flow - {@code clientId, clientSecret, redirectUri, scope}
     * set the grant_type to {@code OAuth2Parameters.GrantType.AUTHORIZATION_CODE}. {@code state} is
     * verified against during the flow resumption using {@link #resume(String, String)}
     * @param redirectUri optional redirectUri defined by client
     * @param scope optional scope of access
     */
    protected void initDefaultProperties(String redirectUri, String scope) {
        setDefaultProperty(OAuth2Parameters.RESPONSE_TYPE, "code", authorizationProperties);
        setDefaultProperty(OAuth2Parameters.CLIENT_ID, clientIdentifier.getClientId(), authorizationProperties,
                accessTokenProperties, refreshTokenProperties);
        setDefaultProperty(OAuth2Parameters.REDIRECT_URI, redirectUri == null
                ? OAuth2Parameters.REDIRECT_URI_UNDEFINED : redirectUri, authorizationProperties, accessTokenProperties);

        setDefaultProperty(OAuth2Parameters.STATE, UUID.randomUUID().toString(), authorizationProperties);
        setDefaultProperty(OAuth2Parameters.SCOPE, scope, authorizationProperties);

        setDefaultProperty(OAuth2Parameters.CLIENT_SECRET, clientIdentifier.getClientSecret(), accessTokenProperties,
                refreshTokenProperties);
        setDefaultProperty(OAuth2Parameters.GrantType.key, OAuth2Parameters.GrantType.AUTHORIZATION_CODE.name().toLowerCase(),
                accessTokenProperties);

        setDefaultProperty(OAuth2Parameters.GrantType.key, OAuth2Parameters.GrantType.REFRESH_TOKEN.name().toLowerCase(),
                refreshTokenProperties);
    }


    /**
     * Implementation of {@link org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow.Builder}
     */
    public static class Builder extends AbstractOAuth2InteractiveWorkflow.Builder<Builder>
            implements OAuth2InteractiveWorkflow.Builder<Builder> {

        public Builder(){}
        /**
         * Create a new builder with defined URIs and client id and callback uri.
         */
        public Builder(final ClientIdentifier clientIdentifier,
                       final String authorizationUri, final String accessTokenUri,
                       final String redirectUri) {

            this.accessTokenUri = accessTokenUri;
            this.authorizationUri = authorizationUri;
            this.redirectUri = redirectUri;
            this.clientIdentifier = clientIdentifier;
        }

        /**
         * Template method implementation for {@link org.glassfish.jersey.client.oauth2.workflows.AbstractOAuth2Workflow.Builder}
         * so that the call chain has reference to right implementation
         * @return self instance
         */
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public OAuth2InteractiveWorkflow build() {
            return new AuthorizationCodeFlow(clientIdentifier,
                    client,
                    authorizationUri,
                    accessTokenUri,
                    refreshTokenUri,
                    redirectUri,
                    scope,
                    authorizationProperties,
                    accessTokenProperties,
                    refreshTokenProperties
            );
        }
    }

}
