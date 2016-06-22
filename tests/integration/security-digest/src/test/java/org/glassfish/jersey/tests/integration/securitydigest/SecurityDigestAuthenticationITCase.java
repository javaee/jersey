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

package org.glassfish.jersey.tests.integration.securitydigest;

import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Miroslav Fuksa
 */
public class SecurityDigestAuthenticationITCase extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(MyApplication.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new LoggingFeature(Logger.getLogger(SecurityDigestAuthenticationITCase.class.getName()),
                LoggingFeature.Verbosity.PAYLOAD_ANY));
    }

    @Test
    public void testResourceGet() {
        _testResourceGet(HttpAuthenticationFeature.digest("homer", "Homer"));
        _testResourceGet(HttpAuthenticationFeature.universal("homer", "Homer"));
        _testResourceGet(HttpAuthenticationFeature.universalBuilder().credentialsForDigest("homer", "Homer").build());
        _testResourceGet(HttpAuthenticationFeature.universalBuilder().credentialsForDigest("homer", "Homer")
                .credentialsForBasic("aaa", "bbb").build());
    }

    public void _testResourceGet(HttpAuthenticationFeature feature) {
        final Response response = target().path("rest/resource")
                .register(feature).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("homer/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceGet401() {
        _testResourceGet401(HttpAuthenticationFeature.digest("nonexisting", "foo"));
        _testResourceGet401(HttpAuthenticationFeature.universalBuilder().credentials("nonexisting", "foo").build());
    }

    public void _testResourceGet401(HttpAuthenticationFeature feature) {
        final Response response = target().path("rest/resource")
                .register(feature).request().get();

        Assert.assertEquals(401, response.getStatus());
    }

    @Test
    public void testResourcePost() {
        _testResourcePost(HttpAuthenticationFeature.digest("homer", "Homer"));
        _testResourcePost(HttpAuthenticationFeature.universal("homer", "Homer"));
    }

    public void _testResourcePost(HttpAuthenticationFeature feature) {
        final Response response = target().path("rest/resource")
                .register(feature).request()
                .post(Entity.entity("helloworld", MediaType.TEXT_PLAIN_TYPE));

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("post-helloworld-homer/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceSubGet403() {
        _testResourceSubGet403(HttpAuthenticationFeature.digest("homer", "Homer"));
        _testResourceSubGet403(HttpAuthenticationFeature.universal("homer", "Homer"));
    }

    public void _testResourceSubGet403(HttpAuthenticationFeature feature) {
        final Response response = target().path("rest/resource/sub")
                .register(feature).request().get();

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testResourceSubGet() {
        _testResourceSubGet2(HttpAuthenticationFeature.digest("bart", "Bart"));
        _testResourceSubGet2(HttpAuthenticationFeature.universal("bart", "Bart"));
    }

    public void _testResourceSubGet2(HttpAuthenticationFeature feature) {
        final Response response = target().path("rest/resource/sub")
                .register(feature).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("subget-bart/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceLocatorGet() {
        _testResourceLocatorGet(HttpAuthenticationFeature.digest("bart", "Bart"));
        _testResourceLocatorGet(HttpAuthenticationFeature.universal("bart", "Bart"));
    }

    public void _testResourceLocatorGet(HttpAuthenticationFeature feature) {

        final Response response = target().path("rest/resource/locator")
                .register(feature).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("locator-bart/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceMultipleRequestsWithOneFilter() {
        _testResourceMultipleRequestsWithOneFilter(HttpAuthenticationFeature.digest("homer", "Homer"));
        _testResourceMultipleRequestsWithOneFilter(HttpAuthenticationFeature.universal("homer", "Homer"));
    }

    public void _testResourceMultipleRequestsWithOneFilter(HttpAuthenticationFeature haf) {
        WebTarget target = target().path("rest/resource")
                .register(haf);
        Response response = target.request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("homer/scheme:DIGEST", response.readEntity(String.class));

        response = target.request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("homer/scheme:DIGEST", response.readEntity(String.class));

        response = target.path("sub").request().get();
        Assert.assertEquals(403, response.getStatus());

        response = target.request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("homer/scheme:DIGEST", response.readEntity(String.class));

        response = target.path("locator").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("locator-homer/scheme:DIGEST", response.readEntity(String.class));
    }

}
