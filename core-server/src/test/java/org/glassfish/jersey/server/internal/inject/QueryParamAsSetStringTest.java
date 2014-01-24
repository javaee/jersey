/*
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Paul Sandoz
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@SuppressWarnings("unchecked")
public class QueryParamAsSetStringTest extends AbstractTest {

    @Path("/")
    public static class ResourceStringSet {
        @GET
        @Produces("application/stringSet")
        public String doGetString(@QueryParam("args") Set<String> args) {
            assertTrue(args.contains("a"));
            assertTrue(args.contains("b"));
            assertTrue(args.contains("c"));
            return "content";
        }

        @GET
        @Produces("application/Set")
        public String doGet(@QueryParam("args") Set args) {
            assertTrue(args.contains("a"));
            assertTrue(args.contains("b"));
            assertTrue(args.contains("c"));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSetEmpty {
        @GET
        @Produces("application/stringSet")
        public String doGetString(@QueryParam("args") Set<String> args) {
            assertEquals(1, args.size());
            assertTrue(args.contains(""));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSetEmptyDefault {
        @GET
        @Produces("application/stringSet")
        public String doGetString(
                @QueryParam("args") Set<String> args) {
            assertEquals(0, args.size());
            return "content";
        }

        @GET
        @Produces("application/Set")
        public String doGet(
                @QueryParam("args") Set args) {
            assertEquals(0, args.size());
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSetDefault {
        @GET
        @Produces("application/stringSet")
        public String doGetString(
                @QueryParam("args") @DefaultValue("a") Set<String> args) {
            assertTrue(args.contains("a"));
            return "content";
        }

        @GET
        @Produces("application/Set")
        public String doGet(
                @QueryParam("args") @DefaultValue("a") Set args) {
            assertTrue(args.contains("a"));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSetDefaultOverride {
        @GET
        @Produces("application/stringSet")
        public String doGetString(
                @QueryParam("args") @DefaultValue("a") Set<String> args) {
            assertTrue(args.contains("b"));
            return "content";
        }

        @GET
        @Produces("application/Set")
        public String doGet(
                @QueryParam("args") @DefaultValue("a") Set args) {
            assertTrue(args.contains("b"));
            return "content";
        }
    }


    @Test
    public void testStringSetGet() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSet.class);

        _test("/?args=a&args=b&args=c", "application/stringSet");
    }

    @Test
    public void testStringSetEmptyGet() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSetEmpty.class);

        _test("/?args&args&args", "application/stringSet");
    }

    @Test
    public void testSetGet() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSet.class);

        _test("/?args=a&args=b&args=c", "application/Set");
    }

    @Test
    public void testStringSetEmptyDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSetEmptyDefault.class);

        _test("/", "application/stringSet");
    }

    @Test
    public void testSetEmptyDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSetEmptyDefault.class);

        _test("/", "application/Set");
    }

    @Test
    public void testStringSetDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSetDefault.class);

        _test("/", "application/stringSet");
    }

    @Test
    public void testSetDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSetDefault.class);

        _test("/", "application/Set");
    }

    @Test
    public void testSetDefaultOverride() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSetDefaultOverride.class);

        _test("/?args=b", "application/Set");
    }
}
