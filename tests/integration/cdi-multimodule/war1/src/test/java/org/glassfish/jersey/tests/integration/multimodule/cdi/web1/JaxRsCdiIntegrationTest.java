/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.multimodule.cdi.web1;

import java.net.URI;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for CDI web application resources. The JAX-RS resources use CDI components from a library jar.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class JaxRsCdiIntegrationTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new JaxRsApp();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("cdi-multimodule-war1").build();
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(LoggingFeature.class);
    }

    @Test
    public void testUriInfoInjectionReqScopedResourceDependentBean() {
        _testResource("request-scoped/dependent");
    }

    @Test
    public void testUriInfoInjectionReqScopedResourceRequestScopedBean() {
        _testResource("request-scoped/req");
    }

    @Test
    public void testUriInfoInjectionAppScopedResourceRequestScopedBean() {
        _testResource("app-scoped/req");
    }

    private void _testResource(String resourcePath) {
        _testUriInfo(resourcePath);
        _testHeader(resourcePath);
    }

    private void _testUriInfo(String resourcePath) {

        _testSinglePathUriUnfo(resourcePath, "one");
        _testSinglePathUriUnfo(resourcePath, "two");
        _testSinglePathUriUnfo(resourcePath, "three");
    }

    private void _testSinglePathUriUnfo(final String resourcePath, final String pathParam) {

        final URI baseUri = getBaseUri();
        final String expectedResult = baseUri.resolve(resourcePath + "/uri/" + pathParam).toString();

        final Response response = target().path(resourcePath).path("uri").path(pathParam).request().get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), equalTo(expectedResult));
    }

    private void _testHeader(final String resourcePath) {

        _testSingleHeader(resourcePath, "one");
        _testSingleHeader(resourcePath, "two");
        _testSingleHeader(resourcePath, "three");
    }

    private void _testSingleHeader(final String resourcePath, final String headerValue) {

        final String expectedResult = headerValue;

        final Response response = target().path(resourcePath).path("header").request().header("x-test", headerValue).get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), equalTo(expectedResult));
    }
}
