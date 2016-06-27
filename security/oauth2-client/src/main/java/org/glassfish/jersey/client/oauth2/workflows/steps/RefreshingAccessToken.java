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

package org.glassfish.jersey.client.oauth2.workflows.steps;

import org.glassfish.jersey.client.oauth2.OAuth2Parameters;
import org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow;
import org.glassfish.jersey.client.oauth2.TokenResult;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import org.glassfish.jersey.client.oauth2.internal.LocalizationMessages;

/**
 * In case the access token becomes invalid due to expiration or some other reason
 * this step is initialized by {@code TokenInvalid} step to refresh the token
 *
 * @author Deepak Pol on 3/11/16.
 */
public class RefreshingAccessToken implements OAuth2WorkflowStep {

    private OAuth2Workflow workflowContext;

    public RefreshingAccessToken(OAuth2Workflow workflowContext) {
        this.workflowContext = workflowContext;
    }

    /**
     * Uses refresh_token available after initial OAuth2 successful handshake
     * for getting a fresh access_token. If successful, sets the {@code OAuth2Workflow}
     * to step {@code AccessTokenAvailable}
     */
    @Override
    public void execute() {

        Map<String, String> refreshTokenProperties = workflowContext.getRefreshTokenProperties();
        // This could be added to context but will be hard to keep it in sync in initial vs intermediate
        // states, rather just pick latest
        String refreshToken = workflowContext.getTokenResult().getRefreshToken();

        if (refreshToken == null){
            throw new IllegalStateException(LocalizationMessages.ERROR_FLOW_REQUEST_REFRESH_TOKEN_MISSING());
        }

        refreshTokenProperties.put(OAuth2Parameters.REFRESH_TOKEN, refreshToken);
        final Form form = new Form();
        for (final Map.Entry<String, String> entry : refreshTokenProperties.entrySet()) {
            form.param(entry.getKey(), entry.getValue());
        }

        final Response response =
                workflowContext.getClient()
                .target(workflowContext.getRefreshTokenUri())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        if (response.getStatus() != 200) {
            throw new ProcessingException(LocalizationMessages.ERROR_FLOW_REQUEST_REFRESH_TOKEN(response.getStatus()));
        }

        TokenResult tokenResult = response.readEntity(TokenResult.class);
        workflowContext.setState(new AccessTokenAvailable(workflowContext, tokenResult));
    }
}
