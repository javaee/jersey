/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.ext.Provider;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@RunWith(ConcurrentRunner.class)
public class XmlMoxyTest extends AbstractTypeTester {

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
        final WebTarget target = target("JaxbBeanResource");

        final String xml = "<root>foo</root>";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
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

    @SuppressWarnings("unchecked")
    public void _testListOrArray(final boolean isList, final MediaType mt) {
        final Object in = isList ? getJAXBElementList() : getJAXBElementArray();
        final GenericType gt = isList ? new GenericType<List<JAXBElement<String>>>() {
        } : new GenericType<JAXBElement<String>[]>() {
        };

        final WebTarget target = target(isList ? "JAXBElementListResource" : "JAXBElementArrayResource");
        final Object out = target.request(mt).post(Entity.entity(new GenericEntity(in, gt.getType()), mt), gt);

        final List<JAXBElement<String>> inList =
                isList ? ((List<JAXBElement<String>>) in) : Arrays.asList((JAXBElement<String>[]) in);
        final List<JAXBElement<String>> outList = isList ? ((List<JAXBElement<String>>) out) : Arrays
                .asList((JAXBElement<String>[]) out);
        assertEquals("Lengths differ", inList.size(), outList.size());
        for (int i = 0; i < inList.size(); i++) {
            assertEquals("Names of elements at index " + i + " differ", inList.get(i).getName(), outList.get(i).getName());
            assertEquals("Values of elements at index " + i + " differ", inList.get(i).getValue(), outList.get(i).getValue());
        }
    }

    @Path("JAXBElementArrayResource")
    @Produces({"application/xml", "application/json"})
    @Consumes({"application/xml", "application/json"})
    public static class JAXBElementArrayResource extends AResource<JAXBElement<String>[]> {
    }

    private JAXBElement<String>[] getJAXBElementArray() {
        //noinspection unchecked
        return new JAXBElement[] {
                new JAXBElement(QName.valueOf("element1"), String.class, "ahoj"),
                new JAXBElement(QName.valueOf("element2"), String.class, "nazdar")
        };
    }

