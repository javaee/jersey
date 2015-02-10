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

package org.glassfish.jersey.client.oauth2;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * OAuth2 client filter feature registers the support for performing authenticated requests to the
 * Service Provider. The feature does not perform Authorization Flow (see {@link OAuth2CodeGrantFlow}
 * for details how to use Authorization Flow and retrieve Access Token). The feature uses access to initialize
 * the internal {@link javax.ws.rs.container.ContainerRequestFilter filter}
 * which will add {@code Authorization} http header containing OAuth 2 authorization information including (based
 * on {@code bearer} tokens).
 * <p>
 * The internal filter can be controlled by properties put into
 * the {@link javax.ws.rs.client.ClientRequestContext client request}
 * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} method. The property key
 * is defined in this class as a static variables
 * ({@link OAuth2ClientSupport#OAUTH2_PROPERTY_ACCESS_TOKEN} (see its javadoc for usage).
 * Using the property a specific
 * access token can be defined for each request.
 * </p>
 * Example of using specific access token for one request:
 * <pre>
 * final Response response = client.target("foo").request()
 *           .property(OAUTH2_PROPERTY_ACCESS_TOKEN, "6ab45ab465e46f54d771a").get();
 * </pre>
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
class OAuth2ClientFeature implements Feature {

    private final OAuth2ClientFilter filter;

    /**
     * Create a new feature initialized for the access token.
     *
     * @param accessToken Access token.
     */
    public OAuth2ClientFeature(String accessToken) {
        this.filter = new OAuth2ClientFilter(accessToken);
    }

    /**
     * Create a new filter feature with no default access token. The token will have to be
     * specified by {@link OAuth2ClientSupport#OAUTH2_PROPERTY_ACCESS_TOKEN}
     * for each request otherwise no {@code Authorization}
     * http header will be added.
     */
    public OAuth2ClientFeature() {
        this.filter = new OAuth2ClientFilter();
    }

    /**
     * Create a new feature with the given {@code filter}.
     *
     * @param filter Filter instance.
     */
    OAuth2ClientFeature(OAuth2ClientFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(filter);
        return true;
    }
}
