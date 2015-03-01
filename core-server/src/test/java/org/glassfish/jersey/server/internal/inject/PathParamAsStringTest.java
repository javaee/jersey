/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@SuppressWarnings("unchecked")
public class PathParamAsStringTest extends AbstractTest {

    @Path("/{arg1}/{arg2}/{arg3}")
    public static class Resource {

        @GET
        public String doGet(@PathParam("arg1") String arg1,
                            @PathParam("arg2") String arg2, @PathParam("arg3") String arg3) {
            assertEquals("a", arg1);
            assertEquals("b", arg2);
            assertEquals("c", arg3);
            return "content";
        }

        @POST
        public String doPost(@PathParam("arg1") String arg1,
                             @PathParam("arg2") String arg2, @PathParam("arg3") String arg3,
                             String r) {
            assertEquals("a", arg1);
            assertEquals("b", arg2);
            assertEquals("c", arg3);
            assertEquals("content", r);
            return "content";
        }
    }

    @Test
    public void testStringArgsGet() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource.class);

        _test("/a/b/c");
    }

    @Test
    public void testStringArgsPost() throws ExecutionException, InterruptedException {
        initiateWebApplication(Resource.class);

        final ContainerResponse response = apply(
                RequestContextBuilder.from("/a/b/c", "POST")
                        .entity("content")
                        .build()
        );

        assertEquals("content", response.getEntity());
    }

    @Path("/{id}")
    public static class Duplicate {

        @GET
        public String get(@PathParam("id") String id) {
            return id;
        }

        @GET
        @Path("/{id}")
        public String getSub(@PathParam("id") String id) {
            return id;
        }
    }

    @Test
    public void testDuplicate() throws ExecutionException, InterruptedException {
        initiateWebApplication(Duplicate.class);

        assertEquals("foo", getResponseContext("/foo").getEntity());
        assertEquals("bar", getResponseContext("/foo/bar").getEntity());
    }

    @Path("/{id}")
    public static class DuplicateList {

        @GET
        public String get(@PathParam("id") String id) {
            return id;
        }

        @GET
        @Path("/{id}")
        public String getSub(@PathParam("id") List<String> id) {
            assertEquals(2, id.size());
            return id.get(0) + id.get(1);
        }
    }

    @Test
    public void testDuplicateList() throws ExecutionException, InterruptedException {
        initiateWebApplication(DuplicateList.class);

        assertEquals("foo", getResponseContext("/foo").getEntity());
        assertEquals("barfoo", getResponseContext("/foo/bar").getEntity());
    }
}
