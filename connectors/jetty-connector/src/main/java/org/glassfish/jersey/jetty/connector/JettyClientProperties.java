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
package org.glassfish.jersey.jetty.connector;

/**
 * Configuration options specific to the Client API that utilizes {@link JettyConnector}.
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class JettyClientProperties {

    /**
     * Support for specifying SSL configuration for HTTPS connections.
     * Used only when making HTTPS requests.
     *
     * <p>The value MUST be an instance of {@link org.glassfish.jersey.SslConfigurator}.</p>
     *
     * <p>A default value is not set.</p>
     *
     * <p>The name of the configuration property is <tt>{@value}</tt>.</p>
     */
    public static final String SSL_CONFIG =
            "jersey.config.jetty.client.ssl.sslConfig";

    /**
     * A value of {@code false} indicates the client should handle cookies
     * automatically using HttpClient's default cookie policy. A value
     * of {@code false} will cause the client to ignore all cookies.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * If the property is absent the default value is {@code false}
     */
    public static final String DISABLE_COOKIES =
            "jersey.config.jetty.client.disableCookies";

    /**
     * The credential provider that should be used to retrieve
     * credentials from a user.
     *
     * If an {@link org.eclipse.jetty.client.api.Authentication} mechanism is found,
     * it is then used for the given request, returning an {@link org.eclipse.jetty.client.api.Authentication.Result},
     * which is then stored in the {@link org.eclipse.jetty.client.api.AuthenticationStore}
     * so that subsequent requests can be preemptively authenticated.

     * <p/>
     * The value MUST be an instance of {@link
     * org.eclipse.jetty.client.util.BasicAuthentication}.  If
     * the property is absent a default provider will be used.
     */
    public static final String PREEMPTIVE_BASIC_AUTHENTICATION =
            "jersey.config.jetty.client.preemptiveBasicAuthentication";

}
