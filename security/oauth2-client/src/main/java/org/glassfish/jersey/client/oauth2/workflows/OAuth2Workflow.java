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

import org.glassfish.jersey.client.oauth2.ClientCredentialsStore;
import org.glassfish.jersey.client.oauth2.ClientIdentifier;
import org.glassfish.jersey.client.oauth2.OAuth2ClientFilter;
import org.glassfish.jersey.client.oauth2.TokenResult;
import org.glassfish.jersey.client.oauth2.workflows.steps.OAuth2WorkflowStep;
import org.glassfish.jersey.client.oauth2.workflows.steps.RefreshingAccessToken;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import org.glassfish.jersey.client.oauth2.internal.LocalizationMessages;

/**
 * Base interface for OAuth2 workflows. It is intended to be implemented for handling
 * non-interactive( requiring no involvement from Resource Owner) workflows,
 * for e.g. Client Credentials and Password grants.
 * <p>
 *     <b>Implementation details:</b>
 *     <list>
 *         <li>Implement {@link #execute()} method that internally performs the desired workflow
 *         through state transition between multiple {@link OAuth2WorkflowStep} implementations starting
 *         with initial state that is defined by workflow implementation
 *         </li>
 *         <li>
 *             Implement {@link #refreshAccessToken()} method for refresh token implementation.
 *         </li>
 *         <li>{@link org.glassfish.jersey.client.oauth2.OAuth2ClientSupport} provides useful methods
 *         to get instances for different workflows</li>
 *         <li>You can use access token from {@code TokenResult} together with {@link ClientCredentialsStore} to
 *         perform the authenticated requests to the Service Provider. You can also call
 *         methods {@link #getAuthorizedClient()} to get {@link Client client} already configured with support
 *         for authentication from consumer credentials and access token received during authorization process.
 *         </li>
 *     </list>
 * </p>
 * <p>
 *     <b>Usage:</b>
 *     <list>
 *         <li>
 *             Use {@link org.glassfish.jersey.client.oauth2.OAuth2ClientSupport} which provides handy
 *             methods to get {@code Builder}s for different workflows. Needs {@link ClientCredentialsStore}
 *             to retrieve Consumer/Client credentials
 *             For e.g.
 *             <pre>
 *             {@code
 *             OAuth2Workflow.Builder builder
 *                  = OAuth2ClientSupport.clientCredentialsFlowBuilder(clientIdentifier, accessTokenUri, null);
 *             }
 *             </pre>
 *         </li>
 *         <li>
 *             Build {@code OAuth2Workflow}
 *             <pre>
 *             {@code
 *             OAuth2Workflow flow =
 *                                  builder
 *                                      .client(client)
 *                                      .refreshTokenUri(refreshTokenUri)
 *                                      .scope("contact")
 *                                      .build();
 *             }
 *             </pre>
 *         </li>
 *         <li>
 *             Execute the flow
 *             {@code flow.execute();}
 *         </li>
 *         <li>
 *             Get the token result
 *             {@code TokenResult result = flow.getTokenResult();}
 *         </li>
 *         <li>
 *             To refresh token with same workflow instance:
 *             {@code flow.refreshAccessToken();}
 *         </li>
 *     </list>
 * </p>
 * <p>
 * Important note: one instance of the interface can be used only for one authorization process.
 * </p>
 * <p>
 * Instance must be stored between method calls for one user authorization process as the instance keeps
 * internal state of the authorization process for easy use.
 * </p>
 *
 * @author Deepak Pol on 3/11/16.
 */
public interface OAuth2Workflow {

    /**
     * Start the workflow from current step. Initially the first step should be defined
     * by {@code OAuth2Workflow} implementations.
     *
     * @return the {@code OAuth2Workflow} instance, which can be used to {@link #getTokenResult()}
     * or to {@link #refreshAccessToken()}
     */
    OAuth2Workflow execute();

    /**
     * Executes refresh token workflow. By default sets the {@code OAuth2Workflow} state
     * to {@link RefreshingAccessToken} and executes that to refresh token.
     * The {@code TokenResult} will be updated with new token set after successful completion
     * and can be used to get the new tokens.
     */
    default void refreshAccessToken() {
        setState(new RefreshingAccessToken(this));
    }

