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

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Taken from Jersey 1: jersey-tests: com.sun.jersey.impl.errors.PathAndResourceMethodErrorsTest
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class PathAndResourceMethodErrorsTest {

    private List<ResourceModelIssue> initiateWebApplication(Class<?>... resourceClasses) {
        try {
            final ResourceConfig rc = ResourceConfig.builder().addClasses(resourceClasses).build();
            Application.builder(rc).build();
            fail("Application build expected to fail");
        } catch (ResourceModelValidator.ModelException e) {
            return e.issues;
        }
        return null;
    }


    @Path("/{")
    public static class PathErrorsResource {
        @Path("/{")
        @GET
        public String get() { return null; }

        @Path("/{sub")
        public Object sub() { return null; }
    }

    // FIXME
    @Ignore
    @Test
    public void testPathErrors() {
        List<ResourceModelIssue> issues = initiateWebApplication(PathErrorsResource.class);
        assertEquals(3, issues.size());
    }

//   TODO: testing not yet available feature (registering explicit resources).
//    @Path("/{one}")
//    public static class PathErrorsOneResource {
//    }
//
//    @Path("/{two}")
//    public static class PathErrorsTwoResource {
//    }
//
//    @Path("/{three}")
//    public static class PathErrorsThreeResource {
//    }
//
//    public void testConflictingRootResourceErrors() {
//        List<Errors.ErrorMessage> messages = catches(new Closure() {
//            @Override
//            public void f() {
//                ResourceConfig rc = new DefaultResourceConfig(PathErrorsOneResource.class, PathErrorsTwoResource.class);
//                rc.getSingletons().add(new PathErrorsThreeResource());
//                rc.getExplicitRootResources().put("/{four}", PathErrorsOneResource.class);
//                rc.getExplicitRootResources().put("/{five}", new PathErrorsThreeResource());
//
//                initiateWebApplication(rc);
//            }
//        }).messages;
//
//        assertEquals(4, messages.size());
//    }
//
//    public void testConflictingRootResourceErrors2() {
//        List<Errors.ErrorMessage> messages = catches(new Closure() {
//            @Override
//            public void f() {
//                ResourceConfig rc = new DefaultResourceConfig();
//                rc.getExplicitRootResources().put("/{one}", PathErrorsOneResource.class);
//                rc.getExplicitRootResources().put("/{one}/", new PathErrorsThreeResource());
//
//                initiateWebApplication(rc);
//            }
//        }).messages;
//
//        assertEquals(1, messages.size());
//    }

    @Path("/")
    public static class AmbiguousResourceMethodsGET {

        @GET
        public String get1() { return null; }

        @GET
        public String get2() { return null; }

        @GET
        public String get3() { return null; }
    }

    @Test
    public void testAmbiguousResourceMethodGET() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousResourceMethodsGET.class);
        assertEquals(3, issues.size());
    }

    @Path("/")
    public static class AmbiguousResourceMethodsProducesGET {

        @GET
        @Produces("application/xml")
        public String getXml() { return null; }

        @GET
        @Produces("text/plain")
        public String getText1() { return null; }

        @GET
        @Produces("text/plain")
        public String getText2() { return null; }

        @GET
        @Produces({"text/plain", "image/png"})
        public String getText3() { return null; }
    }

    @Test
    public void testAmbiguousResourceMethodsProducesGET() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousResourceMethodsProducesGET.class);
        assertEquals(3, issues.size());
    }

    @Path("/")
    public static class AmbiguousResourceMethodsConsumesPUT {

        @PUT
        @Consumes("application/xml")
        public void put1(Object o) { }

        @PUT
        @Consumes({"text/plain", "image/jpeg"})
        public void put2(Object o) { }

        @PUT
        @Consumes("text/plain")
        public void put3(Object o) { }
    }

    @Test
    public void testAmbiguousResourceMethodsConsumesPUT() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousResourceMethodsConsumesPUT.class);
        assertEquals(1, issues.size());
    }


    @Path("/")
    public static class AmbiguousSubResourceMethodsGET {

        @Path("{one}")
        @GET
        public String get1() { return null; }

        @Path("{seven}")
        @GET
        public String get2() { return null; }

        @Path("{million}")
        @GET
        public String get3() { return null; }

        @Path("{million}/")
        @GET
        public String get4() { return null; }
    }

    @Test
    public void testAmbiguousSubResourceMethodsGET() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousSubResourceMethodsGET.class);
        // TODO: do we want @Path("{million}/") to clash with @Path("{million}") ?
        assertEquals(3, issues.size());
    }

    @Path("/")
    public static class AmbiguousSubResourceMethodsProducesGET {

        @Path("x")
        @GET
        @Produces("application/xml")
        public String getXml() { return null; }

        @Path("x")
        @GET
        @Produces("text/plain")
        public String getText1() { return null; }

        @Path("x")
        @GET
        @Produces("text/plain")
        public String getText2() { return null; }

        @Path("x")
        @GET
        @Produces({"text/plain", "image/png"})
        public String getText3() { return null; }
    }

    @Test
    public void testAmbiguousSubResourceMethodsProducesGET() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousSubResourceMethodsProducesGET.class);
        assertEquals(3, issues.size());
    }

    @Path("/")
    public static class AmbiguousSubResourceLocatorsResource {

        @Path("{one}")
        public Object l1() { return null; }

        @Path("{two}")
        public Object l2() { return null; }
    }

    // FIXME: sub resource locators not supported yet
    @Ignore
    @Test
    public void testAmbiguousSubResourceLocatorsResource() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousSubResourceLocatorsResource.class);
        assertEquals(1, issues.size());
    }

    @Path("/")
    public static class AmbiguousSubResourceLocatorsWithSlashResource {

        @Path("{one}")
        public Object l1() { return null; }

        @Path("{two}/")
        public Object l2() { return null; }
    }

    // FIXME: sub resource locators not supported yet
    @Ignore
    @Test
    public void testAmbiguousSubResourceLocatorsWithSlashResource() {
        final List<ResourceModelIssue> issues = initiateWebApplication(AmbiguousSubResourceLocatorsWithSlashResource.class);
        assertEquals(1, issues.size());
    }
}
