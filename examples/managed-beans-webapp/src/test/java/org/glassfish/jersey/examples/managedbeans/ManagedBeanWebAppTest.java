/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.managedbeans;

import java.net.URI;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.examples.managedbeans.resources.MyApplication;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Main test for the Managed Beans web application.
 * The application must be deployed and running on a standalone GlassFish container.
 * To run the tests then, you just launch the following command:
 * <pre>
 * mvn -DskipTests=false test</pre>
 *
 * @author Naresh Srinivas Bhimisetty
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ManagedBeanWebAppTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("managed-beans-webapp").path("app").build();
    }

    /**
     * Test that provided query parameter makes it back.
     */
    @Test
    public void testPerRequestResource() {
        WebTarget perRequest = target().path("managedbean/per-request");

        String responseMsg = perRequest.queryParam("x", "X").request().get(String.class);
        assertThat(responseMsg, containsString("X"));
        assertThat(responseMsg, startsWith("INTERCEPTED"));

        responseMsg = perRequest.queryParam("x", "hi there").request().get(String.class);
        assertThat(responseMsg, containsString("hi there"));
        assertThat(responseMsg, startsWith("INTERCEPTED"));
    }

    /**
     * Test that singleton counter gets incremented with each call and can be reset.
     */
    @Test
    public void testSingletonResource() {
        WebTarget singleton = target().path("managedbean/singleton");

        String responseMsg = singleton.request().get(String.class);
        assertThat(responseMsg, containsString("3"));

        responseMsg = singleton.request().get(String.class);
        assertThat(responseMsg, containsString("4"));

        singleton.request().put(Entity.text("1"));

        responseMsg = singleton.request().get(String.class);
        assertThat(responseMsg, containsString("1"));

        responseMsg = singleton.request().get(String.class);
        assertThat(responseMsg, containsString("2"));
    }

    /**
     * Test the JPA backend.
     */
    @Test
    public void testWidget() {
        WebTarget target = target().path("managedbean/singleton/widget");

        final WebTarget widget = target.path("1");

        assertThat(widget.request().get().getStatus(), is(404));

        widget.request().put(Entity.text("One"));
        assertThat(widget.request().get(String.class), is("One"));

        widget.request().put(Entity.text("Two"));
        assertThat(widget.request().get(String.class), is("Two"));

        assertThat(widget.request().delete().getStatus(), is(204));

        assertThat(widget.request().get().getStatus(), is(404));
    }

    /**
     * Test exceptions are properly mapped.
     */
    @Test
    public void testExceptionMapper() {

        WebTarget singletonTarget = target().path("managedbean/singleton/exception");
        WebTarget perRequestTarget = target().path("managedbean/per-request/exception");

        _testExceptionOutput(singletonTarget, "singleton");
        _testExceptionOutput(perRequestTarget, "per-request");
    }

    /**
     * Test a non empty WADL is generated.
     */
    @Test
    public void testApplicationWadl() {
        WebTarget wadl = target().path("application.wadl");
        String wadlDoc = wadl.request(MediaTypes.WADL_TYPE).get(String.class);

        assertThat(wadlDoc.length(), is(not(0)));
    }


    private void _testExceptionOutput(WebTarget exceptionTarget, String thatShouldBePresentInResponseBody) {

        Response exceptionResponse = exceptionTarget.request().get();
        assertThat(exceptionResponse.getStatus(), is(500));

        final String responseBody = exceptionResponse.readEntity(String.class);

        assertThat(responseBody, containsString("ManagedBeanException"));
        assertThat(responseBody, containsString(thatShouldBePresentInResponseBody));
    }
}
