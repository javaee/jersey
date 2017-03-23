/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.glassfish.jersey.server.mvc.mustache.MustacheMvcFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * MVC encoding charset tests.
 *
 * @author Miroslav Fuksa
 */
@RunWith(Parameterized.class)
public class MvcEncodingTest extends JerseyTest {

    public static final String MESSAGE = "\\u0161\\u010d\\u0159\\u017e\\u00fd\\u00e1\\u00ed\\u00e9";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new FreemarkerMvcFeature(), "freemarker", "FreemarkerResource.ftl", "UTF-8"},
                {new FreemarkerMvcFeature(), "freemarker", "FreemarkerResource.ftl", "UTF-16"},
                {new FreemarkerMvcFeature(), "freemarker", "FreemarkerResource.ftl", "windows-1250"},
                {new FreemarkerMvcFeature(), "freemarker", "FreemarkerResource.ftl", "ISO-8859-2"},
                {new MustacheMvcFeature(), "mustache", "MustacheResource.mustache", "UTF-8"},
                {new MustacheMvcFeature(), "mustache", "MustacheResource.mustache", "UTF-16"},
                {new MustacheMvcFeature(), "mustache", "MustacheResource.mustache", "windows-1250"},
                {new MustacheMvcFeature(), "mustache", "MustacheResource.mustache", "ISO-8859-2"},
        });
    }

    private static String templateName;
    private final String defaultEncoding;

    public MvcEncodingTest(Feature feature, String propertySuffix, String templateName, String defaultEncoding) {
        super(new ResourceConfig()
                .register(feature)
                .register(FreemarkerResource.class)
                .property(MvcFeature.ENCODING + "." + propertySuffix, defaultEncoding));
        MvcEncodingTest.templateName = templateName;
        this.defaultEncoding = defaultEncoding;
    }


    @Path("resource")
    public static class FreemarkerResource {
        @GET
        public Viewable get() {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("user", MESSAGE);

            return new Viewable("/org/glassfish/jersey/tests/e2e/server/mvc/MvcEncodingTest/" + templateName, map);
        }

        @GET
        @Path("textplain")
        @Produces("text/plain")
        public Viewable getTextPlain() {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("user", MESSAGE);

            return new Viewable("/org/glassfish/jersey/tests/e2e/server/mvc/MvcEncodingTest/" + templateName, map);
        }

        @GET
        @Path("textplainUTF16")
        @Produces("text/plain;charset=UTF-16")
        public Viewable getTextPlainUTF16() {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("user", MESSAGE);

            return new Viewable("/org/glassfish/jersey/tests/e2e/server/mvc/MvcEncodingTest/" + templateName, map);
        }
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyTestContainerFactory();
    }

    @Test
    public void testDefaultEncoding() {
        final Response response = target().path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("Model:" + MESSAGE, response.readEntity(String.class));
        Assert.assertEquals("*/*;charset=" + defaultEncoding, response.getMediaType().toString());
        Assert.assertEquals(defaultEncoding, response.getMediaType().getParameters().get(MediaType.CHARSET_PARAMETER));
    }

    @Test
    public void testTextPlainDefaultEncoding() {
        final Response response = target().path("resource/textplain").request("*/*,text/plain,text/html").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("Model:" + MESSAGE, response.readEntity(String.class));
        Assert.assertEquals("text/plain;charset=" + defaultEncoding, response.getMediaType().toString());
        Assert.assertEquals(defaultEncoding, response.getMediaType().getParameters().get(MediaType.CHARSET_PARAMETER));
    }

    @Test
    public void testTextPlain406() {
        final Response response = target().path("resource/textplain").request("text/html").get();
        Assert.assertEquals(406, response.getStatus());
    }

    @Test
    public void testTextPlainUTF16() {
        final Response response = target().path("resource/textplainUTF16").request("*/*,text/plain,text/html").get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("Model:" + MESSAGE, response.readEntity(String.class));
        Assert.assertEquals("text/plain;charset=UTF-16", response.getMediaType().toString());
        Assert.assertEquals("UTF-16", response.getMediaType().getParameters().get(MediaType.CHARSET_PARAMETER));
    }
}
