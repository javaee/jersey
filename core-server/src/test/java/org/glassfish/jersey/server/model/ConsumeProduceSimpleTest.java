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

package org.glassfish.jersey.server.model;


import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import static org.junit.Assert.assertEquals;

/**
 * Taken from Jersey 1: jersey-tests:com.sun.jersey.impl.resource.ConsumeProduceSimpleTest.java
 *
 * @author Paul.Sandoz@Sun.Com
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ConsumeProduceSimpleTest  {

    private Application createApplication(Class<?>... classes) {
        final ResourceConfig resourceConfig = ResourceConfig.builder().addClasses(classes).build();
        return Application.builder(resourceConfig).build();
    }

    @Path("/{arg1}/{arg2}")
    @Consumes("text/html")
    public static class ConsumeSimpleBean {

        @Context
        HttpHeaders httpHeaders;

        @POST
        public String doPostHtml(String data) {
            assertEquals("text/html", httpHeaders.getRequestHeader("Content-Type").get(0));
            return "HTML";
        }

        @POST
        @Consumes("text/xhtml")
        public String doPostXHtml(String data) {
            assertEquals("text/xhtml", httpHeaders.getRequestHeader("Content-Type").get(0));
            return "XHTML";
        }
    }

    @Path("/{arg1}/{arg2}")
    @Produces("text/html")
    public static class ProduceSimpleBean {

        @Context
        HttpHeaders httpHeaders;

        @GET
        public String doGetHtml() {
            assertEquals("text/html", httpHeaders.getRequestHeader("Accept").get(0));
            return "HTML";
        }

        @GET
        @Produces("text/xhtml")
        public String doGetXhtml() {
            assertEquals("text/xhtml", httpHeaders.getRequestHeader("Accept").get(0));
            return "XHTML";
        }
    }

    @Path("/{arg1}/{arg2}")
    @Consumes("text/html")
    @Produces("text/html")
    public static class ConsumeProduceSimpleBean {

        @Context
        HttpHeaders httpHeaders;

        @GET
        public String doGetHtml() {
            assertEquals("text/html", httpHeaders.getRequestHeader("Accept").get(0));
            return "HTML";
        }

        @GET
        @Produces("text/xhtml")
        public String doGetXhtml() {
            assertEquals("text/xhtml", httpHeaders.getRequestHeader("Accept").get(0));
            return "XHTML";
        }

        @POST
        public String doPostHtml(String data) {
            assertEquals("text/html", httpHeaders.getRequestHeader("Content-Type").get(0));
            assertEquals("text/html", httpHeaders.getRequestHeader("Accept").get(0));
            return "HTML";
        }

        @POST
        @Consumes("text/xhtml")
        @Produces("text/xhtml")
        public String doPostXHtml(String data) {
            assertEquals("text/xhtml", httpHeaders.getRequestHeader("Content-Type").get(0));
            assertEquals("text/xhtml", httpHeaders.getRequestHeader("Accept").get(0));
            return "XHTML";
        }
    }

    @Test
    public void testConsumeSimpleBean() throws Exception {
        Application app = createApplication(ConsumeSimpleBean.class);

        assertEquals("HTML", app.apply(Requests.from("/a/b","POST").entity("").type("text/html").build()).get().readEntity(String.class));
        assertEquals("XHTML", app.apply(Requests.from("/a/b","POST").entity("").type("text/xhtml").build()).get().readEntity(String.class));
    }

    @Test
    public void testProduceSimpleBean() throws Exception {
        Application app = createApplication(ProduceSimpleBean.class);

        assertEquals("HTML", app.apply(Requests.from("/a/b","GET").accept("text/html").build()).get().readEntity(String.class));
        assertEquals("XHTML", app.apply(Requests.from("/a/b","GET").accept("text/xhtml").build()).get().readEntity(String.class));
    }

    @Test
    public void testConsumeProduceSimpleBean() throws Exception {
        Application app = createApplication(ConsumeProduceSimpleBean.class);

        assertEquals("HTML", app.apply(Requests.from("/a/b","POST").entity("").type("text/html").accept("text/html").build()).get().readEntity(String.class));
        assertEquals("XHTML", app.apply(Requests.from("/a/b","POST").entity("").type("text/xhtml").accept("text/xhtml").build()).get().readEntity(String.class));
        assertEquals("HTML", app.apply(Requests.from("/a/b","GET").accept("text/html").build()).get().readEntity(String.class));
        assertEquals("XHTML", app.apply(Requests.from("/a/b","GET").accept("text/xhtml").build()).get().readEntity(String.class));
    }

    @Path("/")
    @Consumes("text/html")
    @Produces("text/plain")
    public static class ConsumeProduceWithParameters {

        @Context HttpHeaders h;

        @POST
        public String post(String in) {
            return h.getMediaType().getParameters().toString();
        }
    }

    @Test
    public void testProduceWithParameters() throws Exception {
        Application app = createApplication(ConsumeProduceWithParameters.class);

        assertEquals("{a=b, c=d}", app.apply(Requests.from("/","POST").entity("<html>content</html>").type("text/html;a=b;c=d").build()).get().readEntity(String.class));
    }
}
