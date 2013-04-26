/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class JsonMoxyTest extends AbstractTypeTester {

    @Path("JAXBElementListResource")
    @Produces({"application/json"})
    @Consumes({"application/json"})
    public static class JAXBElementListResource extends AResource<List<JAXBElement<String>>> {
    }

    private List<JAXBElement<String>> getJAXBElementList() {
        return Arrays.asList(getJAXBElementArray());
    }

    public static final class MoxyJsonConfigurationContextResolver implements ContextResolver<MoxyJsonConfig> {

        @Override
        public MoxyJsonConfig getContext(final Class<?> type) {
            final MoxyJsonConfig configuration = new MoxyJsonConfig();
            configuration.setIncludeRoot(true);
            return configuration;
        }
    }

    @SuppressWarnings("unchecked")
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
    @Produces({"application/json"})
    @Consumes({"application/json"})
    public static class JAXBElementArrayResource extends AResource<JAXBElement<String>[]> {
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<String>[] getJAXBElementArray() {
        return new JAXBElement[]{
                new JAXBElement(QName.valueOf("element1"), String.class, "ahoj"),
                new JAXBElement(QName.valueOf("element2"), String.class, "nazdar")
        };
    }

    @Test
    public void testJAXBElementArrayJSONRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return ((ResourceConfig) super.configure()).
                register(new MoxyJsonFeature()).
                register(new MoxyJsonConfigurationContextResolver());
    }

    @Override
    protected void configureClient(ClientConfig clientConfig) {
        super.configureClient(clientConfig);
        clientConfig.register(new MoxyJsonConfigurationContextResolver());
    }

    @Path("JAXBElementBeanJSONResource")
    @Consumes("application/json")
    @Produces("application/json")
    public static class JAXBElementBeanJSONResource extends AResource<JAXBElement<String>> {
    }

    @Test
    public void testJAXBElementBeanJSONRepresentation() {
        final WebTarget target = target("JAXBElementBeanJSONResource");

        final GenericType<JAXBElement<String>> genericType = new GenericType<JAXBElement<String>>() {};
        final GenericEntity<JAXBElement<String>> jaxbElementGenericEntity = new GenericEntity<JAXBElement<String>>(new
                JAXBElement<String>(new QName("test"), String.class, "CONTENT"), genericType.getType());

        Response rib = target.request().post(
                Entity.entity(jaxbElementGenericEntity, "application/json"));

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

    @Path("JAXBListResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBListResourceMediaType extends JAXBListResource {
    }

    @Path("JAXBListResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBListResourceJSON extends JAXBListResource {
    }

    @Test
    public void testJAXBListRepresentationJSONCollection() throws Exception {
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
    }

    @Test
    public void testJAXBListRepresentationJSONLinkedList() throws Exception {
        WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b;

        a = new LinkedList<JaxbBean>(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue<JaxbBean>) a) {
        }, "application/json"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);
    }

    @Test
    public void testJAXBListRepresentationJSONSet() throws Exception {
        WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b;

        a = new HashSet<JaxbBean>(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set<JaxbBean>) a) {
        }, "application/json"), new GenericType<Set<JaxbBean>>() {
        });
        Comparator<JaxbBean> c = new Comparator<JaxbBean>() {
            @Override
            public int compare(JaxbBean t, JaxbBean t1) {
                return t.value.compareTo(t1.value);
            }
        };
        TreeSet<JaxbBean> t1 = new TreeSet<JaxbBean>(c), t2 = new TreeSet<JaxbBean>(c);
        t1.addAll(a);
        t2.addAll(b);
        assertEquals(t1, t2);
    }

    @Test
    public void testJAXBListRepresentationJSONStack() throws Exception {
        WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b;

        Stack<JaxbBean> s = new Stack<JaxbBean>();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/json"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);
    }

    @Test
    public void testJAXBListRepresentationJSONArrayList() throws Exception {
        WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b;

        a = new MyArrayList<JaxbBean>(a);
        b = target.path("custom").request().post(Entity.entity(
                new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList<JaxbBean>) a) {}, "application/json"),
                new GenericType<MyArrayList<JaxbBean>>() {});
        assertEquals(a, b);
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
    }
}
