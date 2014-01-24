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
package org.glassfish.jersey.test.inmemory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Package scanning test for {@link org.glassfish.jersey.test.inmemory.InMemoryConnector}.
 *
 * @author Paul Sandoz
 */
public class InMemoryContainerPackageTest extends JerseyTest {

    /**
     * Creates new instance.
     */
    public InMemoryContainerPackageTest() {
        super(new InMemoryTestContainerFactory());
    }

    @Override
    protected ResourceConfig configure() {
        ResourceConfig rc = new ResourceConfig();
        rc.packages(this.getClass().getPackage().getName());
        return rc;
    }

    /**
     * Test resource class.
     */
    @Path("packagetest")
    public static class TestResource {

        /**
         * Test resource method.
         *
         * @return Test simple string response.
         */
        @GET
        public String getSomething() {
            return "get";
        }

        /**
         * Test (sub)resource method.
         *
         * @return Simple string test response.
         */
        @GET
        @Path("sub")
        public String getSubResource() {
            return "sub";
        }
    }

    @Test
    public void testInMemoryConnectorGet() {
        final Response response = target("packagetest").request().get();

        assertTrue(response.getStatus() == 200);
    }

    @Test
    public void testGetSub() {
        final String response = target("packagetest").path("sub").request().get(String.class);
        assertEquals("sub", response);
    }
}
