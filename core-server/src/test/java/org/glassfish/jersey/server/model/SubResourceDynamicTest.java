/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.model;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Taken from Jersey-1: jersey-tests:com.sun.jersey.impl.subresources.SubResourceDynamicTest
 *
 * @author Paul Sandoz
 */
public class SubResourceDynamicTest {

    ApplicationHandler app;

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/parent")
    public static class Parent {

        @GET
        public String getMe() {
            return "parent";
        }

        @Path("child")
        public Child getChild() {
            return new Child();
        }
    }

    public static class Child {

        @GET
        public String getMe() {
            return "child";
        }
    }

    @Test
    public void testSubResourceDynamic() throws Exception {
        app = createApplication(Parent.class);

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/parent", "GET").accept("text/plain").build()).get();
        assertEquals("parent", response.getEntity());

        response = app.apply(RequestContextBuilder.from("/parent/child", "GET").accept("text/plain").build()).get();
        assertEquals("child", response.getEntity());
    }

    @Path("/{p}")
    public static class ParentWithTemplates {

        @GET
        public String getMe(@PathParam("p") String p) {
            return p;
        }

        @Path("child/{c}")
        public ChildWithTemplates getChildWithTemplates() {
            return new ChildWithTemplates();
        }
    }

    public static class ChildWithTemplates {

        @GET
        public String getMe(@PathParam("c") String c) {
            return c;
        }
    }

    @Test
    public void testSubResourceDynamicWithTemplates() throws Exception {
        app = createApplication(ParentWithTemplates.class);

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/parent", "GET").accept("text/plain").build()).get();
        assertEquals("parent", response.getEntity());
        response = app.apply(RequestContextBuilder.from("/parent/child/first", "GET").accept("text/plain").build()).get();
        assertEquals("first", response.getEntity());
    }

    @Path("/")
    public static class SubResourceExplicitRegexCapturingGroups {

        @Path("{a: (\\d)(\\d*)}-{b: (\\d)(\\d*)}-{c: (\\d)(\\d*)}")
        public SubResourceExplicitRegexCapturingGroupsSub getMultiple() {
            return new SubResourceExplicitRegexCapturingGroupsSub();
        }
    }

    public static class SubResourceExplicitRegexCapturingGroupsSub {

        @GET
        @Path("{d}")
        public String getMe(@PathParam("d") String d) {
            return d;
        }
    }

    @Test
    public void testSubResourceCapturingGroups() throws Exception {
        app = createApplication(SubResourceExplicitRegexCapturingGroups.class);

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/123-456-789/d", "GET").accept("text/plain").build()).get();
        assertEquals("d", response.getEntity());
    }
}
