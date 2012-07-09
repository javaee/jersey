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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.media.json.JsonJaxbFeature;
import org.glassfish.jersey.media.json.JsonJaxbBinder;
import org.glassfish.jersey.message.internal.FileProvider;
import org.glassfish.jersey.server.ResourceConfig;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static junit.framework.Assert.assertEquals;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class EntityTypesTest extends AbstractTypeTester {
    @Path("InputStreamResource")
    public static class InputStreamResource {
        @POST
        public InputStream post(InputStream in) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read;
            final byte[] data = new byte[2048];
            while ((read = in.read(data)) != -1)
                out.write(data, 0, read);

            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Test
    public void testInputStream() {
        ByteArrayInputStream in = new ByteArrayInputStream("CONTENT".getBytes());
        _test(in, InputStreamResource.class);
    }

    @Path("StringResource")
    public static class StringResource extends AResource<String> {
    }

    @Test
    public void testString() {
        _test("CONTENT", StringResource.class);
    }

    @Path("DataSourceResource")
    public static class DataSourceResource extends AResource<DataSource> {
    }

    @Path("ByteArrayResource")
    public static class ByteArrayResource extends AResource<byte[]> {
    }

    @Test
    public void testByteArrayRepresentation() {
        _test("CONTENT".getBytes(), ByteArrayResource.class);
    }

    @Path("JaxbBeanResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JaxbBeanResource extends AResource<JaxbBean> {
    }

    @Test
    public void testJaxbBeanRepresentation() {
        _test(new JaxbBean("CONTENT"), JaxbBeanResource.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Path("JaxbBeanResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JaxbBeanResourceMediaType extends AResource<JaxbBean> {
    }

    @Test
    public void testJaxbBeanRepresentationMediaType() {
        _test(new JaxbBean("CONTENT"), JaxbBeanResourceMediaType.class, MediaType.valueOf("application/foo+xml"));
    }

    @Test
    public void testJaxbBeanRepresentationError() {
        WebTarget target = target("JaxbBeanResource");

        String xml = "<root>foo</root>";
        Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JaxbBeanTextResource")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class JaxbBeanTextResource extends AResource<JaxbBean> {
    }

    @Test
    public void testJaxbBeanTextRepresentation() {
        _test(new JaxbBean("CONTENT"), JaxbBeanTextResource.class, MediaType.TEXT_XML_TYPE);
    }

    @Path("JAXBElementBeanResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBElementBeanResource extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    public void testJAXBElementBeanRepresentation() {
        _test(new JaxbBean("CONTENT"), JAXBElementBeanResource.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Path("JAXBElementListResource")
    @Produces({"application/xml", "application/json"})
    @Consumes({"application/xml", "application/json"})
    public static class JAXBElementListResource extends AResource<List<JAXBElement<String>>> {
    }

    private List<JAXBElement<String>> getJAXBElementList() {
        return Arrays.asList(getJAXBElementArray());
    }

    @Test
    public void testJAXBElementListXMLRepresentation() {
        _testListOrArray(true, MediaType.APPLICATION_XML_TYPE);
    }

    public void _testListOrArray(boolean isList, MediaType mt) {
        Object in = isList ? getJAXBElementList() : getJAXBElementArray();
        GenericType gt = isList ? new GenericType<List<JAXBElement<String>>>() {
        } : new GenericType<JAXBElement<String>[]>() {
        };

        WebTarget target = target(isList ? "JAXBElementListResource" : "JAXBElementArrayResource");
        Object out = target.request(mt).post(Entity.entity(new GenericEntity(in, gt.getType()), mt), gt);

        List<JAXBElement<String>> inList = isList ? ((List<JAXBElement<String>>) in) : Arrays.asList((JAXBElement<String>[]) in);
        List<JAXBElement<String>> outList = isList ? ((List<JAXBElement<String>>) out) : Arrays.asList((JAXBElement<String>[]) out);
        assertEquals("Lengths differ", inList.size(), outList.size());
        for (int i = 0; i < inList.size(); i++) {
            assertEquals("Names of elements at index " + i + " differ", inList.get(i).getName(), outList.get(i).getName());
            assertEquals("Values of elements at index " + i + " differ", inList.get(i).getValue(), outList.get(i).getValue());
        }
    }

    @Test
    public void testJAXBElementListJSONRepresentation() {
        _testListOrArray(true, MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("JAXBElementArrayResource")
    @Produces({"application/xml", "application/json"})
    @Consumes({"application/xml", "application/json"})
    public static class JAXBElementArrayResource extends AResource<JAXBElement<String>[]> {
    }

    private JAXBElement<String>[] getJAXBElementArray() {
        return new JAXBElement[]{
                new JAXBElement(QName.valueOf("element1"), String.class, "ahoj"),
                new JAXBElement(QName.valueOf("element2"), String.class, "nazdar")
        };
    }

    @Test
    public void testJAXBElementArrayXMLRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testJAXBElementArrayJSONRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("JAXBElementBeanResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBElementBeanResourceMediaType extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    public void testJAXBElementBeanRepresentationMediaType() {
        _test(new JaxbBean("CONTENT"), JAXBElementBeanResourceMediaType.class, MediaType.valueOf("application/foo+xml"));
    }

    @Test
    public void testJAXBElementBeanRepresentationError() {
        WebTarget target = target("JAXBElementBeanResource");

        String xml = "<root><value>foo";
        Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JAXBElementBeanTextResource")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class JAXBElementBeanTextResource extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    public void testJAXBElementBeanTextRepresentation() {
        _test(new JaxbBean("CONTENT"), JAXBElementBeanTextResource.class, MediaType.TEXT_XML_TYPE);
    }

    @Path("JAXBTypeResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBTypeResource {
        @POST
        public JaxbBean post(JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Override
    protected Application configure() {
        return ((ResourceConfig) super.configure()).addBinders(new JsonJaxbBinder());
    }

    @Override
    protected void configureClient(Configuration clientConfig) {
        super.configureClient(clientConfig);
        clientConfig.register(new JsonJaxbFeature());
    }

    @Test
    public void testJAXBTypeRepresentation() {
        WebTarget target = target("JAXBTypeResource");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBeanType out = target.request().post(Entity.entity(in, "application/xml"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBTypeResourceMediaType extends JAXBTypeResource {
    }

    @Test
    public void testJAXBTypeRepresentationMediaType() {
        WebTarget target = target("JAXBTypeResourceMediaType");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBeanType out = target.request().post(Entity.entity(in, "application/foo+xml"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }


    @Path("JAXBObjectResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBObjectResource {
        @POST
        public Object post(Object o) {
            return o;
        }
    }

    @Provider
    public static class JAXBObjectResolver implements ContextResolver<JAXBContext> {
        public JAXBContext getContext(Class<?> c) {
            if (Object.class == c) {
                try {
                    return JAXBContext.newInstance(JaxbBean.class);
                } catch (JAXBException ex) {
                }
            }
            return null;
        }
    }

    @Test
    public void testJAXBObjectRepresentation() {
        WebTarget target = target("JAXBObjectResource");
        Object in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/xml"), JaxbBean.class);
        assertEquals(in, out);
    }

    @Path("JAXBObjectResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBObjectResourceMediaType extends JAXBObjectResource {
    }

    @Test
    public void testJAXBObjectRepresentationMediaType() {
        WebTarget target = target("JAXBObjectResourceMediaType");
        Object in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/foo+xml"), JaxbBean.class);
        assertEquals(in, out);
    }


    @Test
    public void testJAXBObjectRepresentationError() {
        WebTarget target = target("JAXBObjectResource");

        String xml = "<root>foo</root>";
        Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("FileResource")
    public static class FileResource extends AResource<File> {
    }

    @Test
    public void testFileRepresentation() throws IOException {
        FileProvider fp = new FileProvider();
        File in = fp.readFrom(File.class, File.class, null, null, null,
                new ByteArrayInputStream("CONTENT".getBytes()));

        _test(in, FileResource.class);
    }

    @Produces("application/x-www-form-urlencoded")
    @Consumes("application/x-www-form-urlencoded")
    @Path("FormResource")
    public static class FormResource extends AResource<Form> {
    }

    @Test
    public void testFormRepresentation() {
        Form fp = new Form();
        fp.param("Email", "johndoe@gmail.com");
        fp.param("Passwd", "north 23AZ");
        fp.param("service", "cl");
        fp.param("source", "Gulp-CalGul-1.05");

        WebTarget target = target("FormResource");
        Form response = target.request().post(Entity.entity(fp, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Form.class);

        assertEquals(fp.asMap().size(), response.asMap().size());
        for (Map.Entry<String, List<String>> entry : fp.asMap().entrySet()) {
            List<String> s = response.asMap().get(entry.getKey());
            assertEquals(entry.getValue().size(), s.size());
            for (Iterator<String> it1 = entry.getValue().listIterator(), it2 = s.listIterator(); it1.hasNext();) {
                assertEquals(it1.next(), it2.next());
            }
        }
    }


    @Produces("application/json")
    @Consumes("application/json")
    @Path("JSONObjectResource")
    public static class JSONObjectResource extends AResource<JSONObject> {
    }

    @Test
    public void testJSONObjectRepresentation() throws Exception {
        JSONObject object = new JSONObject();
        object.put("userid", 1234).
                put("username", "1234").
                put("email", "a@b").
                put("password", "****");

        _test(object, JSONObjectResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Produces("application/xxx+json")
    @Consumes("application/xxx+json")
    @Path("JSONObjectResourceGeneralMediaType")
    public static class JSONObjectResourceGeneralMediaType extends AResource<JSONObject> {
    }

    @Test
    public void testJSONObjectRepresentationGeneralMediaTyp() throws Exception {
        JSONObject object = new JSONObject();
        object.put("userid", 1234).
                put("username", "1234").
                put("email", "a@b").
                put("password", "****");

        _test(object, JSONObjectResourceGeneralMediaType.class, MediaType.valueOf("application/xxx+json"));
    }

    @Produces("application/json")
    @Consumes("application/json")
    @Path("JSONOArrayResource")
    public static class JSONOArrayResource extends AResource<JSONArray> {
    }

    @Test
    public void testJSONArrayRepresentation() throws Exception {
        JSONArray array = new JSONArray();
        array.put("One").put("Two").put("Three").put(1).put(2.0);

        _test(array, JSONOArrayResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Produces("application/xxx+json")
    @Consumes("application/xxx+json")
    @Path("JSONOArrayResourceGeneralMediaType")
    public static class JSONOArrayResourceGeneralMediaType extends AResource<JSONArray> {
    }

    @Test
    public void testJSONArrayRepresentationGeneralMediaType() throws Exception {
        JSONArray array = new JSONArray();
        array.put("One").put("Two").put("Three").put(1).put(2.0);

        _test(array, JSONOArrayResourceGeneralMediaType.class, MediaType.valueOf("application/xxx+json"));
    }

//    @Path("FeedResource")
//    public static class FeedResource extends AResource<Feed> {
//    }
//
//    @Test
//    public void testFeedRepresentation() throws Exception {
//        InputStream in = this.getClass().getResourceAsStream("feed.xml");
//        AtomFeedProvider afp = new AtomFeedProvider();
//        Feed f = afp.readFrom(Feed.class, Feed.class, null, null, null, in);
//
//        _test(f, FeedResource.class);
//    }
//
//    @Path("EntryResource")
//    public static class EntryResource extends AResource<Entry> {
//    }
//
//    @Test
//    public void testEntryRepresentation() throws Exception {
//        InputStream in = this.getClass().getResourceAsStream("entry.xml");
//        AtomEntryProvider afp = new AtomEntryProvider();
//        Entry e = afp.readFrom(Entry.class, Entry.class, null, null, null, in);
//
//        _test(e, EntryResource.class);
//    }

    @Path("ReaderResource")
    public static class ReaderResource extends AResource<Reader> {
    }

    @Test
    @Ignore // TODO: Reader representation is not supported for now
    public void testReaderRepresentation() throws Exception {
        _test(new StringReader("CONTENT"), ReaderResource.class);
    }

    private final static String XML_DOCUMENT = "<n:x xmlns:n=\"urn:n\"><n:e>CONTNET</n:e></n:x>";

    @Path("StreamSourceResource")
    public static class StreamSourceResource extends AResource<StreamSource> {
    }

    @Test
    public void testStreamSourceRepresentation() throws Exception {
        StreamSource ss = new StreamSource(
                new ByteArrayInputStream(XML_DOCUMENT.getBytes()));
        _test(ss, StreamSourceResource.class);
    }

    @Path("SAXSourceResource")
    public static class SAXSourceResource extends AResource<SAXSource> {
    }

    @Test
    public void testSAXSourceRepresentation() throws Exception {
        StreamSource ss = new StreamSource(
                new ByteArrayInputStream(XML_DOCUMENT.getBytes()));
        _test(ss, SAXSourceResource.class);
    }

    @Path("DOMSourceResource")
    public static class DOMSourceResource extends AResource<DOMSource> {
    }

    @Test
    public void testDOMSourceRepresentation() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document d = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(XML_DOCUMENT)));
        DOMSource ds = new DOMSource(d);
        _test(ds, DOMSourceResource.class);
    }

    @Path("DocumentResource")
    public static class DocumentResource extends AResource<Document> {
    }

    @Test
    public void testDocumentRepresentation() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document d = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(XML_DOCUMENT)));
        _test(d, DocumentResource.class);
    }

    @Path("FormMultivaluedMapResource")
    @Produces("application/x-www-form-urlencoded")
    @Consumes("application/x-www-form-urlencoded")
    public static class FormMultivaluedMapResource {
        @POST
        public MultivaluedMap<String, String> post(MultivaluedMap<String, String> t) {
            return t;
        }
    }

    @Test
    public void testFormMultivaluedMapRepresentation() {
        MultivaluedMap<String, String> fp = new MultivaluedStringMap();
        fp.add("Email", "johndoe@gmail.com");
        fp.add("Passwd", "north 23AZ");
        fp.add("service", "cl");
        fp.add("source", "Gulp-CalGul-1.05");
        fp.add("source", "foo.java");
        fp.add("source", "bar.java");

        WebTarget target = target("FormMultivaluedMapResource");
        MultivaluedMap _fp = target.request().post(Entity.entity(fp, "application/x-www-form-urlencoded"), MultivaluedMap.class);
        assertEquals(fp, _fp);
    }

    @Path("StreamingOutputResource")
    public static class StreamingOutputResource {
        @GET
        public StreamingOutput get() {
            return new StreamingOutput() {
                public void write(OutputStream entity) throws IOException {
                    entity.write(new String("CONTENT").getBytes());
                }
            };
        }
    }

    @Test
    public void testStreamingOutputRepresentation() throws Exception {
        WebTarget target = target("StreamingOutputResource");
        assertEquals("CONTENT", target.request().get(String.class));
    }

    @Path("JAXBElementBeanJSONResource")
    @Consumes("application/json")
    @Produces("application/json")
    public static class JAXBElementBeanJSONResource extends AResource<JAXBElement<String>> {
    }

    @Test
    public void testJAXBElementBeanJSONRepresentation() {
        WebTarget target = target("JAXBElementBeanJSONResource");

        Response rib = target.request().post(
                Entity.entity(new JAXBElement<String>(new QName("test"), String.class, "CONTENT"), "application/json"));

        // TODO: the following would not be needed if i knew how to workaround JAXBElement<String>.class literal

        byte[] inBytes = requestEntity;
        byte[] outBytes = getEntityAsByteArray(rib);

        assertEquals(new String(outBytes), inBytes.length, outBytes.length);
        for (int i = 0; i < inBytes.length; i++) {
            if (inBytes[i] != outBytes[i]) {
                assertEquals("Index: " + i, inBytes[i], outBytes[i]);
            }
        }
    }

    @Path("JaxbBeanResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JaxbBeanResourceJSON extends AResource<JaxbBean> {
    }

    @Test
    public void testJaxbBeanRepresentationJSON() {
        WebTarget target = target("JaxbBeanResourceJSON");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JaxbBeanResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JaxbBeanResourceJSONMediaType extends AResource<JaxbBean> {
    }

    @Test
    public void testJaxbBeanRepresentationJSONMediaType() {
        WebTarget target = target("JaxbBeanResourceJSONMediaType");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/foo+json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBElementBeanResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBElementBeanResourceJSON extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    public void testJAXBElementBeanRepresentationJSON() {
        WebTarget target = target("JAXBElementBeanResourceJSON");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBElementBeanResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JAXBElementBeanResourceJSONMediaType extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    public void testJAXBElementBeanRepresentationJSONMediaType() {
        WebTarget target = target("JAXBElementBeanResourceJSONMediaType");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/foo+json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBTypeResourceJSON {
        @POST
        public JaxbBean post(JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Test
    public void testJAXBTypeRepresentationJSON() {
        WebTarget target = target("JAXBTypeResourceJSON");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBeanType out = target.request().post(Entity.entity(in, "application/json"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JAXBTypeResourceJSONMediaType {
        @POST
        public JaxbBean post(JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Test
    public void testJAXBTypeRepresentationJSONMediaType() {
        WebTarget target = target("JAXBTypeResourceJSONMediaType");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBeanType out = target.request().post(Entity.entity(in, "application/foo+json"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JaxbBeanResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JaxbBeanResourceFastInfoset extends AResource<JaxbBean> {
    }

    @Test
    @Ignore // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJaxbBeanRepresentationFastInfoset() {
        WebTarget target = target("JaxbBeanResourceFastInfoset");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/fastinfoset"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBElementBeanResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JAXBElementBeanResourceFastInfoset extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Ignore // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJAXBElementBeanRepresentationFastInfoset() {
        WebTarget target = target("JAXBElementBeanResourceFastInfoset");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBean out = target.request().post(Entity.entity(in, "application/fastinfoset"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JAXBTypeResourceFastInfoset {
        @POST
        public JaxbBean post(JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Test
    @Ignore // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJAXBTypeRepresentationFastInfoset() {
        WebTarget target = target("JAXBTypeResourceFastInfoset");
        JaxbBean in = new JaxbBean("CONTENT");
        JaxbBeanType out = target.request().post(Entity.entity(in, "application/fastinfoset"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }


    @Path("JAXBListResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBListResource {
        @POST
        public List<JaxbBean> post(List<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("set")
        public Set<JaxbBean> postSet(Set<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("queue")
        public Queue<JaxbBean> postQueue(Queue<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("stack")
        public Stack<JaxbBean> postStack(Stack<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("custom")
        public MyArrayList<JaxbBean> postCustom(MyArrayList<JaxbBean> l) {
            return l;
        }

        @GET
        public Collection<JaxbBean> get() {
            ArrayList<JaxbBean> l = new ArrayList<JaxbBean>();
            l.add(new JaxbBean("one"));
            l.add(new JaxbBean("two"));
            l.add(new JaxbBean("three"));
            return l;
        }

        @POST
        @Path("type")
        public List<JaxbBean> postType(Collection<JaxbBeanType> l) {
            List<JaxbBean> beans = new ArrayList<JaxbBean>();
            for (JaxbBeanType t : l)
                beans.add(new JaxbBean(t.value));
            return beans;
        }
    }

    @Path("JAXBArrayResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBArrayResource {
        @POST
        public JaxbBean[] post(JaxbBean[] l) {
            return l;
        }

        @GET
        public JaxbBean[] get() {
            ArrayList<JaxbBean> l = new ArrayList<JaxbBean>();
            l.add(new JaxbBean("one"));
            l.add(new JaxbBean("two"));
            l.add(new JaxbBean("three"));
            return l.toArray(new JaxbBean[l.size()]);
        }

        @POST
        @Path("type")
        public JaxbBean[] postType(JaxbBeanType[] l) {
            List<JaxbBean> beans = new ArrayList<JaxbBean>();
            for (JaxbBeanType t : l)
                beans.add(new JaxbBean(t.value));
            return beans.toArray(new JaxbBean[beans.size()]);
        }
    }

    @Test
    public void testJAXBArrayRepresentation() {
        WebTarget target = target("JAXBArrayResource");

        JaxbBean[] a = target.request().get(JaxbBean[].class);
        JaxbBean[] b = target.request().post(Entity.entity(a, "application/xml"), JaxbBean[].class);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], b[i]);

        b = target.path("type").request().post(Entity.entity(a, "application/xml"), JaxbBean[].class);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++)
            assertEquals(a[i], b[i]);
    }


    @Path("JAXBListResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBListResourceMediaType extends JAXBListResource {
    }

    @Test
    public void testJAXBListRepresentationMediaType() {
        WebTarget target = target("JAXBListResourceMediaType");


        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request().
                post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
                }, "application/foo+xml"),
                        new GenericType<Collection<JaxbBean>>() {
                        });

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+xml"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new LinkedList(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue) a) {
        }, "application/foo+xml"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new HashSet(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set) a) {
        }, "application/foo+xml"), new GenericType<Set<JaxbBean>>() {
        });
        Comparator<JaxbBean> c = new Comparator<JaxbBean>() {
            @Override
            public int compare(JaxbBean t, JaxbBean t1) {
                return t.value.compareTo(t1.value);
            }
        };
        TreeSet t1 = new TreeSet(c), t2 = new TreeSet(c);
        t1.addAll(a);
        t2.addAll(b);
        assertEquals(t1, t2);

        Stack s = new Stack();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/foo+xml"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList(a);
        b = target.path("custom").request().post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList) a) {
        }, "application/foo+xml"), new GenericType<MyArrayList<JaxbBean>>() {
        });
        assertEquals(a, b);
    }


    @Test
    public void testJAXBListRepresentationError() {
        WebTarget target = target("JAXBListResource");

        String xml = "<root><value>foo";
        Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JAXBListResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JAXBListResourceFastInfoset extends JAXBListResource {
    }

    /**
     * TODO, the unmarshalling fails.
     */
    @Test
    @Ignore // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJAXBListRepresentationFastInfoset() {
        WebTarget target = target("JAXBListResourceFastInfoset");

        Collection<JaxbBean> a = target.request().get(new GenericType<Collection<JaxbBean>>() {
        });

        Collection<JaxbBean> b = target.request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/fastinfoset"), new GenericType<Collection<JaxbBean>>() {
        }
        );

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/fastinfoset"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new LinkedList(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue) a) {
        }, "application/fastinfoset"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new HashSet(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set) a) {
        }, "application/fastinfoset"), new GenericType<Set<JaxbBean>>() {
        });
        Comparator<JaxbBean> c = new Comparator<JaxbBean>() {
            @Override
            public int compare(JaxbBean t, JaxbBean t1) {
                return t.value.compareTo(t1.value);
            }
        };
        TreeSet t1 = new TreeSet(c), t2 = new TreeSet(c);
        t1.addAll(a);
        t2.addAll(b);
        assertEquals(t1, t2);

        Stack s = new Stack();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/fastinfoset"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList(a);
        b = target.path("custom").request().post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList) a) {
        }, "application/fastinfoset"), new GenericType<MyArrayList<JaxbBean>>() {
        });
        assertEquals(a, b);
    }

    @Path("JAXBListResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBListResourceJSON extends JAXBListResource {
    }


    @Test
    public void testJAXBListRepresentationJSON() throws Exception {
        WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/json"), new GenericType<Collection<JaxbBean>>() {
        });

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/json"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new LinkedList(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue) a) {
        }, "application/json"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new HashSet(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set) a) {
        }, "application/json"), new GenericType<Set<JaxbBean>>() {
        });
        Comparator<JaxbBean> c = new Comparator<JaxbBean>() {
            @Override
            public int compare(JaxbBean t, JaxbBean t1) {
                return t.value.compareTo(t1.value);
            }
        };
        TreeSet t1 = new TreeSet(c), t2 = new TreeSet(c);
        t1.addAll(a);
        t2.addAll(b);
        assertEquals(t1, t2);

        Stack s = new Stack();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/json"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList(a);
        b = target.path("custom").request().post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList) a) {
        }, "application/json"), new GenericType<MyArrayList<JaxbBean>>() {
        });
        assertEquals(a, b);

        // TODO: would be nice to produce/consume a real JSON array like following
        // instead of what we have now:
//        JSONArray a = r.get(JSONArray.class);
//        JSONArray b = new JSONArray().
//                put(new JSONObject().put("value", "one")).
//                put(new JSONObject().put("value", "two")).
//                put(new JSONObject().put("value", "three"));
//        assertEquals(a.toString(), b.toString());
//        JSONArray c = r.post(JSONArray.class, b);
//        assertEquals(a.toString(), c.toString());
    }

    @Path("JAXBListResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JAXBListResourceJSONMediaType extends JAXBListResource {
    }

    @Test
    public void testJAXBListRepresentationJSONMediaType() throws Exception {
        WebTarget target = target("JAXBListResourceJSONMediaType");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+json"), new GenericType<Collection<JaxbBean>>() {
        });

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+json"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        // TODO: would be nice to produce/consume a real JSON array like following
        // instead of what we have now:
//        JSONArray a = r.get(JSONArray.class);
//        JSONArray b = new JSONArray().
//                put(new JSONObject().put("value", "one")).
//                put(new JSONObject().put("value", "two")).
//                put(new JSONObject().put("value", "three"));
//        assertEquals(a.toString(), b.toString());
//        JSONArray c = r.post(JSONArray.class, b);
//        assertEquals(a.toString(), c.toString());
    }
}
