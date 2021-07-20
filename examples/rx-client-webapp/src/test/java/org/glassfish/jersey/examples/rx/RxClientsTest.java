/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.rx;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.examples.rx.domain.AgentResponse;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Invoke clients in Agent part of the application.
 *
 * @author Michal Gajdos
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class RxClientsTest extends JerseyTest {

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.builder(RxApplication.class)
                .contextPath("rx-client-webapp/rx").build();
    }

    @Test
    public void testSyncClient() throws Exception {
        // warmup
        target("agent").path("sync").request().get();

        final Response response = target("agent").path("sync").request().get();
        response.bufferEntity();

        final AgentResponse agentResponse = response.readEntity(AgentResponse.class);

        assertThat(agentResponse.getVisited().size(), is(5));
        assertThat(agentResponse.getRecommended().size(), is(5));

        assertThat(agentResponse.getProcessingTime() > 4500, is(true));

        System.out.println(response.readEntity(String.class));
        System.out.println("Processing Time: " + agentResponse.getProcessingTime());
    }

    @Test
    public void testAsyncClient() throws Exception {
        // warmup
        target("agent").path("async").request().get();

        final Response response = target("agent").path("async").request().get();
        response.bufferEntity();

        final AgentResponse agentResponse = response.readEntity(AgentResponse.class);

        assertThat(agentResponse.getVisited().size(), is(5));
        assertThat(agentResponse.getRecommended().size(), is(5));

        assertThat(agentResponse.getProcessingTime() > 850, is(true));
        assertThat(agentResponse.getProcessingTime() < 4500, is(true));

        System.out.println(response.readEntity(String.class));
        System.out.println("Processing Time: " + agentResponse.getProcessingTime());
    }

    @Test
    public void testRxObservableClient() throws Exception {
        // warmup
        target("agent").path("observable").request().get();

        final Response response = target("agent").path("observable").request().get();
        response.bufferEntity();

        final AgentResponse agentResponse = response.readEntity(AgentResponse.class);

        assertThat(agentResponse.getVisited().size(), is(5));
        assertThat(agentResponse.getRecommended().size(), is(5));

        assertThat(agentResponse.getProcessingTime() > 850, is(true));
        assertThat(agentResponse.getProcessingTime() < 4500, is(true));

        System.out.println(response.readEntity(String.class));
        System.out.println("Processing Time: " + agentResponse.getProcessingTime());
    }

    @Test
    public void testRxFlowableClient() throws Exception {
        // warmup
        target("agent").path("flowable").request().get();

        final Response response = target("agent").path("flowable").request().get();
        response.bufferEntity();

        final AgentResponse agentResponse = response.readEntity(AgentResponse.class);

        assertThat(agentResponse.getVisited().size(), is(5));
        assertThat(agentResponse.getRecommended().size(), is(5));

        assertThat(agentResponse.getProcessingTime() > 850, is(true));
        assertThat(agentResponse.getProcessingTime() < 4500, is(true));

        System.out.println(response.readEntity(String.class));
        System.out.println("Processing Time: " + agentResponse.getProcessingTime());
    }

    @Test
    public void testRxCompletionStageClient() throws Exception {
        // warmup
        target("agent").path("completion").request().get();

        final Response response = target("agent").path("completion").request().get();
        response.bufferEntity();

        final AgentResponse agentResponse = response.readEntity(AgentResponse.class);

        assertThat(agentResponse.getVisited().size(), is(5));
        assertThat(agentResponse.getRecommended().size(), is(5));

        assertThat(agentResponse.getProcessingTime() > 850, is(true));
        assertThat(agentResponse.getProcessingTime() < 4500, is(true));

        System.out.println(response.readEntity(String.class));
        System.out.println("Processing Time: " + agentResponse.getProcessingTime());
    }
}
