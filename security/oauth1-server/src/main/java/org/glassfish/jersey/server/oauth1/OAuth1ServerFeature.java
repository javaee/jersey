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

package org.glassfish.jersey.server.oauth1;

import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.oauth1.signature.OAuth1SignatureFeature;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.oauth1.internal.AccessTokenResource;
import org.glassfish.jersey.server.oauth1.internal.RequestTokenResource;

/**
 * The feature enables support for OAuth 1.0a on the server.
 * <p/>
 * The OAuth 1 server support requires implementation of {@link OAuth1Provider};
 * which will be used to retrieve Request Tokens, Access tokens, etc. The implementation should be configured
 * in this feature or registered as a standard provider.
 * <p/>
 * Feature can be created and configured by various constructors. Beside that, the feature behaviour
 * can be overwritten by configuration properties {@link OAuth1ServerProperties#ENABLE_TOKEN_RESOURCES},
 * {@link OAuth1ServerProperties#REQUEST_TOKEN_URI} and {@link OAuth1ServerProperties#ACCESS_TOKEN_URI}.
 *
 * @author Miroslav Fuksa
 */
public class OAuth1ServerFeature implements Feature {

    private final OAuth1Provider oAuth1Provider;
    private final String requestTokenUri;
    private final String accessTokenUri;

    /**
     * Create a new feature configured with {@link OAuth1Provider OAuth provider} and request and access token
     * URIs. The feature also exposes Request and Access Token Resources.
     * These resources are part of the Authorization process and
     * grant Request and Access tokens. Resources will be available on
     * URIs defined by parameters {@code requestTokenUri} and {@code accessTokenUri}.
     *
     * @param oAuth1Provider Instance of the {@code OAuth1Provider} that will handle authorization. If the value is
     *                       {@code null}, then the provider must be registered explicitly outside of this feature
     *                       as a standard provider.
     * @param requestTokenUri URI (relative to application context path) of Request Token Resource that will be exposed.
     * @param accessTokenUri URI (relative to application context path) of Request Token Resource that will be exposed.
     */
    public OAuth1ServerFeature(OAuth1Provider oAuth1Provider,
                               String requestTokenUri,
                               String accessTokenUri) {
        this.oAuth1Provider = oAuth1Provider;
        this.requestTokenUri = requestTokenUri;
        this.accessTokenUri = accessTokenUri;
    }

    /**
     * Create a new feature configured with {@link OAuth1Provider OAuth provider}. The feature will not
     * expose Request and Access Token Resources. The OAuth 1 support will not be responsible for handling
     * these authorization request types.
     *
     * @param oAuth1Provider Instance of the {@code OAuth1Provider} that will handle authorization.
     */
    public OAuth1ServerFeature(OAuth1Provider oAuth1Provider) {
        this(oAuth1Provider, null, null);
    }

    /**
     * Create a new feature. The feature will not register any {@link OAuth1Provider OAuth provider}
     * and it will not expose Request and Access Token Resources. {@code OAuth1Provider} must be registered
     * explicitly as a standard provider. As Token Resources are not exposed, the OAuth 1 support will
     * not be responsible for handling Token Requests.
     */
    public OAuth1ServerFeature() {
        this(null);
    }

    @Override
    public boolean configure(FeatureContext context) {
        if (oAuth1Provider != null) {
            context.register(oAuth1Provider);
        }

        context.register(OAuth1ServerFilter.class);

        if (!context.getConfiguration().isRegistered(OAuth1SignatureFeature.class)) {
            context.register(OAuth1SignatureFeature.class);
        }

        final Map<String, Object> properties = context.getConfiguration().getProperties();
        final Boolean propertyResourceEnabled = OAuth1ServerProperties.getValue(properties,
                OAuth1ServerProperties.ENABLE_TOKEN_RESOURCES, null, Boolean.class);

        boolean registerResources = propertyResourceEnabled != null
                ? propertyResourceEnabled : requestTokenUri != null & accessTokenUri != null;

        if (registerResources) {
            String requestUri = OAuth1ServerProperties.getValue(properties, OAuth1ServerProperties.REQUEST_TOKEN_URI,
                    null, String.class);
            if (requestUri == null) {
                requestUri = requestTokenUri == null ? "requestToken" : requestTokenUri;
            }

            String accessUri = OAuth1ServerProperties.getValue(properties, OAuth1ServerProperties.ACCESS_TOKEN_URI,
                    null, String.class);
            if (accessUri == null) {
                accessUri = accessTokenUri == null ? "accessToken" : accessTokenUri;
            }

            final Resource requestResource = Resource.builder(RequestTokenResource.class).path(requestUri).build();
            final Resource accessResource = Resource.builder(AccessTokenResource.class).path(accessUri).build();

            context.register(new OAuthModelProcessor(requestResource, accessResource));
        }
        return true;
    }


    @Priority(100)
    private static class OAuthModelProcessor implements ModelProcessor {
        private final Resource[] resources;

        private OAuthModelProcessor(Resource... resources) {
            this.resources = resources;
        }

        @Override
        public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
            final ResourceModel.Builder builder = new ResourceModel.Builder(resourceModel, false);
            for (Resource resource : resources) {
                builder.addResource(resource);
            }

            return builder.build();
        }

        @Override
        public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
            return subResourceModel;
        }
    }
}
