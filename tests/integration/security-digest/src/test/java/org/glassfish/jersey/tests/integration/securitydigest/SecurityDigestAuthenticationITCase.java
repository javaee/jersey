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
package org.glassfish.jersey.tests.integration.securitydigest;

import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.filter.HttpDigestAuthFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class SecurityDigestAuthenticationITCase extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        ResourceConfig resourceConfig = new ResourceConfig(MyApplication.class);
        resourceConfig.register(new LoggingFilter(Logger.getLogger(SecurityDigestAuthenticationITCase.class.getName()),
                true));
        return resourceConfig;
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }


    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new LoggingFilter(Logger.getLogger(SecurityDigestAuthenticationITCase.class.getName()), true));
    }

    @Test
    public void testResourceGet() {
        final Response response = target().path("rest/resource")
                .register(new HttpDigestAuthFilter("homer", "Homer")).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("homer/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceGet401() {
        final Response response = target().path("rest/resource")
                .register(new HttpDigestAuthFilter("nonexisting", "foo")).request().get();

        Assert.assertEquals(401, response.getStatus());
    }

    @Test
    public void testResourcePost() {
        final Response response = target().path("rest/resource")
                .register(new HttpDigestAuthFilter("homer", "Homer")).request()
                .post(Entity.entity("helloworld", MediaType.TEXT_PLAIN_TYPE));

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("post-helloworld-homer/scheme:DIGEST", response.readEntity(String.class));
    }


    @Test
    public void testResourceSubGet403() {
        final Response response = target().path("rest/resource/sub")
                .register(new HttpDigestAuthFilter("homer", "Homer")).request().get();

        Assert.assertEquals(403, response.getStatus());
    }

    @Test
    public void testResourceSubGet() {
        final Response response = target().path("rest/resource/sub")
                .register(new HttpDigestAuthFilter("marge", "Marge")).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("subget-marge/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceSubGet2() {
        final Response response = target().path("rest/resource/sub")
                .register(new HttpDigestAuthFilter("bart", "Bart")).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("subget-bart/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceLocatorGet() {
        final Response response = target().path("rest/resource/locator")
                .register(new HttpDigestAuthFilter("bart", "Bart")).request().get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("locator-bart/scheme:DIGEST", response.readEntity(String.class));
    }

    @Test
    public void testResourceMultipleRequestsWithOneFilter() {
        WebTarget target = target().path("rest/resource")
                .register(new HttpDigestAuthFilter("homer", "Homer"));
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
