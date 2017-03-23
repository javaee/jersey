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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
@RunWith(Enclosed.class)
public class JAXBContextResolverTest {

    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JAXBContextResolver implements ContextResolver<JAXBContext> {

        private JAXBContext context;
        private int invoked;

        public JAXBContextResolver() {
            try {
                this.context = JAXBContext.newInstance(JaxbBean.class);
            } catch (JAXBException ex) {
                throw new RuntimeException(ex);
            }
        }

        public JAXBContext getContext(Class<?> c) {
            if (JaxbBean.class == c) {
                invoked++;
                return context;
            } else {
                return null;
            }
        }

        public int invoked() {
            return invoked;
        }
    }

    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class MarshallerResolver implements ContextResolver<Marshaller> {

        private JAXBContext context;
        private int invoked;

        public MarshallerResolver() {
            try {
                this.context = JAXBContext.newInstance(JaxbBean.class);
            } catch (JAXBException ex) {
                throw new RuntimeException(ex);
            }
        }

        public Marshaller getContext(Class<?> c) {
            if (JaxbBean.class == c) {
                invoked++;
                try {
                    return context.createMarshaller();
                } catch (JAXBException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                return null;
            }
        }

        public int invoked() {
            return invoked;
        }
    }

    @Provider
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class UnmarshallerResolver implements ContextResolver<Unmarshaller> {

        private JAXBContext context;
        private int invoked;

        public UnmarshallerResolver() {
            try {
                this.context = JAXBContext.newInstance(JaxbBean.class);
            } catch (JAXBException ex) {
                throw new RuntimeException(ex);
            }
        }

        public Unmarshaller getContext(Class<?> c) {
            if (JaxbBean.class == c) {
                invoked++;
                try {
                    return context.createUnmarshaller();
                } catch (JAXBException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                return null;
            }
        }

        public int invoked() {
            return invoked;
        }
    }

    @Path("/")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JaxbBeanResource {

        @POST
        public JaxbBean get(JaxbBean b) {
            return b;
        }
    }

    public static class JAXBContextTest extends AbstractTypeTester {

        private JAXBContextResolver cr;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            return new ResourceConfig(JaxbBeanResource.class).registerInstances(cr);
        }

        @Test
        public void testJAXBContext() throws Exception {
            final Response response = target().request(MediaType.APPLICATION_XML_TYPE).post(Entity.xml(new JaxbBean("foo")));
            assertThat(response.getMediaType(), equalTo(MediaType.APPLICATION_XML_TYPE));

            final JaxbBean foo = response.readEntity(JaxbBean.class);
            assertThat(foo.value, equalTo("foo"));
            assertThat(cr.invoked(), equalTo(2));
        }
    }

    public static class UnmarshallerTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private MarshallerResolver mr;
        private UnmarshallerResolver umr;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            mr = new MarshallerResolver();
            umr = new UnmarshallerResolver();
            return new ResourceConfig(JaxbBeanResource.class).registerInstances(cr, mr, umr);
        }

