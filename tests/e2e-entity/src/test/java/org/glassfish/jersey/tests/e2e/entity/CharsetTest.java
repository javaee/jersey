/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import javax.xml.bind.JAXBContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.jettison.JettisonJaxbContext;
import org.glassfish.jersey.server.ResourceConfig;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.client.ClientResponse;
import org.junit.Test;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class CharsetTest extends AbstractTypeTester {

    private static String[] CHARSETS = {
            "US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16"
    };

    private static final String CONTENT = "\u00A9 CONTENT \u00FF \u2200 \u22FF";

    @Path("/StringCharsetResource")
    public static class StringCharsetResource {

        @Path("US-ASCII")
        @POST
        @Produces("text/plain;charset=US-ASCII")
        public String postUs_Ascii(String t) {
            return t;
        }

        @Path("ISO-8859-1")
        @POST
        @Produces("text/plain;charset=ISO-8859-1")
        public String postIso_8859_1(String t) {
            return t;
        }

        @Path("UTF-8")
        @POST
        @Produces("text/plain;charset=UTF-8")
        public String postUtf_8(String t) {
            return t;
        }

        @Path("UTF-16BE")
        @POST
        @Produces("text/plain;charset=UTF-16BE")
        public String postUtf_16be(String t) {
            return t;
        }

        @Path("UTF-16LE")
        @POST
        @Produces("text/plain;charset=UTF-16LE")
        public String postUtf_16le(String t) {
            return t;
        }

        @Path("UTF-16")
        @POST
        @Produces("text/plain;charset=UTF-16")
        public String postUtf_16(String t) {
            return t;
        }
    }

    @Override
    protected Application configure() {
        return ((ResourceConfig) super.configure()).register(new JettisonFeature());
    }

    @Override
    protected void configureClient(ClientConfig config) {
        super.configureClient(config);
        config.register(new JettisonFeature());
        config.register(MyJaxbContextResolver.class);
    }

    @Test
    public void testStringCharsetResource() {
        String in = "\u00A9 CONTENT \u00FF \u2200 \u22FF";

        WebTarget t = target().path("StringCharsetResource");

        for (String charset : CHARSETS) {
            Response r = t.path(charset).request().post(Entity.entity(in, "text/plain;charset=" + charset));

            byte[] inBytes = getRequestEntity();
            byte[] outBytes = getEntityAsByteArray(r);

            _verify(inBytes, outBytes);
        }
    }

    public abstract static class CharsetResource<T> {

        @Context
        HttpHeaders h;

        @POST
        @Produces("application/*")
        public Response post(T t) {
            return Response.ok(t, h.getMediaType()).build();
        }
    }

    @Path("/StringResource")
    public static class StringResource extends CharsetResource<String> {

    }

    @Test
    public void testStringRepresentation() {
        _test(CONTENT, StringResource.class);
    }

    @Path("/FormMultivaluedMapResource")
    public static class FormMultivaluedMapResource extends CharsetResource<MultivaluedMap<String, String>> {

    }

    @Test
    public void testFormMultivaluedMapRepresentation() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

        map.add("name", "\u00A9 CONTENT \u00FF \u2200 \u22FF");
        map.add("name", "� � �");
        _test(map, FormMultivaluedMapResource.class, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

    @Path("/FormResource")
    public static class FormResource extends CharsetResource<Form> {

    }

    @Test
    public void testRepresentation() {
        Form form = new Form();

        form.param("name", "\u00A9 CONTENT \u00FF \u2200 \u22FF");
        form.param("name", "� � �");
        _test(form, FormResource.class, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

    @Path("/JSONObjectResource")
    public static class JSONObjectResource extends CharsetResource<JSONObject> {

    }

    @Test
    public void testJSONObjectRepresentation() throws Exception {
        JSONObject object = new JSONObject();
        object.put("userid", 1234)
                .put("username", CONTENT)
                .put("email", "a@b")
                .put("password", "****");

        _test(object, JSONObjectResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("/JSONOArrayResource")
    public static class JSONOArrayResource extends CharsetResource<JSONArray> {

    }

    @Test
    public void testJSONArrayRepresentation() throws Exception {
        JSONArray array = new JSONArray();
        array.put(CONTENT).put("Two").put("Three").put(1).put(2.0);

        _test(array, JSONOArrayResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("/JAXBBeanResource")
    public static class JAXBBeanResource extends CharsetResource<JaxbBean> {

    }

    @Test
    public void testJAXBBeanXMLRepresentation() {
        _test(new JaxbBean(CONTENT), JAXBBeanResource.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testJAXBBeanJSONRepresentation() {
        _test(new JaxbBean(CONTENT), JAXBBeanResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Provider
    public static class MyJaxbContextResolver implements ContextResolver<JAXBContext> {

        JAXBContext context;

        public MyJaxbContextResolver() throws Exception {
            context = new JettisonJaxbContext(JaxbBean.class);
        }

        public JAXBContext getContext(Class<?> objectType) {
            return (objectType == JaxbBean.class) ? context : null;
        }
    }

    @Test
    public void testJAXBBeanJSONRepresentationWithContextResolver() throws Exception {
        JaxbBean in = new JaxbBean(CONTENT);
        WebTarget t = target("/JAXBBeanResource");
        for (String charset : CHARSETS) {
            Response rib = t.request().post(Entity.entity(in, "application/json;charset=" + charset));
            byte[] inBytes = getRequestEntity();
            byte[] outBytes = getEntityAsByteArray(rib);
            _verify(inBytes, outBytes);
        }
    }

    @Path("/ReaderResource")
    public static class ReaderResource extends CharsetResource<Reader> {

    }

    @Test
    public void testReaderRepresentation() throws Exception {
        WebTarget t = target("/ReaderResource");
        for (String charset : CHARSETS) {
            Response rib = t.request().post(Entity.entity(new StringReader(CONTENT), "text/plain;charset=" + charset));
            byte[] inBytes = getRequestEntity();
            byte[] outBytes = getEntityAsByteArray(rib);
            _verify(inBytes, outBytes);
        }
    }

    @Override
    public <T> void _test(T in, Class resource) {
        _test(in, resource, MediaType.TEXT_PLAIN_TYPE);
    }

    @Override
    public <T> void _test(T in, Class resource, MediaType m) {
        WebTarget t = target(resource.getSimpleName());
        for (String charset : CHARSETS) {
            Map<String, String> p = new HashMap<>();
            p.put("charset", charset);
            MediaType _m = new MediaType(m.getType(), m.getSubtype(), p);
            Response rib = t.request().post(Entity.entity(in, _m));
            byte[] inBytes = getRequestEntity();
            byte[] outBytes = getEntityAsByteArray(rib);
            _verify(inBytes, outBytes);
        }
    }
}
