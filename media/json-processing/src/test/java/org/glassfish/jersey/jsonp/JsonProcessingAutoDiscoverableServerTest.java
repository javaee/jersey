/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jsonp;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Michal Gajdos
 */
public class JsonProcessingAutoDiscoverableServerTest {

    public static class Filter implements ContainerRequestFilter {

        @Context
        public Configuration config;

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            final Response response = Response
                    .status(400)
                    .entity(config.isEnabled(JsonProcessingFeature.class) ? "Enabled" : "Disabled")
                    .build();

            requestContext.abortWith(response);
        }
    }

    @Path("/")
    public static class Resource {

        @GET
        public String doGetFoo(@Context HttpHeaders headers) {
            return Integer.toString(headers.getLength());
        }
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerDefault() throws Exception {
        _test("Enabled", null, null);
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerEnabled() throws Exception {
        _test("Enabled", null, false);
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerDisabled() throws Exception {
        _test("Disabled", null, true);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerDefault() throws Exception {
        _test("Disabled", true, null);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerEnabled() throws Exception {
        _test("Enabled", true, false);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerDisabled() throws Exception {
        _test("Disabled", true, true);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerDefault() throws Exception {
        _test("Enabled", false, null);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerEnabled() throws Exception {
        _test("Enabled", false, false);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerDisabled() throws Exception {
        _test("Disabled", false, true);
    }

    private void _test(final String response, final Boolean globalDisable, final Boolean serverDisable) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, Filter.class);

        if (globalDisable != null) {
            resourceConfig.property(CommonProperties.JSON_PROCESSING_FEATURE_DISABLE, globalDisable);
        }
        if (serverDisable != null) {
            resourceConfig.property(ServerProperties.JSON_PROCESSING_FEATURE_DISABLE, serverDisable);
        }

        final ApplicationHandler app = new ApplicationHandler(resourceConfig);

        final URI baseUri = URI.create("/");
        assertEquals(response, app.apply(new ContainerRequest(baseUri, baseUri, "GET", null, new MapPropertiesDelegate())).get()
                .getEntity());
    }
}