        @Test
        public void testUnmarshaller() throws Exception {
            final Response response = target().request(MediaType.APPLICATION_XML_TYPE).post(Entity.xml(new JaxbBean("foo")));
            assertThat(response.getMediaType(), equalTo(MediaType.APPLICATION_XML_TYPE));

            final JaxbBean foo = response.readEntity(JaxbBean.class);
            assertThat(foo.value, equalTo("foo"));
            assertThat(cr.invoked(), equalTo(0));
            assertThat(mr.invoked(), equalTo(1));
            assertThat(umr.invoked(), equalTo(1));
        }
    }

    @Provider
    @Produces("application/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JAXBContextResolverApp extends JAXBContextResolver {
    }

    @Provider
    @Produces("application/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class MarshallerResolverApp extends MarshallerResolver {
    }

    @Provider
    @Produces("application/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class UnmarshallerResolverApp extends UnmarshallerResolver {
    }

    @Path("/")
    @Consumes("application/xml")
    @Produces("application/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JaxbBeanResourceApp extends JaxbBeanResource {
    }

    public static class JAXBContextAppTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private JAXBContextResolverApp crApp;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            crApp = new JAXBContextResolverApp();
            return new ResourceConfig(JaxbBeanResourceApp.class).registerInstances(cr, crApp);
        }

        @Test
        public void testJAXBContextApp() throws Exception {
            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "application/xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(2, crApp.invoked());
        }
    }

    public static class UnmarshallerAppTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private MarshallerResolver mr;
        private UnmarshallerResolver umr;
        private MarshallerResolverApp mrApp;
        private UnmarshallerResolverApp umrApp;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            mr = new MarshallerResolver();
            umr = new UnmarshallerResolver();
            mrApp = new MarshallerResolverApp();
            umrApp = new UnmarshallerResolverApp();
            return new ResourceConfig(JaxbBeanResourceApp.class).registerInstances(cr, mr, umr, mrApp, umrApp);
        }

        @Test
        public void testUnmarshallerApp() throws Exception {
            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "application/xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, mr.invoked());
            assertEquals(0, umr.invoked());
            assertEquals(1, mrApp.invoked());
            assertEquals(1, umrApp.invoked());

            assertEquals("foo", target().request()
                    .post(Entity.entity(new JaxbBean("foo"), "application/xml;charset=UTF-8"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, mr.invoked());
            assertEquals(0, umr.invoked());
            assertEquals(2, mrApp.invoked());
            assertEquals(2, umrApp.invoked());
        }
    }

    @Provider
    @Produces("text/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JAXBContextResolverText extends JAXBContextResolver {
    }

    @Provider
    @Produces("text/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class MarshallerResolverText extends MarshallerResolver {
    }

    @Provider
    @Produces("text/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class UnmarshallerResolverText extends UnmarshallerResolver {
    }

    @Path("/")
    @Consumes("text/xml")
    @Produces("text/xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JaxbBeanResourceText extends JaxbBeanResource {
    }

    public static class JAXBContextTextTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private JAXBContextResolverText crText;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            crText = new JAXBContextResolverText();
            return new ResourceConfig(JaxbBeanResourceText.class).registerInstances(cr, crText);
        }

        @Test
        public void testJAXBContextText() throws Exception {
            assertEquals("foo", target().request().post(Entity.entity(new JaxbBean("foo"), "text/xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(2, crText.invoked());
        }
    }

    public static class UnmarshallerTextTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private MarshallerResolver mr;
        private UnmarshallerResolver umr;
        private MarshallerResolverText mrText;
        private UnmarshallerResolverText umrText;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            mr = new MarshallerResolver();
            umr = new UnmarshallerResolver();
            mrText = new MarshallerResolverText();
            umrText = new UnmarshallerResolverText();
            return new ResourceConfig(JaxbBeanResourceText.class).registerInstances(cr, mr, umr, mrText, umrText);
        }

        @Test
        public void testUnmarshallerText() throws Exception {
            assertEquals("foo", target().request().post(Entity.entity(new JaxbBean("foo"), "text/xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, mr.invoked());
            assertEquals(0, umr.invoked());
            assertEquals(1, mrText.invoked());
            assertEquals(1, umrText.invoked());
        }
    }

    @Provider
    @Produces("text/foo+xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class MarshallerResolverFoo extends MarshallerResolver {
    }

    @Provider
    @Produces("text/foo+xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class UnmarshallerResolverFoo extends UnmarshallerResolver {
    }

    @Path("/")
    @Consumes("text/foo+xml")
    @Produces("text/foo+xml")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JaxbBeanResourceFoo extends JaxbBeanResource {
    }

    public static class UnmarshallerFooTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private MarshallerResolver mr;
        private UnmarshallerResolver umr;
        private MarshallerResolverFoo mrFoo;
        private UnmarshallerResolverFoo umrFoo;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            mr = new MarshallerResolver();
            umr = new UnmarshallerResolver();
            mrFoo = new MarshallerResolverFoo();
            umrFoo = new UnmarshallerResolverFoo();
            return new ResourceConfig(JaxbBeanResourceFoo.class).registerInstances(cr, mr, umr, mrFoo, umrFoo);
        }

        @Test
        public void testUnmarshallerFoo() throws Exception {
            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "text/foo+xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, mr.invoked());
            assertEquals(0, umr.invoked());
            assertEquals(1, mrFoo.invoked());
            assertEquals(1, umrFoo.invoked());

            assertEquals("foo", target().request()
                    .post(Entity.entity(new JaxbBean("foo"), "text/foo+xml;charset=UTF-8"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, mr.invoked());
            assertEquals(0, umr.invoked());
            assertEquals(2, mrFoo.invoked());
            assertEquals(2, umrFoo.invoked());
        }
    }

    @Path("/")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JaxbBeanResourceAll {

        @POST
        @Consumes("application/foo+xml")
        @Produces("application/foo+xml")
        public JaxbBean get(JaxbBean b) {
            return b;
        }

        @POST
        @Consumes("application/xml")
        @Produces("application/xml")
        public JaxbBean getApp(JaxbBean b) {
            return b;
        }

        @POST
        @Consumes("text/xml")
        @Produces("text/xml")
        public JaxbBean getText(JaxbBean b) {
            return b;
        }
    }

    public static class JAXBContextAllTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private JAXBContextResolverApp crApp;
        private JAXBContextResolverText crText;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            crApp = new JAXBContextResolverApp();
            crText = new JAXBContextResolverText();
            return new ResourceConfig(JaxbBeanResourceAll.class).registerInstances(cr, crApp, crText);
        }

        @Test
        public void testJAXBContextAll() throws Exception {
            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "application/foo+xml"), JaxbBean.class).value);
            assertEquals(2, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());

            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "application/xml"), JaxbBean.class).value);
            assertEquals(2, cr.invoked());
            assertEquals(2, crApp.invoked());
            assertEquals(0, crText.invoked());

            assertEquals("foo", target().request().post(Entity.entity(new JaxbBean("foo"), "text/xml"), JaxbBean.class).value);
            assertEquals(2, cr.invoked());
            assertEquals(2, crApp.invoked());
            assertEquals(2, crText.invoked());
        }
    }

    public static class UnmarshallerAllTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private JAXBContextResolverApp crApp;
        private JAXBContextResolverText crText;
        private MarshallerResolver mr;
        private UnmarshallerResolver umr;
        private MarshallerResolverApp mrApp;
        private UnmarshallerResolverApp umrApp;
        private MarshallerResolverText mrText;
        private UnmarshallerResolverText umrText;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            crApp = new JAXBContextResolverApp();
            crText = new JAXBContextResolverText();
            mr = new MarshallerResolver();
            umr = new UnmarshallerResolver();
            mrApp = new MarshallerResolverApp();
            umrApp = new UnmarshallerResolverApp();
            mrText = new MarshallerResolverText();
            umrText = new UnmarshallerResolverText();
            return new ResourceConfig(JaxbBeanResourceAll.class).registerInstances(cr, crApp, crText, mr, umr,
                    mrApp, umrApp, mrText, umrText);
        }

        @Test
        public void testUnmarshallerAll() throws Exception {
            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "application/foo+xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());
            assertEquals(1, mr.invoked());
            assertEquals(1, umr.invoked());
            assertEquals(0, mrApp.invoked());
            assertEquals(0, umrApp.invoked());
            assertEquals(0, mrText.invoked());
            assertEquals(0, umrText.invoked());

            assertEquals("foo",
                    target().request().post(Entity.entity(new JaxbBean("foo"), "application/xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());
            assertEquals(1, mr.invoked());
            assertEquals(1, umr.invoked());
            assertEquals(1, mrApp.invoked());
            assertEquals(1, umrApp.invoked());
            assertEquals(0, mrText.invoked());
            assertEquals(0, umrText.invoked());

            assertEquals("foo", target().request().post(Entity.entity(new JaxbBean("foo"), "text/xml"), JaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());
            assertEquals(1, mr.invoked());
            assertEquals(1, umr.invoked());
            assertEquals(1, mrApp.invoked());
            assertEquals(1, umrApp.invoked());
            assertEquals(1, mrText.invoked());
            assertEquals(1, umrText.invoked());
        }
    }

    @XmlRootElement
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class OtherJaxbBean {

        public String value;

        public OtherJaxbBean() {
        }

        public OtherJaxbBean(String str) {
            value = str;
        }

        public boolean equals(Object o) {
            return o instanceof JaxbBean && ((JaxbBean) o).value.equals(value);
        }

        public String toString() {
            return "JAXBClass: " + value;
        }
    }

    @Path("/")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class JaxbBeanResourceAllOtherJaxbBean {

        @POST
        @Consumes("application/foo+xml")
        @Produces("application/foo+xml")
        public OtherJaxbBean get(OtherJaxbBean b) {
            return b;
        }

        @POST
        @Consumes("application/xml")
        @Produces("application/xml")
        public OtherJaxbBean getApp(OtherJaxbBean b) {
            return b;
        }

        @POST
        @Consumes("text/xml")
        @Produces("text/xml")
        public OtherJaxbBean getText(OtherJaxbBean b) {
            return b;
        }
    }

    public static class JAXBContextAllWithOtherJaxbBeanTest extends AbstractTypeTester {

        private JAXBContextResolver cr;
        private JAXBContextResolverApp crApp;
        private JAXBContextResolverText crText;

        @Override
        protected Application configure() {
            cr = new JAXBContextResolver();
            crApp = new JAXBContextResolverApp();
            crText = new JAXBContextResolverText();
            return new ResourceConfig(JaxbBeanResourceAllOtherJaxbBean.class).registerInstances(cr, crApp, crText);
        }

        @Test
        public void testJAXBContextAllWithOtherJaxbBean() throws Exception {
            assertEquals("foo", target().request()
                    .post(Entity.entity(new OtherJaxbBean("foo"), "application/foo+xml"), OtherJaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());

            assertEquals("foo", target().request()
                    .post(Entity.entity(new OtherJaxbBean("foo"), "application/xml"), OtherJaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());

            assertEquals("foo",
                    target().request().post(Entity.entity(new OtherJaxbBean("foo"), "text/xml"), OtherJaxbBean.class).value);
            assertEquals(0, cr.invoked());
            assertEquals(0, crApp.invoked());
            assertEquals(0, crText.invoked());
        }
    }
}
