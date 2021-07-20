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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests injections into constructor parameters.
 *
 * @author Miroslav Fuksa
 */
public class ConstructorParameterInjectionTest extends AbstractTest {
    @Test
    public void testInjection() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceWithConstructor.class);

        _test("/resource/p;matrix=m?query=5");
    }

    @Test
    public void testInjectionIntoFields() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceWithFieldInjections.class);

        _test("/fields/p;matrix=m?query=5");
    }


    @Test
    public void testInjectionsWithoutMatrix() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceWithConstructor.class);

        _test("/resource/p/nullMatrix/?query=5");
    }

    @Path("resource/{path}")
    public static class ResourceWithConstructor {
        private final String pathParam;
        private final int queryParam;
        private final String matrixParam;

        public ResourceWithConstructor(@PathParam("path") String pathParam, @QueryParam("query") int queryParam,
                                       @MatrixParam("matrix") String matrixParam) {
            this.matrixParam = matrixParam;
            this.pathParam = pathParam;
            this.queryParam = queryParam;
        }

        @GET
        public String get() {
            assertEquals("p", pathParam);
            assertEquals(5, queryParam);
            assertEquals("m", matrixParam);
            return "content";
        }

        @GET
        @Path("nullMatrix")
        public String getWithoutMatrixParam() {
            assertEquals("p", pathParam);
            assertEquals(5, queryParam);
            assertNull(matrixParam);
            return "content";
        }
    }


    @Path("fields/{path}")
    public static class ResourceWithFieldInjections {
        @PathParam("path")
        private String pathParam;
        @QueryParam("query")
        private int queryParam;
        @MatrixParam("matrix")
        private String matrixParam;


        @GET
        public String get() {
            assertEquals("p", pathParam);
            assertEquals(5, queryParam);
            assertEquals("m", matrixParam);
            return "content";
        }
    }

}
