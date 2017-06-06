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

package org.glassfish.jersey.tests.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import javax.inject.Singleton;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test {@link ResourceContext}: resource context must provide access to
 * sub-resources that can be provided by a custom component provider.
 *
 * @author Martin Grotzke
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
public class ResourceContextTest extends JerseyTest {

    @Before
    public void setup() {
        Assume.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(MyRootResource.class);
    }

    @Path("/")
    public static class MyRootResource {

        @Context
        ResourceContext resourceContext;

        @Path("singleton")
        public SingletonResource getSingletonResource() {
            return resourceContext.getResource(SingletonResource.class);
        }

        @Path("perrequest")
        public PerRequestResource getPerRequestSubResource() {
            return resourceContext.getResource(PerRequestResource.class);
        }

        @Path("inject/{path}")
        public InjectResource getInjectResource() {
            final InjectResource resource = resourceContext.getResource(InjectResource.class);
            resource.setPath("something");
            return resourceContext.initResource(resource);
        }

        @Path("injectFromNewResource/{path}")
        public InjectResource getInjectResourceFromNew() {
            final InjectResource resource = new InjectResource();
            resource.setPath("something");
            return resourceContext.initResource(resource);
        }

    }

    public static class InjectResource {
        @PathParam("path")
        private String path;

        @GET
        public String get() {
            return path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }


    @Singleton
    public static class SingletonResource {
        int i;

        @GET
        public String get() {
            i++;
            return Integer.toString(i);
        }
    }

    public static class PerRequestResource {
        int i;

        @GET
        public String get() {
            i++;
            return Integer.toString(i);
        }
    }

    @Test
    public void testGetResourceFromResourceContext() {
        assertEquals("1", target("/singleton").request().get(String.class));
        assertEquals("2", target("/singleton").request().get(String.class));

        assertEquals("1", target("/perrequest").request().get(String.class));
        assertEquals("1", target("/perrequest").request().get(String.class));
    }


    @Test
    public void testInitializeResourceFromResourceContext() {
        assertEquals("aaa", target("/inject/aaa").request().get(String.class));
        assertEquals("bbb", target("/inject/bbb").request().get(String.class));
    }

    @Test
    public void testInitializeResourceFromNewResource() {
        assertEquals("aaa", target("/injectFromNewResource/aaa").request().get(String.class));
        assertEquals("bbb", target("/injectFromNewResource/bbb").request().get(String.class));
    }

}
