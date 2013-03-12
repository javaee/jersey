/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.entity;

import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * JERSEY-1540.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EmptyEntityTest extends AbstractTypeTester {

    public EmptyEntityTest() {
        enable(TestProperties.LOG_TRAFFIC);
    }

    @Path("empty")
    public static class EmptyResource {

        @POST
        @Path("jaxbelem")
        public String jaxbelem(JAXBElement<String> jaxb) {
            return "ERROR"; // shouldn't be called
        }

        @POST
        @Path("jaxbbean")
        public String jaxbbean(A a) {
            return "ERROR"; // shouldn't be called
        }

        @POST
        @Path("jaxblist")
        public String jaxblist(List<A> a) {
            return "ERROR"; // shouldn't be called
        }

        @POST
        @Path("boolean")
        public String bool(Boolean b) {
            return "ERROR"; // shouldn't be called
        }

        @POST
        @Path("character")
        public String character(Character c) {
            return "ERROR"; // shouldn't be called
        }

        @POST
        @Path("integer")
        public String integer(Integer i) {
            return "ERROR"; // shouldn't be called
        }
    }

    @Test
    public void testJAXBElement() {
        WebTarget target = target("empty/jaxbelem");
        final Response response = target.request().post(Entity.entity(null, MediaType.APPLICATION_XML_TYPE));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testJAXBbean() {
        WebTarget target = target("empty/jaxbbean");
        final Response response = target.request().post(Entity.entity(null, MediaType.APPLICATION_XML_TYPE));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testJAXBlist() {
        WebTarget target = target("empty/jaxblist");
        final Response response = target.request().post(Entity.entity(null, MediaType.APPLICATION_XML_TYPE));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testBoolean() {
        WebTarget target = target("empty/boolean");
        final Response response = target.request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testCharacter() {
        WebTarget target = target("empty/character");
        final Response response = target.request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(400, response.getStatus());
    }

    @Test
    public void testInteger() {
        WebTarget target = target("empty/integer");
        final Response response = target.request().post(Entity.entity(null, MediaType.TEXT_PLAIN_TYPE));

        assertEquals(400, response.getStatus());
    }

    @XmlRootElement
    public static class A {

    }
}
