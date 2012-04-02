/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.client.Target;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.JerseyApplication;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientTest extends JerseyTest {

    @Path("helloworld")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class HelloWorldResource {
        private static final String MESSAGE = "Hello world!";

        @GET
        public String getClichedMessage() {
            return MESSAGE;
        }
    }

    @Override
    protected JerseyApplication configure() {
        return JerseyApplication.builder(new ResourceConfig(HelloWorldResource.class)).build();
    }

    @Test
    public void testAccesingHelloworldResource() {
        Target resource = target().path("helloworld");
        Response r = resource.request().get();
        assertEquals(200, r.getStatus());

        String responseMessage = resource.request().get(String.class);
        assertEquals(HelloWorldResource.MESSAGE, responseMessage);
    }

    @Test
    public void testAccesingMissingResource() {
        Target missingResource = target().path("missing");
        Response r = missingResource.request().get();
        assertEquals(404, r.getStatus());


        try {
            missingResource.request().get(String.class);
        } catch (InvocationException ex) {
            assertEquals(404, ex.getResponse().getStatus());
            return;
        }

        fail("Expected InvocationException has not been thrown.");
    }
}
