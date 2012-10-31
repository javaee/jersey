/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import javax.inject.Inject;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
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
        public boolean configure(final Configurable configurable) {
            return true;
        }
    }

    public static class ClientFeature implements Feature {

        @Override
        public boolean configure(final Configurable configurable) {
            configurable.register(ClientReaderInterceptor.class);
            configurable.setProperty("foo", "bar");
            return true;
        }
    }

    public static class ClientReaderInterceptor implements ReaderInterceptor {

        private final Configurable config;

        @Inject
        public ClientReaderInterceptor(final Configurable configurable) {
            this.config = configurable;
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            assertEquals(1, config.getProviderClasses().size());
            assertEquals(ClientReaderInterceptor.class, config.getProviderClasses().iterator().next());

            assertEquals(1, config.getProperties().size());
            assertEquals("bar", config.getProperty("foo"));

            assertTrue(config.getProviderInstances().isEmpty());
            assertTrue(!config.getFeatures().isEmpty());

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

        target.configuration().register(ClientFeature.class);

        final Response response = target.request(MediaType.WILDCARD_TYPE).get(Response.class);

        for (final Configurable config : new Configurable[] {target.configuration(), target().configuration()}) {
            assertTrue(config.getProviderClasses().isEmpty());
            assertTrue(config.getProviderInstances().isEmpty());
            assertTrue(config.getProperties().isEmpty());
        }

        assertEquals(1, target.configuration().getFeatures().size());
        assertEquals(0, target().configuration().getFeatures().size());

        assertEquals("get", response.readEntity(String.class));
        assertEquals("ClientReaderInterceptor", response.getHeaderString("CustomHeader"));
    }
}
