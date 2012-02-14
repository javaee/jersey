/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Taken from Jersey-1: jersey-tests:com.sun.jersey.impl.subresources.SubResourceDynamicTest
 *
 * @author Paul.Sandoz at Sun.Com
 */
public class SubResourceDynamicTest {

    Application app;

    private Application.Builder createApplicationBuilder(Class<?>... rc) {
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(rc).build();

        return Application.builder(resourceConfig);
    }

    @Path("/parent")
    static public class Parent {
        @GET
        public String getMe() {
            return "parent";
        }

        @Path("child")
        public Child getChild() {
            return new Child();
        }
    }

    static public class Child {
        @GET
        public String getMe() {
            return "child";
        }
    }

    @Test
    public void testSubResourceDynamic() throws Exception {
        app = createApplicationBuilder(Parent.class).build();

        Response response;

        response = app.apply(Requests.from("/parent","GET").accept("text/plain").build()).get();
        assertEquals("parent", response.readEntity(String.class));

        response = app.apply(Requests.from("/parent/child","GET").accept("text/plain").build()).get();
        assertEquals("child", response.readEntity(String.class));
    }

    @Path("/{p}")
    static public class ParentWithTemplates {
        @GET
        public String getMe(@PathParam("p") String p) {
            return p;
        }

        @Path("child/{c}")
        public ChildWithTemplates getChildWithTemplates() {
            return new ChildWithTemplates();
        }
    }

    static public class ChildWithTemplates {
        @GET
        public String getMe(@PathParam("c") String c) {
            return c;
        }
    }

    @Test
    public void testSubResourceDynamicWithTemplates() throws Exception {
        app = createApplicationBuilder(ParentWithTemplates.class).build();

        Response response;

        response = app.apply(Requests.from("/parent","GET").accept("text/plain").build()).get();
        assertEquals("parent", response.readEntity(String.class));
        response = app.apply(Requests.from("/parent/child/first","GET").accept("text/plain").build()).get();
        assertEquals("first", response.readEntity(String.class));
    }


    @Path("/")
    static public class SubResourceExplicitRegexCapturingGroups {
        @Path("{a: (\\d)(\\d*)}-{b: (\\d)(\\d*)}-{c: (\\d)(\\d*)}")
        public SubResourceExplicitRegexCapturingGroupsSub getMultiple() {
            return new SubResourceExplicitRegexCapturingGroupsSub();
        }
    }

    static public class SubResourceExplicitRegexCapturingGroupsSub {
        @GET
        @Path("{d}")
        public String getMe(@PathParam("d") String d) {
            return d;
        }
    }

    @Test
    public void testSubResourceCapturingGroups() throws Exception {
        app = createApplicationBuilder(SubResourceExplicitRegexCapturingGroups.class).build();

        Response response;

        response = app.apply(Requests.from("/123-456-789/d","GET").accept("text/plain").build()).get();
        assertEquals("d", response.readEntity(String.class));
    }
}