    /**
     * Set the workflow state to a new step, for e.g. to refresh token from a newly created
     * {@code OAuth2Workflow} instance (which may not have the original state).
     *
     * <b>Implementation Note: The implementations are responsible for executing the step as well.
     * This can be done right away. See {@link AuthorizationCodeFlow} for e.g.</b>
     * @param workflowStep {@link OAuth2WorkflowStep} to be set (and optionally executed) as current step
     */
    void setState(OAuth2WorkflowStep workflowStep);

    /**
     * Get result of workflow execution. {@code null} result might indicate non-executed
     * or newly initialized workflow instance.
     * @return TokenResult
     */
    TokenResult getTokenResult();

    /**
     * Return the http client being used by workflow. The consumers initializing
     * workflow should be able to provide a {@code Client} implementation to use
     * @return {@code Client} being used to execute the workflow
     */
    Client getClient();

    /**
     * @return {@code accessTokenUri} set on the workflow instance.
     */
    String getAccessTokenUri();

    /**
     * Set {@code TokenResult} on workflow instance. Typically done by the workflow after token
     * is available; however, can be also used to initialize new instance of {@code OAuth2Workflow}
     * to perform operations such as {@link #refreshAccessToken()}
     * @param tokenResult to be used
     */
    void setTokenResult(TokenResult tokenResult);

    /**
     * @return {@code refreshTokenProperties} currently set for workflow instance
     */
    Map<String, String> getRefreshTokenProperties();

    /**
     * @return {@code refreshTokenUri} set for workflow instance
     */
    String getRefreshTokenUri();

    /**
     * @return {@code accessTokenProperties} currently set for workflow instance
     */
    Map<String, String> getAccessTokenProperties();

    /**
     * Return authorized HTTP {@code Client} after workflow has been successfully
     * executed. The client should be able to set Authorization headers based
     * on OAuth handshake
     * @return Authorized {@code Client}
     */
    Client getAuthorizedClient();

    default ClientRequestFilter getOAuth2Filter() {
        if (getTokenResult() == null) {
            throw new IllegalStateException(LocalizationMessages.ERROR_FLOW_NOT_FINISHED());
        }
        return new OAuth2ClientFilter(getTokenResult().getAccessToken());
    }

    /**
     * {@code MessageBodyReader} for parsing TokenResult
     */
    class DefaultTokenMessageBodyReader implements MessageBodyReader<TokenResult> {
        // Provider here prevents circular dependency error from HK2 (workers inject providers and this provider inject workers)
        @Inject
        private Provider<MessageBodyWorkers> workers;

        @Inject
        private Provider<PropertiesDelegate> propertiesDelegateProvider;

        private static Iterable<ReaderInterceptor> EMPTY_INTERCEPTORS = new ArrayList<>();

        @Override
        public boolean isReadable(final Class<?> type, final Type genericType,
                                  final Annotation[] annotations,
                                  final MediaType mediaType) {
            return type.equals(TokenResult.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public TokenResult readFrom(final Class<TokenResult> type, final Type genericType, final Annotation[] annotations,
                                    final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                                    final InputStream entityStream) throws IOException, WebApplicationException {

            final GenericType<Map<String, Object>> mapType = new GenericType<Map<String, Object>>() {
            };

            final Map<String, Object> map = (Map<String, Object>) workers.get()
                    .readFrom(
                            mapType.getRawType(),
                            mapType.getType(),
                            annotations,
                            mediaType,
                            httpHeaders,
                            propertiesDelegateProvider.get(),
                            entityStream, EMPTY_INTERCEPTORS, false
                    );

            return new TokenResult(map);
        }
    }

    enum Phase {
        /**
         * Authorization phase. The phase when user is redirected to the authorization server to authorize
         * the application.
         */
        AUTHORIZATION {
            @Override
            public void property(String key, String value, Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps, Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, authorizationProps);
            }
        },
        /**
         * Requesting the access token phase.
         */
        ACCESS_TOKEN_REQUEST {
            @Override
            public void property(String key,
                                 String value,
                                 Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps,
                                 Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, accessTokenProps);
            }
        },
        /**
         * Refreshing the access token phase.
         */
        REFRESH_ACCESS_TOKEN {
            @Override
            public void property(String key,
                                 String value,
                                 Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps,
                                 Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, refreshTokenProps);
            }
        },
        /**
         * All phases.
         */
        ALL {
            @Override
            public void property(String key,
                                 String value,
                                 Map<String, String> authorizationProps,
                                 Map<String, String> accessTokenProps,
                                 Map<String, String> refreshTokenProps) {
                nonNullProperty(key, value, authorizationProps);
                nonNullProperty(key, value, accessTokenProps);
                nonNullProperty(key, value, refreshTokenProps);
            }
        };

