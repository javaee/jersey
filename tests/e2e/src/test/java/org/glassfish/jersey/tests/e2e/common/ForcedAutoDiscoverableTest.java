/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Note: Auto-discoverables from this test "affects" all other tests in suit.
 *
 * @author Michal Gajdos
 */
public class ForcedAutoDiscoverableTest extends JerseyTest {

    private static final String PROPERTY = "ForcedAutoDiscoverableTest";

    public static class CommonAutoDiscoverable implements ForcedAutoDiscoverable {

        @Override
        public void configure(final FeatureContext context) {
            // Return if PROPERTY is not true - applicable for other tests.
            if (!PropertiesHelper.isProperty(context.getConfiguration().getProperty(PROPERTY))) {
                return;
            }

            context.register(new WriterInterceptor() {
                @Override
                public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
                    context.setEntity(context.getEntity() + "-common");

                    context.proceed();
                }
            }, 1);
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class ClientAutoDiscoverable implements ForcedAutoDiscoverable {

        @Override
        public void configure(final FeatureContext context) {
            // Return if PROPERTY is not true - applicable for other tests.
            if (!PropertiesHelper.isProperty(context.getConfiguration().getProperty(PROPERTY))) {
                return;
            }

            context.register(new WriterInterceptor() {
                @Override
                public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
                    context.setEntity(context.getEntity() + "-client");

                    context.proceed();
                }
            }, 10);
        }
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class ServerAutoDiscoverable implements ForcedAutoDiscoverable {

        @Override
        public void configure(final FeatureContext context) {
            // Return if PROPERTY is not true - applicable for other tests.
            if (!PropertiesHelper.isProperty(context.getConfiguration().getProperty(PROPERTY))) {
                return;
            }

            context.register(new WriterInterceptor() {
                @Override
                public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
                    context.setEntity(context.getEntity() + "-server");

                    context.proceed();
                }
            }, 10);
        }
    }

    @Path("/")
    public static class Resource {

        @POST
        public String post(final String value) {
            return value;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class)
                .property(PROPERTY, true)
                .property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE_SERVER, true)
                .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE_SERVER, true);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.property(PROPERTY, true)
        .property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE_CLIENT, true)
        .property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE_CLIENT, true);
    }

    @Test
    public void testForcedAutoDiscoverable() throws Exception {
        final Response response = target().request().post(Entity.text("value"));

        assertThat(response.readEntity(String.class), is("value-common-client-common-server"));
    }
}
