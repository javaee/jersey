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

package org.glassfish.jersey.server.oauth1;

/**
 * Properties used to configure OAuth server module.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public final class OAuth1ServerProperties {
    /**
     * OAuth realm (String property).
     *
     * <p>
     * A default value is {@code 'default'}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     **/
    public static final String REALM = "jersey.config.server.oauth1.realm";


    /**
     * Property that can be set to a regular expression used to match the path (relative to the base URI) this
     * filter should not be applied to.
     * <p>
     * A default value is {@code null}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     **/
    public static final String IGNORE_PATH_PATTERN = "jersey.config.server.oauth1.ignorePathPattern";

    /**
     * Property defines maximum age (in milliseconds) of nonces that should be tracked (default = 300000 ms = 5 min).
     *
     * <p>
     * A default value is {@code 300000} which corresponds to 5 minutes.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     **/
    public static final String MAX_AGE = "jersey.config.server.oauth1.maxAge";


    /**
     * Property that can be set to frequency of collecting nonces exceeding max. age (default = 100 = every 100 requests).
     *
     * */
    public static final String GC_PERIOD = "jersey.config.server.oauth1.gcPeriod";


    /**
     * If set to {@code true} makes the correct OAuth authentication optional.
     * Instead of returning the appropriate status code
     * ({@link javax.ws.rs.core.Response.Status#BAD_REQUEST}
     * or {@link javax.ws.rs.core.Response.Status#UNAUTHORIZED}) the {@link OAuth1ServerFilter OAuth filter}
     * will ignore this request (as if it was not authenticated) and let the web application deal with it.
     *
     * <p>
     * A default value is {@code false}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * */
    public static final String NO_FAIL = "jersey.config.server.oauth1.noFail";

    /**
     * If set to {@code true}, token resources will be exposed. Token resources are JAX-RS resources
     * for retrieving Request Tokens and Access Tokens. If the property is set to {@code false},
     * the resources will not be exposed and it is responsibility of the user custom resource to issue
     * Request Tokens and Access Tokens.
     * <p>
     * URIs of exposed resources can be specified by {@link #REQUEST_TOKEN_URI} and {@link #ACCESS_TOKEN_URI}.
     * </p>
     *
     * <p>
     * A default value is {@code true}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String ENABLE_TOKEN_RESOURCES = "jersey.config.server.oauth1.resource.enabled";

    /**
     * If exposure of token resources is enabled (e.g. by the property {@link #ENABLE_TOKEN_RESOURCES}),
     * this property defines the relative URI of exposed Request Token Resource. The URI must be relative
     * to the base URI of the JAX-RS application.
     * <p/>
     * Request Token resource is the resource which issues a Request Token and Request Token secret
     * during the Authorization process (temporary credentials IDs).
     *
     * <p>
     * The property is undefined by default.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String REQUEST_TOKEN_URI = "jersey.config.server.oauth1.resource.requestToken.uri";

    /**
     * If exposure of token resources is enabled (e.g. by the property {@link #ENABLE_TOKEN_RESOURCES}),
     * this property defines the relative URI of exposed Access Token Resource. The URI must be relative
     * to the base URI of the JAX-RS application.
     * <p/>
     * Access Token resource is the resource which issues a Access Token and Access Token secret
     * during the Authorization process.
     *
     * <p>
     * The property is undefined by default.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String ACCESS_TOKEN_URI = "jersey.config.server.oauth1.resource.accessToken.uri";
}
