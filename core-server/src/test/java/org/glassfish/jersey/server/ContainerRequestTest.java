/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.MapPropertiesDelegate;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Jersey container request context test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContainerRequestTest {

    private static final SecurityContext SECURITY_CONTEXT = new SecurityContext() {
        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return null;
        }
    };

    @Test
    public void testAcceptableMediaTypes() throws URISyntaxException {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.ACCEPT, "application/xml, text/plain");
        r.header(HttpHeaders.ACCEPT, "application/json");
        assertEquals(r.getAcceptableMediaTypes().size(), 3);
        assertTrue(r.getAcceptableMediaTypes().contains(MediaType.APPLICATION_XML_TYPE));
        assertTrue(r.getAcceptableMediaTypes().contains(MediaType.TEXT_PLAIN_TYPE));
        assertTrue(r.getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void testAcceptableLanguages() throws URISyntaxException {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.ACCEPT_LANGUAGE, "en-gb;q=0.8, en;q=0.7");
        r.header(HttpHeaders.ACCEPT_LANGUAGE, "de");
        assertEquals(r.getAcceptableLanguages().size(), 3);
        assertTrue(r.getAcceptableLanguages().contains(Locale.UK));
        assertTrue(r.getAcceptableLanguages().contains(Locale.ENGLISH));
        assertTrue(r.getAcceptableLanguages().contains(Locale.GERMAN));
    }

    @Test
    public void testMethod() {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        assertEquals(r.getMethod(), "GET");
    }

    @Test
    public void testUri() throws URISyntaxException {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        assertEquals(r.getRequestUri(), URI.create("http://example.org/app/resource"));
    }

    @Test
    public void testSelectVariant() {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        r.header(HttpHeaders.ACCEPT_LANGUAGE, "en");
        List<Variant> lv = Variant
                .mediaTypes(MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE)
                .languages(Locale.ENGLISH, Locale.FRENCH)
                .add().build();
        assertEquals(r.selectVariant(lv).getMediaType(), MediaType.APPLICATION_JSON_TYPE);
        assertEquals(r.selectVariant(lv).getLanguage(), Locale.ENGLISH);
    }

    @Test
    public void testPreconditionsMatch() {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.IF_MATCH, "\"686897696a7c876b7e\"");
        assertNull(r.evaluatePreconditions(new EntityTag("686897696a7c876b7e")));
        assertEquals(r.evaluatePreconditions(new EntityTag("0")).build().getStatus(),
                Response.Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testPreconditionsNoneMatch() {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.IF_NONE_MATCH, "\"686897696a7c876b7e\"");
        assertEquals(r.evaluatePreconditions(new EntityTag("686897696a7c876b7e")).build().getStatus(),
                Response.Status.NOT_MODIFIED.getStatusCode());
        assertNull(r.evaluatePreconditions(new EntityTag("000000000000000000")));
    }

    @Test
    public void testPreconditionsModified() throws ParseException {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.IF_MODIFIED_SINCE, "Sat, 29 Oct 2011 19:43:31 GMT");
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Sat, 29 Oct 2011 19:43:31 GMT");
        assertEquals(r.evaluatePreconditions(date).build().getStatus(),
                Response.Status.NOT_MODIFIED.getStatusCode());
        date = f.parse("Sat, 30 Oct 2011 19:43:31 GMT");
        assertNull(r.evaluatePreconditions(date));
    }

    @Test
    public void testPreconditionsUnModified() throws ParseException {
        ContainerRequest r = new ContainerRequest(
                URI.create("http://example.org/app"), URI.create("http://example.org/app/resource"),
                "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
        r.header(HttpHeaders.IF_UNMODIFIED_SINCE, "Sat, 29 Oct 2011 19:43:31 GMT");
        SimpleDateFormat f = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        Date date = f.parse("Sat, 29 Oct 2011 19:43:31 GMT");
        assertNull(r.evaluatePreconditions(date));
        date = f.parse("Sat, 30 Oct 2011 19:43:31 GMT");
        assertEquals(r.evaluatePreconditions(date).build().getStatus(),
                Response.Status.PRECONDITION_FAILED.getStatusCode());
    }

    private ContainerRequest getContainerRequestForPreconditionsTest() {
        return new ContainerRequest(URI.create("http://example.org"),
                    URI.create("http://example.org/app/respource"), "GET", SECURITY_CONTEXT, new MapPropertiesDelegate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEvaluatePreconditionsDateNull() {
        ContainerRequest r = getContainerRequestForPreconditionsTest();
        r.evaluatePreconditions((Date) null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testEvaluatePreconditionsEntityTagNull() {
        ContainerRequest r = getContainerRequestForPreconditionsTest();
        r.evaluatePreconditions((EntityTag) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEvaluatePreconditionsBothNull() {
        ContainerRequest r = getContainerRequestForPreconditionsTest();
        r.evaluatePreconditions(null, null);
    }
}
