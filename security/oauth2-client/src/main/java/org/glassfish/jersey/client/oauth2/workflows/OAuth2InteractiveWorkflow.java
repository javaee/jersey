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

import javax.ws.rs.client.Client;
import java.util.Map;

/**
 * Interface for implementations of Interactive OAuth2 grant workflows
 * such as Authorization Code and Implicit workflows
 *
 * <p>
 *     <b>Implementation details:</b>
 *     <list>
 *         <li>
 *             In case of {@code OAuth2InteractiveWorkflow}, the {@link #execute()} method
 *             should go into a waiting state, such as
 *             {@link org.glassfish.jersey.client.oauth2.workflows.steps.AwaitingAuthorizationResponse}
 *             for the caller to proceed user interaction to get the grant for requesting consumer
 *             by redirecting the Resource Owner (user) to authorization request uri. The method returns
 *             current instance of {@code OAuth2InteractiveWorkflow} which can be used to get
 *             {@link #getAuthorizationRequestUri()}
 *         </li>
 *         <li>
 *             After {@link #execute()} completes the first part of the workflow, {@link #getAuthorizationRequestUri()}
 *             should have the valid authorization request URI which should return the request URI for
 *             the current flow.
 *             Note that the same {@code OAuth2InteractiveWorkflow} instance should be used to get the
 *             {@code authorizationRequestUri}
 *         </li>
 *         <li>
 *             Workflow {@link #resume(String, String)} method accepts {@code authorizationCode} and
 *             {@code state} as parameters respectively which should be used to resume the workflow
 *             with next step by setting {@link org.glassfish.jersey.client.oauth2.workflows.steps.AuthorizationResponse}
 *         </li>
 *         <li>You can use access token from {@code TokenResult} together with {@link ClientCredentialsStore} to
 *              perform the authenticated requests to the Service Provider. You can also call
 *              methods {@link #getAuthorizedClient()} to get {@link Client client} already configured with support
 *              for authentication from consumer credentials and access token received during authorization process.
 *         </li>
 *     </list>
 * </p>
 * <p>
 *     <b>Usage Example:</b>
 *     <list>
 *         <li>
 *             Use {@link org.glassfish.jersey.client.oauth2.OAuth2ClientSupport} which provides handy
 *             methods to get {@code Builder}s for different workflows. Needs {@link ClientCredentialsStore}
 *             to retrieve Consumer/Client credentials
 *             For e.g.
 *             <pre>
 *             {@code
 *             OAuth2Workflow.Builder builder
 *                  = OAuth2ClientSupport.authorizationCodeGrantFlowBuilder(
 *                                          ClientCredentialsStore clientIdentifier,
 *                                          String authorizationUri,
 *                                          String accessTokenUri,
 *                                          String redirectUri);
 *             }
 *             </pre>
 *         </li>
 *         <li>
 *             Build {@code OAuth2Workflow}
 *             <pre>
 *             {@code
 *             OAuth2Workflow flow =
 *             builder
 *                    .client(client)
 *                    .refreshTokenUri(refreshTokenUri)
 *                    .property(OAuth2Workflow.Phase.AUTHORIZATION, OAuth2Parameters.STATE, state) // Override default params
 *                    .scope("contact")
 *                    .build();
 *             }
 *             </pre>
 *         </li>
 *         <li>
 *             Execute the flow
 *             {@code flow.execute();}
 *         </li>
 *         <li>
 *             Get the authorizationRequestUri, {@code flow.getAuthorizationRequestUri()} instance and
 *             redirect client to get user's (Resource Owner's) grant
 *         </li>
 *         <li>
 *             The server should return {@code authorizationCode} and {@code state} after user grant
 *             which can be used to resume workflow to get access token set
 *             {@code flow.resume(code, state);}
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
 * Important note: one instance of the interface can be used only for one authorization process. The methods
 * must be called exactly in the order specified by the list above. Therefore the instance is also not
 * thread safe and no concurrent access is expected.
 * </p>
 * <p>
 * Instance must be stored between method calls (between {@code execute} and {@code resume})
 * for one user authorization process as the instance keeps
 * internal state of the authorization process.
 * </p>
 *
 * @author Deepak Pol on 6/14/16.
 */
public interface OAuth2InteractiveWorkflow extends OAuth2Workflow {

    /**
     * Overrides {@link OAuth2Workflow#execute()} to return {@code OAuth2InteractiveWorkflow}
     * which can be used to continue workflow {@code resume()}
     * Implementations should ensure that {@link #getAuthorizationRequestUri()} has valid
     * uri after the flow is started by {@code execute()}
     * @return interactive workflow instance with {@code authorizationRequestUri} and ability to {@code resume}
     * workflow
     */
    @Override
    OAuth2InteractiveWorkflow execute();

    /**
     * API to resume workflow after user(Resource Owner) approves the grant for delegated
     * access to Client (Consumer)
     * Implementations can assume the method being called on same {@code OAuth2InteractiveWorkflow}
     * instance which holds to workflow state
     * @param authorizationCode authorizationCode received from Authorization Server after auth grant
     * @param state optional workflow state interchanged between server and client
     */
    void resume(String authorizationCode, String state);

    /**
     * Get authorization uri set during configuration.
     * Note: Not to be confused with {@link #getAuthorizationRequestUri()} which is the request
     * uri generated to be used by clients after partial execution of the workflow by calling
     * {@link #execute()} method for interactive workflow
     * @return authorizationUri set during configuration
     */
    String getAuthorizationUri();

    /**
     * The request uri generated to be used by clients after partial execution of the workflow by calling
     * {@link #execute()} method for interactive workflow
     * @return request uri to be used by client for making authorization request
     */
    String getAuthorizationRequestUri();

    /**
     * Authorization properties currently set on workflow instance
     * @return authorization properties
     */
    Map<String, String> getAuthorizationProperties();

    /**
     * Builder for {@code OAuth2InteractiveWorkflow}. Supports parameterization so that
     * the implementations can return custom types after {@code build()}
     * @param <T> The implementation of the Builder interface
     * @see AuthorizationCodeFlow.Builder
     */
    interface Builder<T extends Builder<T>>
            extends OAuth2Workflow.Builder<T> {

        /**
         * Set the URI to which the user should be redirected to authorize our application. The URI points to the
         * authorization server and is defined by the Service Provider.
         *
         * @param authorizationUri Authorization URI.
         * @return Builder instance.
         */
        T authorizationUri(String authorizationUri);

        OAuth2InteractiveWorkflow build();
    }
}
