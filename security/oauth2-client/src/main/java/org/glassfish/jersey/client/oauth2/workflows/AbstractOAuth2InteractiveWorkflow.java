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

import org.glassfish.jersey.client.oauth2.ClientIdentifier;

import javax.ws.rs.client.Client;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by deepakpol on 6/24/16.
 */
public abstract class AbstractOAuth2InteractiveWorkflow extends AbstractOAuth2Workflow
        implements OAuth2InteractiveWorkflow {

    protected Map<String, String> authorizationProperties;
    protected String authorizationUri;

    private AbstractOAuth2InteractiveWorkflow(Client client,
                                              ClientIdentifier clientIdentifier,
                                              String accessTokenUri,
                                              String refreshTokenUri){
        super(client, clientIdentifier, accessTokenUri, refreshTokenUri);
    }

    protected AbstractOAuth2InteractiveWorkflow(
            Client client,
            ClientIdentifier clientIdentifier,
            String authorizationUri,
            String accessTokenUri,
            String refreshTokenUri,
            String redirectUri,
            String scope,
            Map<String, String> authorizationProperties,
            Map<String, String> accessTokenProperties,
            Map<String, String> refreshTokenProperties) {
        this(client, clientIdentifier, accessTokenUri, refreshTokenUri);

        this.authorizationUri = authorizationUri;
        this.authorizationProperties = authorizationProperties;
        this.accessTokenProperties = accessTokenProperties;
        this.refreshTokenProperties = refreshTokenProperties;

        initDefaultProperties(redirectUri, scope);
        configureClient();

    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public Map<String, String> getAuthorizationProperties() {
        return authorizationProperties;
    }

    /**
     * Abstract implementation for {@link org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow.Builder}
     * @param <T> to support extention by specific implementations
     */
    public abstract static class Builder<T extends Builder<T>> extends AbstractOAuth2Workflow.Builder<T>
            implements OAuth2InteractiveWorkflow.Builder<T> {
        protected String authorizationUri;
        protected Map<String, String> authorizationProperties = new HashMap<>();

        /**
         * Set authorization URI to be used for workflow
         * @param authorizationUri Authorization URI.
         * @return current builder instance
         */
        @Override
        public T authorizationUri(final String authorizationUri) {
            this.authorizationUri = authorizationUri;
            return self();
        }

        /**
         * Allow clients to pass and override {@code authorizationProperties, accessTokenProperties, refreshTokenProperties}
         * while initializing.
         * @param phase which {@link org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow.Phase} the property is applicable to
         * @param key Property key.
         * @param value Property value.
         * @return self instance
         */
        @Override
        public T property(final Phase phase, String key, String value) {
            phase.property(key, value, authorizationProperties,
                    accessTokenProperties, refreshTokenProperties);
            return self();
        }

    }
}
