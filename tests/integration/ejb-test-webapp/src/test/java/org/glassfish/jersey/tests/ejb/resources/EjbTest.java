/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.ejb.resources;

import java.net.URI;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test for EJB web application resources.
 * Run with:
 * <pre>
 * mvn clean package
 * $AS_HOME/bin/asadmin deploy target/ejb-test-webapp
 * mvn -DskipTests=false test</pre>
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class EjbTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("ejb-test-webapp").build();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(LoggingFeature.class);
    }

    @Test
    public void testEjbException() {
        final Response jerseyResponse = target().path("rest/exception/ejb").request().get();
        _check500Response(jerseyResponse, ExceptionEjbResource.EjbExceptionMESSAGE);

        final Response servletResponse =
                target().path("servlet")
                  .queryParam("action", StandaloneServlet.ThrowEjbExceptionACTION).request().get();
        _check500Response(servletResponse, ExceptionEjbResource.EjbExceptionMESSAGE);
    }

    @Test
    public void testCheckedException() {
        final Response jerseyResponse = target().path("rest/exception/checked").request().get();
        _check500Response(jerseyResponse, ExceptionEjbResource.CheckedExceptionMESSAGE);

        final Response servletResponse =
                target().path("servlet")
                  .queryParam("action", StandaloneServlet.ThrowCheckedExceptionACTION).request().get();
        _check500Response(servletResponse, ExceptionEjbResource.CheckedExceptionMESSAGE);
    }

    @Test
    public void testCustomException1() {
        Response jerseyResponse = target().path("rest/exception/custom1/big").request().get();
        assertThat(jerseyResponse.getStatus(), is(200));
        assertThat(jerseyResponse.readEntity(String.class), is(EjbExceptionMapperOne.RESPONSE_BODY));
        assertThat(jerseyResponse.getHeaderString("My-Location"), is("exception/custom1/big"));
        assertThat(jerseyResponse.getHeaderString("My-Echo"), is("ECHOED: 1"));

        jerseyResponse = target().path("rest/exception/custom1/one").request().get();
        assertThat(jerseyResponse.getStatus(), is(200));
        assertThat(jerseyResponse.readEntity(String.class), is(EjbExceptionMapperOne.RESPONSE_BODY));
        assertThat(jerseyResponse.getHeaderString("My-Location"), is("exception/custom1/one"));
        assertThat(jerseyResponse.getHeaderString("My-Echo"), is("ECHOED: 1"));
    }

    @Test
    public void testCustomException2() {
        Response jerseyResponse = target().path("rest/exception/custom2/small").request().get();
        assertThat(jerseyResponse.getStatus(), is(200));
        assertThat(jerseyResponse.readEntity(String.class), is(EjbExceptionMapperTwo.RESPONSE_BODY));
        assertThat(jerseyResponse.getHeaderString("My-Location"), is("exception/custom2/small"));
        assertThat(jerseyResponse.getHeaderString("My-Echo"), is("ECHOED: 2"));

        jerseyResponse = target().path("rest/exception/custom2/one").request().get();
        assertThat(jerseyResponse.getStatus(), is(200));
        assertThat(jerseyResponse.readEntity(String.class), is(EjbExceptionMapperTwo.RESPONSE_BODY));
        assertThat(jerseyResponse.getHeaderString("My-Location"), is("exception/custom2/one"));
        assertThat(jerseyResponse.getHeaderString("My-Echo"), is("ECHOED: 2"));
    }

    @Test
    public void testRemoteLocalEJBInterface() {

        final String message = "Hi there";
        final Response response = target().path("rest/echo").queryParam("message", message).request().get();

        assertThat(response.getStatus(), is(200));

        final String responseMessage = response.readEntity(String.class);

        assertThat(responseMessage, startsWith(EchoBean.PREFIX));
        assertThat(responseMessage, endsWith(message));
    }

    @Test
    public void testRemoteAnnotationRegisteredEJBInterface() {

        final String message = "Hi there";
        final Response response = target().path("rest/raw-echo").queryParam("message", message).request().get();

        assertThat(response.getStatus(), is(200));

        final String responseMessage = response.readEntity(String.class);

        assertThat(responseMessage, startsWith(EchoBean.PREFIX));
        assertThat(responseMessage, endsWith(message));
    }

    @Test
    public void testRequestCountGetsIncremented() {

        final Response response1 = target().path("rest/echo").queryParam("message", "whatever").request().get();
        assertThat(response1.getStatus(), is(200));
        final String counterHeader1 = response1.getHeaderString(CounterFilter.RequestCountHEADER);
        final int requestCount1 = Integer.parseInt(counterHeader1);

        final Response response2 = target().path("rest/echo").queryParam("message", requestCount1).request().get();
        assertThat(response2.getStatus(), is(200));
        final int requestCount2 = Integer.parseInt(response2.getHeaderString(CounterFilter.RequestCountHEADER));

        assertThat(requestCount2, is(greaterThan(requestCount1)));
    }


    @Test
    public void testSync() {
        final Response response = target().path("rest/async-test/sync").request().get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is("sync"));
    }

    @Test
    public void testAsync() {
        final Response response = target().path("rest/async-test/async").request().get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is("async"));
    }

    @Test
    public void testAppIsEjbSingleton() {

        int c1 = target().path("rest/app/count").request().get(Integer.class);
        int c2 = target().path("rest/app/count").request().get(Integer.class);
        int c3 = target().path("rest/app/count").request().get(Integer.class);

        assertThat("the first count should be less than the second one", c1, is(lessThan(c2)));
        assertThat("the second count should be less than the third one", c2, is(lessThan(c3)));
    }

    private void _check500Response(final Response response, final String expectedSubstring) {
        assertThat(response.getStatus(), is(500));
        assertThat(response.readEntity(String.class), containsString(expectedSubstring));
    }
}