/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Miroslav Fuksa
 *
 */
public class XmlJaxBElementProviderTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(Atom.class);
    }

    @Path("atom")
    public static class Atom {
        @Context
        HttpHeaders headers;

        @Path("wildcard")
        @POST
        @Consumes("application/*")
        @Produces("application/*")
        public Response wildcard(JAXBElement<String> jaxb) {
            MediaType media = headers.getMediaType();
            return Response.ok(jaxb).type(media).build();
        }

        @Path("atom")
        @POST
        @Consumes("application/atom+xml")
        @Produces("application/atom+xml")
        public Response atom(JAXBElement<String> jaxb) {
            MediaType media = headers.getMediaType();
            return Response.ok(jaxb).type(media).build();
        }

        @Path("empty")
        @POST
        public Response emptyConsumesProduces(JAXBElement<String> jaxb) {
            MediaType media = headers.getMediaType();
            return Response.ok(jaxb).type(media).build();
        }
    }

    @Test
    public void testWildcard() {
        final String path = "atom/wildcard";
        _test(path);
    }

    private void _test(String path) {
        WebTarget target = target(path);
        final Response res = target.request("application/atom+xml").post(
                Entity.entity(new JAXBElement<String>(new QName("atom"), String.class, "value"),
                        "application/atom+xml"));
        assertEquals(200, res.getStatus());
        final GenericType<JAXBElement<String>> genericType = new GenericType<JAXBElement<String>>() {};
        final JAXBElement<String> stringJAXBElement = res.readEntity(genericType);
        assertEquals("value", stringJAXBElement.getValue());
    }

    @Test
    public void testAtom() {
        final String path = "atom/atom";
        _test(path);
    }

    @Test
    public void testEmpty() {
        final String path = "atom/empty";
        _test(path);
    }
}
