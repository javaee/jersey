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

package org.glassfish.jersey.examples.oauth2.googleclient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.glassfish.jersey.client.oauth2.OAuth2CodeGrantFlow;
import org.glassfish.jersey.client.oauth2.OAuth2FlowGoogleBuilder;
import org.glassfish.jersey.client.oauth2.TokenResult;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Task resource that returns Google tasks that was queried using a {@link Client}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@Path("tasks")
public class TaskResource {
    private static final String GOOGLE_TASKS_BASE_URI = "https://www.googleapis.com/tasks/v1/";
    @Context
    private UriInfo uriInfo;

    @GET
    public Response getTasks() {
        final TokenResult tokenResult = CredentialStore.getTokenResult();
        if (tokenResult == null) {
            // we do not have access token yet. We need to perform authorization flow.

            // build the redirect URI (the same URI must be registered in Google API console).
            final String redirectURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path("oauth2/authorize")
                    .build().toString();

            // create a new Google Authorization Flow.
            final OAuth2CodeGrantFlow flow = OAuth2ClientSupport.googleFlowBuilder(
                    CredentialStore.getClientId(),
                    redirectURI,
                    "https://www.googleapis.com/auth/tasks.readonly")
                    .prompt(OAuth2FlowGoogleBuilder.Prompt.CONSENT).build();

            // cache this flow -> it will be used later on after user will be redirected back to redirectURI
            CredentialStore.cachedFlow = flow;
            final String uri = flow.start();

            // redirect user to Google Authorization URI.
            return Response.seeOther(UriBuilder.fromUri(uri).build()).build();
        }

        // We have already an access token. Query the data from Google.
        final Client client = CredentialStore.cachedFlow.getAuthorizedClient();
        String message = "These tasks were queried using a Jersey client and access " +
                "token received from the authorization flow:<br/>\n";
        return Response.ok(message + getTasksAsString(client)).type(MediaType.TEXT_HTML_TYPE).build();
    }


    /**
     * Queries task data from google.
     * @param client Client configured for authentication with access token.
     * @return String html Google task data.
     */
    private static String getTasksAsString(Client client) {
        client.register(JacksonFeature.class);
        final WebTarget baseTarget = client.target(GOOGLE_TASKS_BASE_URI);
        final Response response = baseTarget.path("users/@me/lists").request().get();

        StringBuilder sb = new StringBuilder();
        final TaskRoot taskRoot = response.readEntity(TaskRoot.class);
        for (TaskList taskList : taskRoot.getItems()) {
            sb.append("<b>" + taskList.getTitle() + "</b><br/>");
            final WebTarget listTarget = baseTarget.path("lists/{tasklist}/tasks")
                    .resolveTemplate("tasklist", taskList.getId());

            final TaskList fullTaskList = listTarget.request().get(TaskList.class);
            for (Task task : fullTaskList.getTasks()) {
                sb.append("   - ").append(task.getTitle()).append("<br/>");
            }
            sb.append("<br/>");
        }
        return sb.toString();
    }
}