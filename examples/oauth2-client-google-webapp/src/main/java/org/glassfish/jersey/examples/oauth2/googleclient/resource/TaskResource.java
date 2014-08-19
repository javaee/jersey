/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.oauth2.googleclient.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
import org.glassfish.jersey.examples.oauth2.googleclient.SimpleOAuthService;
import org.glassfish.jersey.examples.oauth2.googleclient.entity.TaskBean;
import org.glassfish.jersey.examples.oauth2.googleclient.entity.TaskListBean;
import org.glassfish.jersey.examples.oauth2.googleclient.entity.TaskRootBean;
import org.glassfish.jersey.examples.oauth2.googleclient.model.AllTaskListsModel;
import org.glassfish.jersey.examples.oauth2.googleclient.model.TaskListModel;
import org.glassfish.jersey.examples.oauth2.googleclient.model.TaskModel;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.mvc.Template;

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
    @Template(name = "/tasks.mustache")
    @Produces("text/html")
    public Response getTasks() {
        if (SimpleOAuthService.getAccessToken() == null) {
            final String redirectURI = UriBuilder.fromUri(uriInfo.getBaseUri())
                    .path("oauth2/authorize").build().toString();

            final OAuth2CodeGrantFlow flow = OAuth2ClientSupport.googleFlowBuilder(
                    SimpleOAuthService.getClientIdentifier(),
                    redirectURI,
                    "https://www.googleapis.com/auth/tasks.readonly")
                    .prompt(OAuth2FlowGoogleBuilder.Prompt.CONSENT).build();

            SimpleOAuthService.setFlow(flow);

            // start the flow
            final String googleAuthURI = flow.start();

            // redirect user to Google Authorization URI.
            return Response.seeOther(UriBuilder.fromUri(googleAuthURI).build()).build();
        }
        // We have already an access token. Query the data from Google API.
        final Client client = SimpleOAuthService.getFlow().getAuthorizedClient();
        final AllTaskListsModel allTaskListsModel = getTasks(client);
        return Response.ok(allTaskListsModel).type(MediaType.TEXT_HTML_TYPE).build();
    }


    /**
     * Queries task data from google.
     * @param client Client configured for authentication with access token.
     * @return String html Google task data.
     */
    private static AllTaskListsModel getTasks(final Client client) {
        client.register(JacksonFeature.class);
        final WebTarget baseTarget = client.target(GOOGLE_TASKS_BASE_URI);
        final Response response = baseTarget.path("users/@me/lists").request().get();

        final TaskRootBean taskRootBean = response.readEntity(TaskRootBean.class);

        final List<TaskListModel> listOfTaskLists = new ArrayList<TaskListModel>();
        for (final TaskListBean taskListBean : taskRootBean.getItems()) {
            final List<TaskModel> taskList = new ArrayList<TaskModel>();
            final WebTarget listTarget = baseTarget.path("lists/{tasklist}/tasks")
                    .resolveTemplate("tasklist", taskListBean.getId());

            final TaskListBean fullTaskListBean = listTarget.request().get(TaskListBean.class);
            for (final TaskBean taskBean : fullTaskListBean.getTasks()) {
                taskList.add(new TaskModel(taskBean.getTitle()));
            }
            final TaskListModel listModel = new TaskListModel(taskListBean == null ? "No tasks were found. Define some tasks."
                    : taskListBean.getTitle(), taskList);
            listOfTaskLists.add(listModel);

        }
        return new AllTaskListsModel(listOfTaskLists);
    }
}