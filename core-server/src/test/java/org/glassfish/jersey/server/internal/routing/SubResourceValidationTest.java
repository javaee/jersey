/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.routing;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * This tests disabling of a sub resource locator validation.
 *
 * @author Miroslav Fuksa
 *
 */
public class SubResourceValidationTest {

    @Path("root")
    public static class RootResource {
        @Path("sub")
        public InvalidSubResource getSubResource() {
            return new InvalidSubResource();
        }
    }


    public static class InvalidSubResource {
        // invalid: multiple get methods on the same path

        @GET
        public String get() {
            return "get";
        }

        @GET
        @Path("aaa")
        public String aget() {
            return "aaa-get";
        }

        @GET
        @Path("aaa")
        public String aget2() {
            return "aaa-get2";
        }

    }

    @Test
    public void testEnable() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(RootResource.class);
        resourceConfig.property(ServerProperties.RESOURCE_VALIDATION_DISABLE, "false");

        ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        try {
            final ContainerResponse response = applicationHandler.apply(
                    RequestContextBuilder.from("/root/sub", "GET").build()).get();
            // should throw an exception or return 500
            Assert.assertEquals(500, response.getStatus());
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testDisable() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(RootResource.class);
        resourceConfig.property(ServerProperties.RESOURCE_VALIDATION_DISABLE, "true");

        ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ContainerResponse response = applicationHandler.apply(
                RequestContextBuilder.from("/root/sub", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
    }
}
