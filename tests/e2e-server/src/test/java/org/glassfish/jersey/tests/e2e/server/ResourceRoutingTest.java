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

package org.glassfish.jersey.tests.e2e.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Miroslav Fuksa
 */
public class ResourceRoutingTest extends JerseyTest {

    @Path("a")
    public static class ResourceA {
        @Path("b/d")
        @GET
        public String get() {
            // this method cannot be chosen as the request path "a/b/..." always firstly choose the ResourceAB and routing
            // will never check this resource. This is based on the algorithm from the jax-rs spec 2
            return "a/b/d";
        }

        @Path("q")
        @GET
        public String getQ() {
            return "a/q";
        }

    }

    @Path("a/b")
    public static class ResourceAB {
        @Path("c")
        @GET
        public String get() {
            return "a/b/c";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ResourceA.class, ResourceAB.class);
    }


    @Test
    public void subWrongPath() throws Exception {
        Response response = target("a/b/d").request().get();
        Assert.assertEquals(404, response.getStatus());
    }

    @Test
    public void correctPath() throws Exception {
        Response response = target("a/b/c").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("a/b/c", response.readEntity(String.class));
    }

    @Test
    public void correctPath2() throws Exception {
        Response response = target("a/q").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("a/q", response.readEntity(String.class));
    }

}




