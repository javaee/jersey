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

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Feature;

/**
 * The interface of the OAuth 1 Authorization Flow utility.
 * <p>
 * The implementation of this interface is capable of performing of the user
 * authorization defined in the OAuth1 specification. The result of the authorization
 * is the {@link AccessToken access token}. The user authorization is called also
 * Authorization Flow. The implementation initiates the authorization process with
 * the Authorization server, then provides redirect URI to which the user should
 * be redirected (the URI points to authorization consent page hosted by Service Provider). The user
 * grants an access using this page. Service Provider redirects the user back to the
 * our server and the authorization process is finished using the implementation.
 * </p>
 * <p>
 * To perform the authorization follow these steps:
 * <list>
 * <li>Get the instance of this interface using {@link OAuth1ClientSupport}.</li>
 * <li>Call {@link #start()} method. The method returns redirection uri as a String. Note: the method internally
 * makes a request to the request token uri and gets Request Token which will be used for the authorization process.</li>
 * <li>Redirect user to the redirect uri returned from the {@code start} method. If your application deployment
 * does not allow redirection (for example the app is a console application), then provide the redirection URI
 * to the user in other ways.</li>
 * <li>User should authorize your application on the redirect URI.</li>
 * <li>After authorization the Authorization Server redirects the user back to the URI specified
 * by {@link OAuth1Builder.FlowBuilder#callbackUri(String)} and provide the {@code oauth_verifier} as
 * a request query parameter. Extract this parameter from the request. If your deployment does not support
 * redirection (your app is not a web server) then Authorization Server will provide the user with
 * {@code verifier} in other ways (for example display on the html page). You need to get
 * this verifier from the user.</li>
 * <li>Use the {@code verifier} to finish the authorization process by calling the method
 * {@link #finish(String)} supplying the verifier. The method will internally request
 * the access token from the Authorization Server and return it.</li>
 * <li>You can use {@code AccessToken} together with {@link ConsumerCredentials} to
 * perform the authenticated requests to the Service Provider. You can also call
 * methods {@link #getAuthorizedClient()} to get {@link Client client} already configured with support
 * for authentication from consumer credentials and access token received during authorization process.
 * </li>
 * </list>
 * </p>
 * <p>
 * Important note: one instance of the interface can be used only for one authorization process. The methods
 * must be called exactly in the order specified by the list above. Therefore the instance is also not
 * thread safe and no concurrent access is expected.
 * </p>
 * Instance must be stored between method calls (between {@code start} and {@code finish})
 * for one user authorization process as the instance keeps
 * internal state of the authorization process.
 * </p>
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public interface OAuth1AuthorizationFlow {

    /**
     * Start the authorization process and return redirection URI on which the user should give a consent
     * for our application to access resources.
     * <p>
     * Note: the method makes a request to the Authorization Server in order to get request token.
     * </p>
     *
     * @return URI to which user should be redirected.
     */
    public String start();

    /**
     * Finish the authorization process and return the {@link AccessToken}. The method must be called on the
     * same instance after the {@link #start()} method was called and user granted access to this application.
     * <p>
     * The method makes a request to the Authorization Server but does not exchange verifier for access token. This method is
     * intended only for some flows/cases in OAuth1.
     * </p>
     *
     * @return Access token.
     * @since 2.7
     */
    public AccessToken finish();

    /**
     * Finish the authorization process and return the {@link AccessToken}. The method must be called on the
     * same instance after the {@link #start()} method was called and user granted access to this application.
     * <p>
     * The method makes a request to the Authorization Server in order to exchange verifier for access token.
     * </p>
     *
     * @param verifier Verifier provided from the user authorization.
     * @return Access token.
     */
    public AccessToken finish(String verifier);

    /**
     * Return the client configured for performing authorized requests to the Service Provider. The
     * authorization process must be successfully finished by instance by calling methods {@link #start()} and
     * {@link #finish(String)}.
     *
     * @return Client configured to add correct {@code Authorization} header to requests.
     */
    public Client getAuthorizedClient();

    /**
     * Return the {@link javax.ws.rs.core.Feature oauth filter feature} that can be used to configure
     * {@link Client client} instances to perform authenticated requests to the Service Provider.
     * <p>
     * The authorization process must be successfully finished by instance by calling methods {@link #start()} and
     * {@link #finish(String)}.
     * </p>
     *
     * @return oauth filter feature configured with received {@code AccessToken}.
     */
    public Feature getOAuth1Feature();
}
