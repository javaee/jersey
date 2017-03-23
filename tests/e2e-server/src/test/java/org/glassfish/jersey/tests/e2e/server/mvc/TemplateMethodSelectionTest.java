/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.mvc;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests that {@link Template} annotated methods are selected by the routing algorithms as if they
 * would actually return {@link Viewable} instead of the model.
 *
 * @author Miroslav Fuksa
 */
public class TemplateMethodSelectionTest extends JerseyTest {

    private static final Map<String, String> MODEL = new HashMap<String, String>() {{
        put("a", "hello");
        put("b", "world");
    }};


    @Override
    protected Application configure() {
        return new ResourceConfig(
                TemplateAnnotatedResourceMethod.class,
                TemplateAnnotatedResource.class,
                BasicResource.class,
                AsViewableResource.class,
                NoTemplateResource.class,
                LoggingFeature.class,
                MvcFeature.class,
                TestViewProcessor.class,
                MoxyJsonFeature.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(MoxyJsonFeature.class);
    }

    public static MyBean getMyBean() {
        final MyBean myBean = new MyBean();
        myBean.setName("hello");
        return myBean;
    }

    @XmlRootElement
    public static class MyBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Path("annotatedMethod")
    public static class TemplateAnnotatedResourceMethod {

        @GET
        @Produces(MediaType.TEXT_HTML)
        @Template()
        public Map<String, String> getAsHTML() {
            return MODEL;
        }

        @GET
        @Produces("application/json")
        public MyBean getAsJSON() {
            return getMyBean();
        }
    }

    @Path("noTemplate")
    public static class NoTemplateResource {

        @GET
        @Produces(MediaType.TEXT_HTML)
        public Map<String, String> getAsHTML() {
            return MODEL;
        }

        @GET
        @Produces("application/json")
        public MyBean getAsJSON() {
            return getMyBean();
        }
    }

    @Path("annotatedClass")
    @Template
    @Produces(MediaType.TEXT_HTML)
    public static class TemplateAnnotatedResource {

        @GET
        @Produces("application/json")
        public MyBean getAsJSON() {
            return getMyBean();
        }

        @Override
        public String toString() {
            return "This toString() method will be used to get model.";
        }
    }

    @Path("basic")
    public static class BasicResource {
        @GET
        @Produces(MediaType.TEXT_HTML)
        public String getAsHTML() {
            return "Hello World";
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public MyBean getAsJSON() {
            return getMyBean();
        }
    }

    @Path("viewable")
    public static class AsViewableResource {
        @GET
        @Produces(MediaType.TEXT_HTML)
        public Viewable getAsHTML() {
            return new Viewable("index.testp", MODEL);
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public MyBean getAsJSON() {
            return getMyBean();
        }
    }

    /**
     * This test makes request for text/html which is preferred. The resource defines the method
     * {@link org.glassfish.jersey.tests.e2e.server.mvc.TemplateMethodSelectionTest.TemplateAnnotatedResourceMethod#getAsHTML()}
     * which returns {@link Map} for which there is not {@link javax.ws.rs.ext.MessageBodyWriter}. The absence of the
     * writer would cause that the method would not have been selected but as the {@link Template} annotation
     * is on the method, the {@link org.glassfish.jersey.server.internal.routing.MethodSelectingRouter} considers
     * it as if this would have been {@link Viewable} instead of the {@link Map}.
     */
    @Test
    public void testAnnotatedMethodByTemplateHtml() {
        final Response response = target().path("annotatedMethod").request("text/html;q=0.8", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
        assertThat(response.readEntity(String.class),
                anyOf(containsString("{b=world, a=hello}"), containsString("{a=hello, b=world}")));
    }

    @Test
    public void testAnnotatedMethodByTemplateJson() {
        final Response response = target().path("annotatedMethod").request("text/html;q=0.6", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("hello", response.readEntity(MyBean.class).getName());
    }

    @Test
    public void testAnnotatedClassByTemplateHtml() {
        final Response response = target().path("annotatedClass").request("text/html;q=0.8", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
        assertTrue(response.readEntity(String.class).contains("model=This toString() method will be used to get model."));
    }

    @Test
    public void testAnnotatedClassByTemplateJson() {
        final Response response = target().path("annotatedClass").request("text/html;q=0.6", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("hello", response.readEntity(MyBean.class).getName());
    }

    @Test
    public void testBasicHtml() {
        final Response response = target().path("basic").request("text/html;q=0.8", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
        assertTrue(response.readEntity(String.class).contains("Hello World"));
    }

    @Test
    public void testBasicJson() {
        final Response response = target().path("basic").request("text/html;q=0.6", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("hello", response.readEntity(MyBean.class).getName());
    }

    @Test
    public void testAsViewableHtml() {
        final Response response = target().path("viewable").request("text/html;q=0.8", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
        assertThat(response.readEntity(String.class),
                anyOf(containsString("{b=world, a=hello}"), containsString("{a=hello, b=world}")));
    }

    @Test
    public void testAsViewableJson() {
        final Response response = target().path("viewable").request("text/html;q=0.6", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("hello", response.readEntity(MyBean.class).getName());
    }

    /**
     * This test verifies that there is really no {@link javax.ws.rs.ext.MessageBodyWriter}
     * for {@code Map<String,String>}}. text/html is requested but application/json is chosen there is no
     * MBW for {@code Map}.
     */
    @Test
    public void testNoTemplateHtml() {
        final Response response = target().path("noTemplate").request("text/html;q=0.9", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("hello", response.readEntity(MyBean.class).getName());
    }

    @Test
    public void testNoTemplateJson() {
        final Response response = target().path("noTemplate").request("text/html;q=0.6", "application/json;q=0.7").get();
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals("hello", response.readEntity(MyBean.class).getName());
    }


}
