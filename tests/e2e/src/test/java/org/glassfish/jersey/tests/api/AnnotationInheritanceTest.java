/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * Test that JAX-RS annotations are correctly inherited according to the specification.
 *
 * The annotations on a super-class should take precedence over annotations on an interface.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class AnnotationInheritanceTest extends JerseyTest {
    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(
                Foo.class, Bar.class
        );
    }

    public interface SuperFooable extends Fooable {
        @GET
        @Path("superFooableWhatsUp")
        String getWhatsUp();
    }

    public interface Fooable {
        @GET
        @Path("fooableHello")
        String getHello();

        @GET
        @Path("fooableHi")
        String getHi();

    }

    public abstract static class SuperFoo implements SuperFooable {
        @GET
        @Path("superFooHello")
        public String getHello() {
            return "-WRONG MESSAGE-";
        }

        // no annotations here this time
        public String getHi() {
            return "-WRONG MESSAGE-";
        }
    }

    @Path(value = "/foo")
    public static class Foo extends SuperFoo implements Fooable {
        public String getHello() {
            return "Hello!";
        }

        public String getHi() {
            return "Hi!";
        }

        public String getWhatsUp() {
            return "What's up?";
        }
    }

    @Path("hyperBar")
    public static class HyperBar {

    }

    public static class SuperBar extends HyperBar {
    }

    @Path("barable")
    public interface Barable {
    }


    public static class Bar extends SuperBar implements Barable {
        @GET
        @Path("bar")
        public String getBar() {
            return "bar";
        }
    }

    /**
     * Test that when there are conflicting annotations on the methods, the annotations in the superclass has higher
     * priority than the one in the interface.
     */
    @Test
    public void testSuperClassPrecedence() {
        final String superClassResponse = target().path("foo/superFooHello").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("The path from the super-class annotation should be used instead of the path from interface",
                "Hello!", superClassResponse);

        final Response ifaceResponse = target().path("foo/fooableHello").request(MediaType.TEXT_PLAIN).get(Response.class);
        assertEquals("The path defined in the interface annotation should not exist.", 404, ifaceResponse.getStatus());
    }

    /**
     * Test that the annotation is inherited from the interface, if it cannot be found in the chain of superclasses.
     */
    @Test
    public void testInterfaceAnnotationInheritance() {
        final String response = target().path("foo/fooableHi").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("The path from the super-class annotation should inherited.",
                "Hi!", response);
    }

    /**
     * Test that if the annotation is not found in the chain of superclasses, it is inherited from the interface, that
     * is "nearest" ito the class. In this particular case - superclasses do not have annotation,
     * that could be inherited, neither does the interface directly implemented by the class, so the test expects the
     * annotation to be inherited from the interface of the superclass.
     */
    @Test
    public void testInheritenceFromSuperclassInterface() {
        final String response = target().path("foo/superFooableWhatsUp").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("The path from the interface of the superclass should inherited.",
                "What's up?", response);
    }

    /**
     * Test that class-level annotation behave in the similar manner as the method-level annotations, although this
     * behaviour is not directly specified in the JSR-339 (in the Chapter 3.6., the specification explicitly states
     * that "Note that inheritance of class or interface annotations is not supported".
     *
     * Jersey does support class-level annotations inheritance as its specific behaviour beyond the JSR scope.
     */
    @Test
    public void testClassAnnotationInheritance() {
        final String superClassResponse = target().path("hyperBar/bar").request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals("The path from the superclass annotation should be used instead of the path from interface",
                "bar", superClassResponse);

        final Response ifaceResponse = target().path("barable/bar").request(MediaType.TEXT_PLAIN).get(Response.class);
        assertEquals("The path defined in the interface annotation should not exist.", 404, ifaceResponse.getStatus());
    }


}