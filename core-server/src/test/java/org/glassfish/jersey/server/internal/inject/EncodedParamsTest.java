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

package org.glassfish.jersey.server.internal.inject;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
@SuppressWarnings("unchecked")
public class EncodedParamsTest extends AbstractTest {

    @Encoded
    @Path("/{u}")
    public static class EncodedOnClass {
        public EncodedOnClass(
                @PathParam("u") String u,
                @QueryParam("q") String q,
                @MatrixParam("m") String m) {
            assertEquals("%20u", u);
            assertEquals("%20q", q);
            assertEquals("%20m", m);
        }

        @GET
        public String doGet(
                @PathParam("u") String u,
                @QueryParam("q") String q,
                @MatrixParam("m") String m) {
            assertEquals("%20u", u);
            assertEquals("%20q", q);
            assertEquals("%20m", m);
            return "content";
        }
    }

    public void testEncodedOnClass() throws ExecutionException, InterruptedException {
        initiateWebApplication(EncodedOnClass.class);

        _test("/%20u;m=%20m?q=%20q");
    }

    @Path("/{u}")
    public static class EncodedOnAccessibleObject {
        @Encoded
        public EncodedOnAccessibleObject(
                @PathParam("u") String u,
                @QueryParam("q") String q,
                @MatrixParam("m") String m) {
            assertEquals("%20u", u);
            assertEquals("%20q", q);
            assertEquals("%20m", m);
        }

        @Encoded
        @GET
        public String doGet(
                @PathParam("u") String u,
                @QueryParam("q") String q,
                @MatrixParam("m") String m) {
            assertEquals("%20u", u);
            assertEquals("%20q", q);
            assertEquals("%20m", m);
            return "content";
        }
    }

    public void testEncodedOnAccessibleObject() throws ExecutionException, InterruptedException {
        initiateWebApplication(EncodedOnAccessibleObject.class);

        _test("/%20u;m=%20m?q=%20q");
    }

    @Path("/{u}")
    public static class EncodedOnParameters {
        public EncodedOnParameters(
                @Encoded @PathParam("u") String u,
                @Encoded @QueryParam("q") String q,
                @Encoded @MatrixParam("m") String m) {
            assertEquals("%20u", u);
            assertEquals("%20q", q);
            assertEquals("%20m", m);
        }

        @GET
        public String doGet(
                @Encoded @PathParam("u") String u,
                @Encoded @QueryParam("q") String q,
                @Encoded @MatrixParam("m") String m) {
            assertEquals("%20u", u);
            assertEquals("%20q", q);
            assertEquals("%20m", m);
            return "content";
        }
    }

    public void testEncodedOnParameters() throws ExecutionException, InterruptedException {
        initiateWebApplication(EncodedOnParameters.class);

        _test("/%20u;m=%20m?q=%20q");
    }

    @Path("/{u}")
    public static class MixedEncodedOnParameters {
        public MixedEncodedOnParameters(
                @PathParam("u") String du,
                @QueryParam("q") String dq,
                @MatrixParam("m") String dm,
                @Encoded @PathParam("u") String eu,
                @Encoded @QueryParam("q") String eq,
                @Encoded @MatrixParam("m") String em) {
            assertEquals(" u", du);
            assertEquals(" q", dq);
            assertEquals(" m", dm);
            assertEquals("%20u", eu);
            assertEquals("%20q", eq);
            assertEquals("%20m", em);
        }

        @GET
        public String doGet(
                @PathParam("u") String du,
                @QueryParam("q") String dq,
                @MatrixParam("m") String dm,
                @Encoded @PathParam("u") String eu,
                @Encoded @QueryParam("q") String eq,
                @Encoded @MatrixParam("m") String em) {
            assertEquals(" u", du);
            assertEquals(" q", dq);
            assertEquals(" m", dm);
            assertEquals("%20u", eu);
            assertEquals("%20q", eq);
            assertEquals("%20m", em);
            return "content";
        }
    }

    public void testMixedEncodedOnParameters() throws ExecutionException, InterruptedException {
        initiateWebApplication(MixedEncodedOnParameters.class);

        _test("/%20u;m=%20m?q=%20q");
    }
}