    @Test
    public void testJAXBElementArrayXMLRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_XML_TYPE);
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
        final WebTarget target = target("JAXBElementBeanResource");

        final String xml = "<root><value>foo";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
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
        public JaxbBean post(final JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Override
    protected Application configure() {
        return ((ResourceConfig) super.configure()).register(new MoxyXmlFeature(SimpleBean.class));
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);
        config.register(new MoxyXmlFeature(SimpleBean.class));
    }

    @Test
    public void testJAXBTypeRepresentation() {
        final WebTarget target = target("JAXBTypeResource");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/xml"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBTypeResourceMediaType extends JAXBTypeResource {
    }

    @Test
    public void testJAXBTypeRepresentationMediaType() {
        final WebTarget target = target("JAXBTypeResourceMediaType");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/foo+xml"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBObjectResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBObjectResource {

        @POST
        public Object post(final Object o) {
            return o;
        }
    }

    @Provider
    public static class JAXBObjectResolver implements ContextResolver<JAXBContext> {

        public JAXBContext getContext(final Class<?> c) {
            if (Object.class == c) {
                try {
                    return JAXBContext.newInstance(JaxbBean.class);
                } catch (final JAXBException ex) {
                    // NOOP.
                }
            }
            return null;
        }
    }

    @Test
    public void testJAXBObjectRepresentation() {
        final WebTarget target = target("JAXBObjectResource");
        final Object in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/xml"), JaxbBean.class);
        assertEquals(in, out);
    }

    @Path("JAXBObjectResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBObjectResourceMediaType extends JAXBObjectResource {
    }

    @Test
    public void testJAXBObjectRepresentationMediaType() {
        final WebTarget target = target("JAXBObjectResourceMediaType");
        final Object in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/foo+xml"), JaxbBean.class);
        assertEquals(in, out);
    }

    @Test
    public void testJAXBObjectRepresentationError() {
        final WebTarget target = target("JAXBObjectResource");

        final String xml = "<root>foo</root>";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JAXBListResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBListResource {

        @POST
        public List<JaxbBean> post(final List<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("set")
        public Set<JaxbBean> postSet(final Set<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("queue")
        public Queue<JaxbBean> postQueue(final Queue<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("stack")
        public Stack<JaxbBean> postStack(final Stack<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("custom")
        public MyArrayList<JaxbBean> postCustom(final MyArrayList<JaxbBean> l) {
            return l;
        }

        @GET
        public Collection<JaxbBean> get() {
            final ArrayList<JaxbBean> l = new ArrayList<>();
            l.add(new JaxbBean("one"));
            l.add(new JaxbBean("two"));
            l.add(new JaxbBean("three"));
            return l;
        }

        @POST
        @Path("type")
        public List<JaxbBean> postType(final Collection<JaxbBeanType> l) {
            final List<JaxbBean> beans = new ArrayList<>();
            for (final JaxbBeanType t : l) {
                beans.add(new JaxbBean(t.value));
            }
            return beans;
        }
    }

    @Path("JAXBArrayResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBArrayResource {

        @POST
        public JaxbBean[] post(final JaxbBean[] l) {
            return l;
        }

        @GET
        public JaxbBean[] get() {
            final ArrayList<JaxbBean> l = new ArrayList<>();
            l.add(new JaxbBean("one"));
            l.add(new JaxbBean("two"));
            l.add(new JaxbBean("three"));
            return l.toArray(new JaxbBean[l.size()]);
        }

        @POST
        @Path("type")
        public JaxbBean[] postType(final JaxbBeanType[] l) {
            final List<JaxbBean> beans = new ArrayList<>();
            for (final JaxbBeanType t : l) {
                beans.add(new JaxbBean(t.value));
            }
            return beans.toArray(new JaxbBean[beans.size()]);
        }
    }

    @Test
    public void testJAXBArrayRepresentation() {
        final WebTarget target = target("JAXBArrayResource");

        final JaxbBean[] a = target.request().get(JaxbBean[].class);
        JaxbBean[] b = target.request().post(Entity.entity(a, "application/xml"), JaxbBean[].class);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }

        b = target.path("type").request().post(Entity.entity(a, "application/xml"), JaxbBean[].class);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }

    @Path("JAXBListResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBListResourceMediaType extends JAXBListResource {
    }

    @Test
    public void testJAXBListRepresentationMediaType() {
        final WebTarget target = target("JAXBListResourceMediaType");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request()
                .post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {}, "application/foo+xml"),
                        new GenericType<Collection<JaxbBean>>() {});

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+xml"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new LinkedList<>(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue<JaxbBean>) a) {
        }, "application/foo+xml"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new HashSet<>(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set<JaxbBean>) a) {
        }, "application/foo+xml"), new GenericType<Set<JaxbBean>>() {
        });
        final Comparator<JaxbBean> c = new Comparator<JaxbBean>() {
            @Override
            public int compare(final JaxbBean t, final JaxbBean t1) {
                return t.value.compareTo(t1.value);
            }
        };
        final TreeSet<JaxbBean> t1 = new TreeSet<>(c);
        final TreeSet<JaxbBean> t2 = new TreeSet<>(c);
        t1.addAll(a);
        t2.addAll(b);
        assertEquals(t1, t2);

        final Stack<JaxbBean> s = new Stack<>();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/foo+xml"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList<>(a);
        b = target.path("custom").request()
                .post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList<JaxbBean>) a) {
                }, "application/foo+xml"), new GenericType<MyArrayList<JaxbBean>>() {
                });
        assertEquals(a, b);
    }

    @Test
    public void testJAXBListRepresentationError() {
        final WebTarget target = target("JAXBListResource");

        final String xml = "<root><value>foo";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class SimpleBean {

        private String value;

        public SimpleBean() {
        }

        public SimpleBean(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @XmlRootElement
    public static class ComplexJaxbBean {

        private Object simpleBean;

        public ComplexJaxbBean() {
        }

        public ComplexJaxbBean(final Object simpleBean) {
            this.simpleBean = simpleBean;
        }

        public Object getSimpleBean() {
            return simpleBean;
        }

        public void setSimpleBean(final Object simpleBean) {
            this.simpleBean = simpleBean;
        }
    }

    @Path("AdditionalClassesResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class AdditionalClassesResource {

        @GET
        public ComplexJaxbBean get() {
            return new ComplexJaxbBean(new SimpleBean("foo"));
        }
    }

    @Test
    public void testAdditionalClasses() throws Exception {
        final ComplexJaxbBean nonJaxbBean = target("AdditionalClassesResource").request().get(ComplexJaxbBean.class);
        final Object simpleBean = nonJaxbBean.getSimpleBean();

        assertThat(simpleBean, notNullValue());
        assertThat(simpleBean, instanceOf(SimpleBean.class));
        assertThat("foo", equalTo(((SimpleBean) simpleBean).getValue()));
    }
}
