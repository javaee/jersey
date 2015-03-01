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

package org.glassfish.jersey.tests.e2e;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.uri.UriTemplate;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Martin Matula
 */
public class LinkTest extends JerseyTest {

    @Path("resource")
    public static class Resource {

        @POST
        @Produces({
                MediaType.APPLICATION_XHTML_XML,
                MediaType.APPLICATION_ATOM_XML,
                MediaType.APPLICATION_SVG_XML
        })
        @Path("producesxml")
        public String producesXml() {
            return "";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, LinkTestResource.class);
    }

    @Test
    public void testEquals() {
        Link link = Link.fromMethod(Resource.class, "producesXml").build();
        String string = link.toString();
        Link fromValueOf = Link.valueOf(string);
        assertEquals(link, fromValueOf);
    }

    @Test
    public void testFromResourceMethod() {
        Link link = Link.fromMethod(Resource.class, "producesXml").build();
        assertEquals("producesxml", link.getUri().toString());
    }

    @Test
    public void testDelimiters() {
        Link.Builder builder = Link.fromUri("http://localhost:80");
        final String value = "param1value1    param1value2";
        builder = builder.param("param1", value);
        Link link = builder.build();
        final Map<String, String> params = link.getParams();
        assertEquals(value, params.get("param1"));
    }

    @Path("linktest")
    public static class LinkTestResource {

        @GET
        public Response get(@Context UriInfo uriInfo) throws Exception {
            URI test1 = URI.create(uriInfo.getAbsolutePath().toString() + "test1");
            URI test2 = URI.create(uriInfo.getAbsolutePath().toString() + "test2");

            return Response.ok()
                    .link("http://oracle.com", "parent")
                    .link(new URI("http://jersey.java.net"), "framework")
                    .links(
                            Link.fromUri(uriInfo.relativize(test1)).rel("test1").build(),
                            Link.fromUri(test2).rel("test2").build(),
                            Link.fromUri(uriInfo.relativize(URI.create("linktest/test3"))).rel("test3").build()
                    ).build();
        }
    }

    @Test
    public void simpleLinkTest() {
        final WebTarget target = target("/linktest/");
        final Response response = target.request().get();
        assertThat(response.getStatus(), equalTo(200));

        final URI targetUri = target.getUri();
        assertThat(response.getLink("parent").getUri(), equalTo(URI.create("http://oracle.com")));
        assertThat(response.getLink("framework").getUri(), equalTo(URI.create("http://jersey.java.net")));

        assertThat(response.getLink("test1").getUri(), equalTo(UriTemplate.resolve(targetUri, "test1")));
        assertThat(response.getLink("test2").getUri(), equalTo(UriTemplate.resolve(targetUri, "test2")));
        assertThat(response.getLink("test3").getUri(), equalTo(UriTemplate.resolve(targetUri, "test3")));
    }
}
