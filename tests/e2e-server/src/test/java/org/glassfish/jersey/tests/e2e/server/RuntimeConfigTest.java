/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import javax.inject.Inject;

import org.glassfish.jersey.internal.InternalProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos
 */
public class RuntimeConfigTest extends JerseyTest {

    @Path("/")
    public static class Resource {

        @GET
        public String get() {
            return "get";
        }
    }

    public static class EmptyFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            return true;
        }
    }

    public static class ClientFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            context.register(ClientReaderInterceptor.class);
            context.property("foo", "bar");
            return true;
        }
    }

    public static class ClientReaderInterceptor implements ReaderInterceptor {

        private final Configuration config;

        @Inject
        public ClientReaderInterceptor(final Configuration configuration) {
            this.config = configuration;
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            assertTrue(config.isRegistered(ClientFeature.class));
            assertTrue(config.isRegistered(ClientReaderInterceptor.class));

            assertThat(config.getProperties().size(), is(2));
            assertThat(config.getProperty("foo").toString(), is("bar"));

            // JsonFeature
            assertThat(config.getProperty(InternalProperties.JSON_FEATURE_CLIENT), notNullValue());

            // MetaInfAutoDiscoverable
            assertThat(config.getInstances().size(), is(1));
            assertTrue(config.isEnabled(ClientFeature.class));

            context.getHeaders().add("CustomHeader", "ClientReaderInterceptor");

            return context.proceed();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testRuntimeClientConfig() throws Exception {
        final WebTarget target = target();

        target.register(ClientFeature.class);

        final Response response = target.request(MediaType.WILDCARD_TYPE).get(Response.class);

        assertEquals(1, target.getConfiguration().getClasses().size());
        assertTrue(target.getConfiguration().isRegistered(ClientFeature.class));
        assertTrue(target.getConfiguration().getInstances().isEmpty());
        assertTrue(target.getConfiguration().getProperties().isEmpty());
        assertFalse(target.getConfiguration().isEnabled(ClientFeature.class));

        WebTarget t = target();
        assertEquals(0, t.getConfiguration().getClasses().size());
        assertFalse(t.getConfiguration().isRegistered(ClientFeature.class));
        assertTrue(t.getConfiguration().getInstances().isEmpty());
        assertTrue(t.getConfiguration().getProperties().isEmpty());
        assertFalse(t.getConfiguration().isEnabled(ClientFeature.class));

        assertEquals("get", response.readEntity(String.class));
        assertEquals("ClientReaderInterceptor", response.getHeaderString("CustomHeader"));
    }
}
