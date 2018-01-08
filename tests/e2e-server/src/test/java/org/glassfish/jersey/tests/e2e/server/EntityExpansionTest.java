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

package org.glassfish.jersey.tests.e2e.server;

import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.SAXParserFactory;

import org.glassfish.jersey.internal.util.SaxHelper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXParseException;

/**
 * Tests properties configuring secure sax parsing.
 *
 * @author Miroslav Fuksa
 */
public class EntityExpansionTest extends JerseyTest {

    private static final Logger LOG = Logger.getLogger(EntityExpansionTest.class.getName());
    private static boolean isXdk = false;

    @Override
    protected Application configure() {
        System.setProperty("entityExpansionLimit", "10");
        System.setProperty("elementAttributeLimit", "1");

        final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class, BadRequestMapper.class);
        return resourceConfig;
    }

    public static class BadRequestMapper implements ExceptionMapper<BadRequestException> {
        @Override
        public Response toResponse(BadRequestException exception) {
            Throwable t = exception;
            while (t != null && t.getClass() != SAXParseException.class) {
                t = t.getCause();
            }
            if (t != null) {
                return Response.ok().entity("PASSED:" + t.getMessage()).build();
            }
            return Response.status(500).build();
        }
    }

    @Path("resource")
    public static class TestResource {

        @POST
        public String post(TestBean bean) {
            return bean.getInput();
        }

    }

    @XmlRootElement()
    @XmlAccessorType(value = XmlAccessType.FIELD)
    public static class TestBean {
        @XmlElement
        private String input;

        @XmlAttribute
        private String str;

        @XmlAttribute
        private String str2;

        @XmlAttribute
        private String str3;


        public String getStr2() {
            return str2;
        }

        public void setStr2(String str2) {
            this.str2 = str2;
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }


        public String getStr3() {
            return str3;
        }

        public void setStr3(String str3) {
            this.str3 = str3;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }
    }

    @BeforeClass
    public static void setXdkFlag() {
        // XDK SAXParser does not support this feature, so the test has to be skipped if XDK detected.
        if (SaxHelper.isXdkParserFactory(SAXParserFactory.newInstance())) {
            LOG.warning("XDK SAXParser detected, FEATURE_SECURE_PROCESSING is not supported. Tests will be skipped.");
            isXdk = true;
        }
        Assume.assumeTrue(!isXdk);
    }

    @Test
    public void testEntityExpansion() {
        String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "\n<!DOCTYPE lolz [\n"
                + "  <!ENTITY lol \"lollollollollollollol[...]\">\n"
                + "  <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
                + "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
                + "]>\n"
                + "<testBean><input>&lol3;</input></testBean>";

        final Response response = target().path("resource").request().post(Entity.entity(str, MediaType.APPLICATION_XML));
        Assert.assertEquals(200, response.getStatus());
        final String entity = response.readEntity(String.class);
        Assert.assertTrue(entity.startsWith("PASSED"));
    }

    @Test
    public void testMaxAttributes() {
        String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<testBean str=\"aaa\" str2=\"bbb\" str3=\"ccc\"><input>test</input></testBean>";
        final Response response = target().path("resource").request().post(Entity.entity(str, MediaType.APPLICATION_XML));
        Assert.assertEquals(200, response.getStatus());
        final String entity = response.readEntity(String.class);
        Assert.assertTrue(entity.startsWith("PASSED"));
    }

}
