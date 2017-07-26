/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.servlet.ServletContext;

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
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.mvc.Template;

/**
 * Task resource that returns Google tasks that was queried using a {@link Client}.
 *
 * @author Miroslav Fuksa
 */
@Path("tasks")
public class TaskResource {

    private static final String GOOGLE_TASKS_BASE_URI = "https://www.googleapis.com/tasks/v1/";

    @Context
    private UriInfo uriInfo;
    @Context
    private ServletContext servletContext;

    @GET
    @Template(name = "/tasks.mustache")
    @Produces("text/html")
    public Response getTasks() {
        // check oauth setup
        if (SimpleOAuthService.getClientIdentifier() == null) {
            final URI uri = UriBuilder.fromUri(servletContext.getContextPath())
                    .path("/index.html") //to show "Enter your Client Id and Secret" setup page
                    .build();
            return Response.seeOther(uri).build();
        }
        // check access token
        if (SimpleOAuthService.getAccessToken() == null) {
            return googleAuthRedirect();
        }
        // We have already an access token. Query the data from Google API.
        final Client client = SimpleOAuthService.getFlow().getAuthorizedClient();
        return getTasksResponse(client);
    }

    /**
     * Prepare redirect response to Google Tasks API auth consent request.
     *
     * @return redirect response to Google Tasks API auth consent request
     */
    private Response googleAuthRedirect() {
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

    /**
     * Queries task data from google.
     *
     * @param client Client configured for authentication with access token.
     * @return Google task data response or redirect to google authorize page response.
     */
    private Response getTasksResponse(final Client client) {
        client.register(JacksonFeature.class);
        client.register(new LoggingFeature(Logger.getLogger("example.client.tasks"), LoggingFeature.Verbosity.PAYLOAD_ANY));

        final WebTarget baseTarget = client.target(GOOGLE_TASKS_BASE_URI);
        final Response response = baseTarget.path("users/@me/lists").request().get();

        final List<TaskListModel> listOfTaskLists;
        switch (response.getStatus()) {
            case 401: //Response.Status.UNAUTHORIZED
                SimpleOAuthService.setAccessToken(null);
                return googleAuthRedirect();
            case 200: //Response.Status.OK
                listOfTaskLists = processTaskLists(baseTarget, response.readEntity(TaskRootBean.class));
                break;
            default:
                listOfTaskLists = null;
        }

        final AllTaskListsModel tasks = new AllTaskListsModel(listOfTaskLists);
        return Response.ok(tasks).build();
    }

    /**
     * Process users task lists and read task details. Collect just
     * @param baseTarget base JAX-RS client target with oauth context configured
     * @param taskRootBean root task bean to be processed
     * @return Detailed list of non-completed tasks or {@code null} if there is no task list available.
     */
    private List<TaskListModel> processTaskLists(final WebTarget baseTarget, final TaskRootBean taskRootBean) {
        final List<TaskListModel> listOfTaskLists = new ArrayList<>();
        for (final TaskListBean taskListBean : taskRootBean.getItems()) {
            final List<TaskModel> taskList = new ArrayList<>();
            final WebTarget listTarget = baseTarget.path("lists/{tasklist}/tasks")
                    .resolveTemplate("tasklist", taskListBean.getId());

            final TaskListBean fullTaskListBean = listTarget.request().get(TaskListBean.class);
            for (final TaskBean taskBean : fullTaskListBean.getTasks()) {
                if (taskBean.getCompleted() == null) {
                    taskList.add(new TaskModel(taskBean.getTitle()));
                }
            }
            listOfTaskLists.add(new TaskListModel(taskListBean.getTitle(), taskList.size() > 0 ? taskList : null));
        }
        return listOfTaskLists.size() > 0 ? listOfTaskLists : null;
    }

}
