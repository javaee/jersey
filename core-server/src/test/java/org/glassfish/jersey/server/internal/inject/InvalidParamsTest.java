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
package org.glassfish.jersey.server.internal.inject;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InvalidParamsTest {

    ApplicationHandler app;

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    public InvalidParamsTest() {
        app = createApplication(

                ResourceInvalidParams.class);
    }

    public static class ParamEntity {
        public static ParamEntity fromString(
                String arg) {
            return new ParamEntity();
        }
    }

    @Path("invalid/path/param")
    public static class ResourceInvalidParams {

        @PathParam("arg1") String f1;
        @QueryParam("arg2") String f2;
        @CookieParam("arg3") String f3;
        @QueryParam("arg4") ParamEntity f4;

        @GET
        public String doGet(@PathParam("arg1") String s1,
                            @QueryParam("arg2") String s2,
                            @CookieParam("arg3") String s3,
                            @QueryParam("arg4") ParamEntity s4) {
            assertEquals(s1, null);
            assertEquals(s2, null);
            assertEquals(s3, null);
            assertEquals(s4, null);
            assertEquals(f1, null);
            assertEquals(f2, null);
            assertEquals(f3, null);
            assertEquals(f4, null);

            return s1;
        }
    }


    @Test
    public void testInvalidPathParam() throws Exception {
        ContainerResponse responseContext = app.apply(RequestContextBuilder.from("/invalid/path/param", "GET").build()).get();
        // returned param is null -> 204 NO CONTENT
        assertEquals(204, responseContext.getStatus());
    }
}
