package org.glassfish.jersey.client.oauth2.workflows;

import org.glassfish.jersey.client.oauth2.ClientIdentifier;
import org.glassfish.jersey.client.oauth2.OAuth2Parameters;
import org.glassfish.jersey.client.oauth2.workflows.steps.OAuth2WorkflowStep;
import org.glassfish.jersey.client.oauth2.workflows.steps.RequestingAccessToken;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link OAuth2Workflow} for OAuth2
 * <a href="https://tools.ietf.org/html/rfc6749#section-4.4">client_credentials grant</a>
 *
 * <p>
 *     The client credentials grant flow allows a confidential (trusted) client to request
 *     access token by presenting credentials. Thus, needing no interaction from resource owner
 *     to authorize the grant.
 *     The implementation sets initial step during construction to be {@link RequestingAccessToken}
 *     which upon {@link #execute()} executes the access token request.
 * </p>
 * @see org.glassfish.jersey.client.oauth2.OAuth2ClientSupport which provides convinient method
 * to get instance
 * @author Deepak Pol on 3/10/16.
 */
public class ClientCredentialsFlow extends AbstractOAuth2Workflow implements OAuth2Workflow {

    private OAuth2WorkflowStep currentWorkflowStep;

    protected ClientCredentialsFlow(ClientIdentifier clientCredentialsStore,
                                 String accessTokenUri, String refreshTokenUri, String scope){
        this(clientCredentialsStore,
                ClientBuilder.newClient()
                        .register(DefaultTokenMessageBodyReader.class),
                accessTokenUri,
                refreshTokenUri,
                null,
                scope,
                new HashMap<>(),
                new HashMap<>());
    }

    public ClientCredentialsFlow(ClientIdentifier clientIdentifier,
                                 Client client,
                                 String accessTokenUri,
                                 String refreshTokenUri,
                                 String redirectUri, String scope,
                                 Map<String, String> accessTokenProperties,
                                 Map<String, String> refreshTokenProperties){
        super(client,
                clientIdentifier,
                accessTokenUri, refreshTokenUri, redirectUri, scope,
                accessTokenProperties, refreshTokenProperties);
        currentWorkflowStep = new RequestingAccessToken(this);
    }

    /**
     * Execute current step, starting with {@link RequestingAccessToken}
     * @return reference to the instance for further use
     */
    @Override
    public OAuth2Workflow execute() {
        currentWorkflowStep.execute();
        return this;
    }

    /**
     * Allow workflow steps and clients to set/reset current state
     * <b>Note:</b> Setting the state to a step also executes the step
     * @param workflowStep {@link OAuth2WorkflowStep} to be set (and optionally executed) as current step
     */
    @Override
    public void setState(OAuth2WorkflowStep workflowStep) {
        this.currentWorkflowStep = workflowStep;
        execute();
    }

    /**
     * Initialize {@code #accessTokenProperties, #refreshTokenProperties} maps with
     * properties relevant to the flow - {@code clientId, clientSecret, redirectUri, scope}
     * set the grant_type to {@code OAuth2Parameters.GrantType.CLIENT_CREDENTIALS}
     * @param redirectUri optional redirectUri defined by client
     * @param scope optional scope of access
     */
    protected void initDefaultProperties(String redirectUri, String scope) {
        setDefaultProperty(OAuth2Parameters.CLIENT_ID, clientIdentifier.getClientId(),
                accessTokenProperties, refreshTokenProperties);
        setDefaultProperty(OAuth2Parameters.CLIENT_SECRET, clientIdentifier.getClientSecret(),
                accessTokenProperties, refreshTokenProperties);
        setDefaultProperty(OAuth2Parameters.REDIRECT_URI, redirectUri == null
                ? OAuth2Parameters.REDIRECT_URI_UNDEFINED : redirectUri, accessTokenProperties);
        setDefaultProperty(OAuth2Parameters.SCOPE, scope, accessTokenProperties);
        setDefaultProperty(OAuth2Parameters.GrantType.key,
                OAuth2Parameters.GrantType.CLIENT_CREDENTIALS.name().toLowerCase(),
                accessTokenProperties);
        setDefaultProperty(OAuth2Parameters.GrantType.key,
                OAuth2Parameters.GrantType.REFRESH_TOKEN.name().toLowerCase(),
                refreshTokenProperties);
    }

    /**
     * {@code ClientCredentialsFlow} builder
     */
    public static class Builder extends AbstractOAuth2Workflow.Builder<Builder>
            implements OAuth2Workflow.Builder<Builder> {

        public Builder(final ClientIdentifier clientCredentialsStore,
                       final String accessTokenUri,
                       final String redirectUri) {

            this.accessTokenUri = accessTokenUri;
            this.redirectUri = redirectUri;
            this.clientIdentifier = clientCredentialsStore;
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
        public OAuth2Workflow build() {
            return new ClientCredentialsFlow(
                    clientIdentifier,
                    client,
                    accessTokenUri, refreshTokenUri,
                    redirectUri, scope,
                    accessTokenProperties,
                    refreshTokenProperties
            );
        }
    }

}