/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Matula
 */
public class HttpMethodOverrideFilterTest {

    @Test
    public void testEnableFor() {
        ResourceConfig rc = new ResourceConfig();
        HttpMethodOverrideFilter.enableFor(rc, HttpMethodOverrideFilter.Source.HEADER);
        assertTrue(rc.getClasses().contains(HttpMethodOverrideFilter.class));
        assertEquals(1, ((HttpMethodOverrideFilter.Source[]) rc.getProperty(ServerProperties.HTTP_METHOD_OVERRIDE)).length);
        assertEquals(HttpMethodOverrideFilter.Source.HEADER,
                ((HttpMethodOverrideFilter.Source[]) rc.getProperty(ServerProperties.HTTP_METHOD_OVERRIDE))[0]);
    }

    @Test
    public void testDefaultConfig() {
        HttpMethodOverrideFilter f = new HttpMethodOverrideFilter(new ResourceConfig());
        assertTrue(HttpMethodOverrideFilter.Source.HEADER.isPresentIn(f.config)
                && HttpMethodOverrideFilter.Source.QUERY.isPresentIn(f.config));
    }

    @Test
    public void testHeaderOnlyConfig() {
        HttpMethodOverrideFilter f = new HttpMethodOverrideFilter(new ResourceConfig().property(
                ServerProperties.HTTP_METHOD_OVERRIDE, "HEADER"
        ));
        assertTrue(HttpMethodOverrideFilter.Source.HEADER.isPresentIn(f.config)
                && !HttpMethodOverrideFilter.Source.QUERY.isPresentIn(f.config));
    }

    @Test
    public void testQueryOnlyConfig() {
        HttpMethodOverrideFilter f = new HttpMethodOverrideFilter(new ResourceConfig().property(
                ServerProperties.HTTP_METHOD_OVERRIDE, "QUERY"
        ));
        assertTrue(!HttpMethodOverrideFilter.Source.HEADER.isPresentIn(f.config)
                && HttpMethodOverrideFilter.Source.QUERY.isPresentIn(f.config));
    }

    @Test
    public void testHeaderAndQueryConfig() {
        HttpMethodOverrideFilter f = new HttpMethodOverrideFilter(new ResourceConfig().property(
                ServerProperties.HTTP_METHOD_OVERRIDE, "HEADER, QUERY"
        ));
        assertTrue(HttpMethodOverrideFilter.Source.HEADER.isPresentIn(f.config)
                && HttpMethodOverrideFilter.Source.QUERY.isPresentIn(f.config));
    }

    @Test
    public void testInvalidAndQueryConfig() {
        HttpMethodOverrideFilter f = new HttpMethodOverrideFilter(new ResourceConfig().property(
                ServerProperties.HTTP_METHOD_OVERRIDE, "foo, QUERY"
        ));
        assertTrue(!HttpMethodOverrideFilter.Source.HEADER.isPresentIn(f.config)
                && HttpMethodOverrideFilter.Source.QUERY.isPresentIn(f.config));
    }

    @Test
    public void testInitWithStringArrayConfig() {
        HttpMethodOverrideFilter f = new HttpMethodOverrideFilter(new ResourceConfig().property(
                ServerProperties.HTTP_METHOD_OVERRIDE, new String[] {"HEADER"}
        ));
        assertTrue(HttpMethodOverrideFilter.Source.HEADER.isPresentIn(f.config)
                && !HttpMethodOverrideFilter.Source.QUERY.isPresentIn(f.config));
    }

    @Path("/")
    public static class Resource {

        @PUT
        public String put() {
            return "PUT";
        }

        @DELETE
        public String delete() {
            return "DELETE";
        }
    }

    @Test
    public void testDefault() {
        assertEquals("400", test());
    }

    @Test
    public void testHeader() {
        assertEquals("PUT", test(HttpMethodOverrideFilter.Source.HEADER));
    }

    @Test
    public void testQuery() {
        assertEquals("DELETE", test(HttpMethodOverrideFilter.Source.QUERY));
    }

    @Test
    public void testHeaderAndQuery() {
        assertEquals("400", test(HttpMethodOverrideFilter.Source.HEADER, HttpMethodOverrideFilter.Source.QUERY));
    }

    public String test(HttpMethodOverrideFilter.Source... sources) {
        ResourceConfig rc = new ResourceConfig(Resource.class);
        HttpMethodOverrideFilter.enableFor(rc, sources);
        ApplicationHandler handler = new ApplicationHandler(rc);
        try {
            ContainerResponse response = handler.apply(RequestContextBuilder.from("", "/?_method=DELETE", "POST")
                    .header("X-HTTP-Method-Override", "PUT").build()).get();
            if (Response.Status.OK.equals(response.getStatusInfo())) {
                return (String) response.getEntity();
            } else {
                return "" + response.getStatus();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
