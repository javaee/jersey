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

import javax.inject.Singleton;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Taken from Jersey-1: jersey-tests:com.sun.jersey.impl.subresources.SubResourceClassDynamicTest
 *
 * @author Paul Sandoz
 */
public class SubResourceClassDynamicTest {

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
        public Class<Child> getChild() {
            return Child.class;
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

        assertEquals("parent", app.apply(RequestContextBuilder.from("/parent", "GET").build()).get().getEntity());
        assertEquals("child", app.apply(RequestContextBuilder.from("/parent/child", "GET").build()).get().getEntity());
    }

    @Path("/{p}")
    public static class ParentWithTemplates {

        @GET
        public String getMe(@PathParam("p") String p) {
            return p;
        }

        @Path("child/{c}")
        public Class<ChildWithTemplates> getChildWithTemplates() {
            return ChildWithTemplates.class;
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

        assertEquals("parent", app.apply(RequestContextBuilder.from("/parent", "GET").build()).get().getEntity());
        assertEquals("first", app.apply(RequestContextBuilder.from("/parent/child/first", "GET").build()).get().getEntity());
    }

    @Path("/{p}")
    public static class ParentWithTemplatesLifecycle {

        @GET
        public String getMe(@PathParam("p") String p) {
            return p;
        }

        @Path("child/{c}")
        public Class<ChildWithTemplatesPerRequest> getChildWithTemplates() {
            return ChildWithTemplatesPerRequest.class;
        }

        @Path("child/singleton/{c}")
        public Class<ChildWithTemplatesSingleton> getChildWithTemplatesSingleton() {
            return ChildWithTemplatesSingleton.class;
        }
    }

    public static class ChildWithTemplatesPerRequest {

        private int i = 0;
        private String c;

        public ChildWithTemplatesPerRequest(@PathParam("c") String c) {
            this.c = c;
        }

        @GET
        public String getMe() {
            i++;
            return c + i;
        }
    }

    @Singleton
    public static class ChildWithTemplatesSingleton {

        private int i = 0;

        @GET
        public String getMe(@PathParam("c") String c) {
            i++;
            return c + i;
        }
    }

    @Test
    public void testSubResourceDynamicWithTemplatesLifecycle() throws Exception {
        app = createApplication(ParentWithTemplatesLifecycle.class);

        assertEquals("parent", app.apply(RequestContextBuilder.from("/parent", "GET").build()).get().getEntity());
        assertEquals("x1", app.apply(RequestContextBuilder.from("/parent/child/x", "GET").build()).get().getEntity());
        assertEquals("x1", app.apply(RequestContextBuilder.from("/parent/child/x", "GET").build()).get().getEntity());
        assertEquals("x1", app.apply(RequestContextBuilder.from("/parent/child/singleton/x", "GET").build()).get().getEntity());
        assertEquals("x2", app.apply(RequestContextBuilder.from("/parent/child/singleton/x", "GET").build()).get().getEntity());
    }
}
