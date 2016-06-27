package org.glassfish.jersey.client.oauth2.workflows;

import org.glassfish.jersey.client.oauth2.ClientIdentifier;
import org.glassfish.jersey.client.oauth2.TokenResult;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract implementation of {@link OAuth2Workflow} provides common functionality
 * implementations for different non-interactive flows, such as Client credentials and password
 * flow
 * @author Deepak Pol on 6/15/16.
 */
public abstract class AbstractOAuth2Workflow implements OAuth2Workflow {

    protected Client client;
    protected ClientIdentifier clientIdentifier;
    protected TokenResult tokenResult;

    protected String accessTokenUri;
    protected String refreshTokenUri;
    protected Map<String, String> accessTokenProperties;
    protected Map<String, String> refreshTokenProperties;

    protected AbstractOAuth2Workflow(
            Client client,
            ClientIdentifier clientIdentifier,
            String accessTokenUri,
            String refreshTokenUri) {

        this.clientIdentifier = clientIdentifier;
        this.client = client;
        this.accessTokenUri = accessTokenUri;
        this.refreshTokenUri = refreshTokenUri;
    }

    protected AbstractOAuth2Workflow(Client client,
                                     ClientIdentifier clientIdentifier,
                                     String accessTokenUri,
                                     String refreshTokenUri, String redirectUri,
                                     String scope,
                                     Map<String, String> accessTokenProperties,
                                     Map<String, String> refreshTokenProperties) {

        this(client, clientIdentifier, accessTokenUri, refreshTokenUri);

        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;

        initDefaultProperties(redirectUri, scope);
        configureClient();
    }

    /**
     * Method to be implemented by sub classes to setup properties to be used during
     * flow execution.
     * @param redirectUri optional redirectUri passed by client
     * @param scope optional scope requested by client
     */
    protected abstract void initDefaultProperties(String redirectUri, String scope);

    /**
     * Configure HTTP client for commonly required features
     */
    protected void configureClient() {

        if (client == null) {
            client = ClientBuilder.newClient();
        }

        final Configuration config = client.getConfiguration();
        if (!config.isRegistered(OAuth2Workflow.DefaultTokenMessageBodyReader.class)) {
            client.register(OAuth2Workflow.DefaultTokenMessageBodyReader.class);
        }
        if (!config.isRegistered(JacksonFeature.class)) {
            client.register(JacksonFeature.class);
        }
    }

    @SafeVarargs
    public static void setDefaultProperty(final String key, final String value, final Map<String, String>... properties) {
        if (value == null) {
            return;
        }
        for (final Map<String, String> props : properties) {
            if (props.get(key) == null) {
                props.put(key, value);
            }

        }

    }

    @Override
    public TokenResult getTokenResult() {
        return tokenResult;
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    @Override
    public void setTokenResult(TokenResult tokenResult) {
        this.tokenResult = tokenResult;
    }

    @Override
    public Map<String, String> getRefreshTokenProperties() {
        return refreshTokenProperties;
    }

    @Override
    public String getRefreshTokenUri() {
        return refreshTokenUri;
    }

    @Override
    public Map<String, String> getAccessTokenProperties() {
        return accessTokenProperties;
    }

    @Override
    public Client getAuthorizedClient() {
        return client.register(getOAuth2Filter());
    }

    /**
     * Abstract implementation for {@link org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow.Builder}
     * provides common functionality
     * @param <T> to support appropriate extensions
     */
    public abstract static class Builder<T extends Builder<T>> implements OAuth2Workflow.Builder<T> {
        protected ClientIdentifier clientIdentifier;
        protected String accessTokenUri;
        protected String refreshTokenUri;
        protected String scope;
        protected Client client;
        protected String redirectUri;

        protected Map<String, String> accessTokenProperties = new HashMap<>();
        protected Map<String, String> refreshTokenProperties = new HashMap<>();


        @Override
        public T clientIdentifier(ClientIdentifier clientIdentifier) {
            this.clientIdentifier = clientIdentifier;
            return self();
        }

        @Override
        public T accessTokenUri(String accessTokenUri) {
            this.accessTokenUri = accessTokenUri;
            return self();
        }

        @Override
        public T refreshTokenUri(String refreshTokenUri) {
            this.refreshTokenUri = refreshTokenUri;
            return self();
        }

        @Override
        public T redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return self();
        }

        @Override
        public T scope(String scope) {
            this.scope = scope;
            return self();
        }

        @Override
        public T client(Client client) {
            this.client = client;
            return self();
        }

        /**
         * Allow clients to pass and override {@code accessTokenProperties, refreshTokenProperties}
         * while initializing.
         * @param phase which {@link org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow.Phase} the property is applicable to
         * @param key Property key.
         * @param value Property value.
         * @return self instance
         */
        @Override
        public T property(final Phase phase, String key, String value) {
            phase.property(key, value, null, accessTokenProperties, refreshTokenProperties);
            return self();
        }

        /**
         * Implementations must implement this method and return {@code 'this'} instance so that
         * right reference type is used during builder method call chain
         * @return Concrete Implementation's self instance
         */
        protected abstract T self();

        public abstract OAuth2Workflow build();
    }
}
