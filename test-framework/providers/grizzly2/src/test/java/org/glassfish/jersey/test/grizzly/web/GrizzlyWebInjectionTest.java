/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.grizzly.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test injection support in the {@link org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class GrizzlyWebInjectionTest extends JerseyTest {

    @Path("fields")
    public static class FieldsResource {

        @Context
        private HttpServletRequest request;
        @Context
        private HttpServletResponse response;

        @Context
        private ServletConfig config;
        @Context
        private ServletContext context;

        @GET
        public String get() {
            return testInjections(request, response, config, context);
        }
    }

    @Path("singleton/fields")
    @Singleton
    public static class SingletonFieldsResource {

        @Context
        private HttpServletRequest request;
        @Context
        private HttpServletResponse response;

        @Context
        private ServletConfig config;
        @Context
        private ServletContext context;

        @GET
        public String get() {
            return testInjections(request, response, config, context);
        }
    }

    @Path("/constructor")
    public static class ConstructorResource {

        private HttpServletRequest request;
        private HttpServletResponse response;

        private ServletConfig config;
        private ServletContext context;

        public ConstructorResource(
                @Context HttpServletRequest req,
                @Context HttpServletResponse res,
                @Context ServletConfig sconf,
                @Context ServletContext scont) {
            this.request = req;
            this.response = res;
            this.config = sconf;
            this.context = scont;
        }

        @GET
        public String get() {
            return testInjections(request, response, config, context);
        }
    }

    private static String testInjections(final HttpServletRequest request, final HttpServletResponse response,
                                  final ServletConfig config, final ServletContext context) {
        if (config != null && context != null
                && request != null && response != null
                && config.getInitParameter(ServerProperties.PROVIDER_PACKAGES)
                         .equals(GrizzlyWebInjectionTest.class.getPackage().getName())) {
            return "SUCCESS";
        } else {
            return "FAIL";
        }
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.builder(new ResourceConfig())
                .initParam(ServerProperties.PROVIDER_PACKAGES, this.getClass().getPackage().getName())
                .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @Test
    public void testFields() {
        _test("fields");
    }

    @Test
    public void testSingletonFields() {
        _test("singleton/fields");
    }

    @Test
    public void testConstructor() {
        _test("constructor");
    }


    private void _test(final String path) {
        assertThat(target(path).request().get().readEntity(String.class), is("SUCCESS"));
    }
}