        /**
         * Set property defined by {@code key} and {@code value} to the appropriate
         * property map based on this phase.
         * @param key Property key.
         * @param value Property value.
         * @param authorizationProps Properties used in construction of redirect URI.
         * @param accessTokenProps Properties (parameters) used in access token request.
         * @param refreshTokenProps Properties (parameters) used in request for refreshing the access token.
         */
        public abstract void property(String key, String value,
                                      Map<String, String> authorizationProps,
                                      Map<String, String> accessTokenProps,
                                      Map<String, String> refreshTokenProps);

        private static void nonNullProperty(String key, String value, Map<String, String> props) {

            if (props == null) return;

            if (value == null) {
                props.remove(key);
            } else {
                props.put(key, value);
            }
        }

    }

    /**
     * The builder of {@link OAuth2Workflow}.
     *
     * @param <T> Type of the builder implementation. This parameter is used for convenience to allow
     *           better implementations of the builders implementing this interface (builder can return
     *           their own specific type instead of type defined by this interface only).
     */
     interface Builder<T extends Builder<T>> {

        /**
         * Set the access token URI on which the access token can be requested. The URI points to the
         * authorization server and is defined by the Service Provider.
         *
         * @param accessTokenUri  Access token URI.
         * @return Builder instance.
         */
        T accessTokenUri(String accessTokenUri);

        /**
         * Set the redirect URI to which the user (resource owner) should be redirected after he/she
         * grants access to our application. In most cases, the URI is under control of this application
         * and request done on this URI will be used to extract query parameter {@code code} and {@code state}
         * that will be used in
         * {@link OAuth2InteractiveWorkflow#resume(String, String)} method.
         * <p>
         * If URI is not defined by this method, the default value {@code urn:ietf:wg:oauth:2.0:oob} will be used
         * in the Authorization
         * Flow which should cause that {@code code} will be passed to application in other way than request
         * redirection (for example shown to the user using html page).
         * </p>
         *
         * @param redirectUri URI that should receive authorization response from the Service Provider.
         * @return Builder instance.
         */
        T redirectUri(String redirectUri);

        /**
         * Set client identifier of the application that should be authorized.
         *
         * @param clientIdentifier client credentials store
         * @return Builder instance.
         */
        T clientIdentifier(ClientIdentifier clientIdentifier);

        /**
         * Set a scope to which the application will get authorization grant. Values of this parameter
         * are defined by the Service Provider and defines usually subset of resource and operations available
         * in the Service Provider.
         * <p>
         * The parameter is optional but ServiceProvider might require it.
         * </p>
         *
         * @param scope Scope string.
         * @return Builder instance.
         */
        T scope(String scope);

        /**
         * Set the client that should be used internally by the {@code OAuth1AuthorizationFlow} to make requests to
         * Authorization Server. If this method is not called, it is up to the implementation to create or get
         * any private client instance to perform these requests. This method could be used mainly for
         * performance reasons to avoid creation of new client instances and have control about created client
         * instances used in the application.
         *
         * @param client Client instance.
         * @return Builder instance.
         */
        T client(Client client);

        /**
         * Set the refresh token URI on which the access token can be refreshed using a refresh token.
         * The URI points to the
         * authorization server and is defined by the Service Provider. If the URI is not defined by this method
         * it will be the same as URI defined in {@link #accessTokenUri(String)} (which is the default value
         * defined by the OAuth2 spec).
         * Some providers do not support
         * refreshing access tokens at all.
         *
         * @param refreshTokenUri  Refresh token URI.
         * @return Builder instance.
         */
        T refreshTokenUri(String refreshTokenUri);

        /**
         * Set property (parameter) that will be added to requests or URIs as a query parameters during
         * the Authorization Flow. Default parameters used during the Authorization Flow can be also
         * overridden by this method.
         *
         * @param key Property key.
         * @param value Property value.
         * @return Builder instance.
         */
        T property(Phase phase, String key, String value);


        /**
         * Build the {@code OAuth2Workflow} instance.
         *
         * @return New instance of {@code OAuth2Workflow}.
         */
         OAuth2Workflow build();
    }
}
