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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jettison.JettisonConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.jettison.JettisonJaxbContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
@RunWith(Enclosed.class)
public class EmptyRequestWithJaxbTest {

    @SuppressWarnings("UnusedParameters")
    @Path("/")
    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class Resource {

        @POST
        public void bean(JaxbBean b) {
        }

        @Path("type")
        @POST
        public void type(JaxbBeanType b) {
        }

        @Path("list-bean")
        @POST
        public void listBean(List<JaxbBean> b) {
        }

        @Path("list-type")
        @POST
        public void listType(List<JaxbBeanType> b) {
        }

        @Path("array-bean")
        @POST
        public void arrayBean(JaxbBean[] b) {
        }

        @Path("array-type")
        @POST
        public void arrayType(JaxbBeanType[] b) {
        }

    }

    public static class EmptyRequestTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(Resource.class).register(new JettisonFeature());
        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.register(JettisonFeature.class);
        }

        @Test
        public void testEmptyJsonRequestMapped() {
            _test(target());
        }

        @Test
        public void testEmptyXmlRequest() {
            WebTarget r = target();

            Response cr = r.request().post(Entity.entity(null, "application/xml"));
            assertEquals(400, cr.getStatus());

            cr = r.path("type").request().post(Entity.entity(null, "application/xml"));
            assertEquals(400, cr.getStatus());

            cr = r.path("list-bean").request().post(Entity.entity(null, "application/xml"));
            assertEquals(400, cr.getStatus());

            cr = r.path("list-type").request().post(Entity.entity(null, "application/xml"));
            assertEquals(400, cr.getStatus());

            cr = r.path("array-bean").request().post(Entity.entity(null, "application/xml"));
            assertEquals(400, cr.getStatus());

            cr = r.path("array-type").request().post(Entity.entity(null, "application/xml"));
            assertEquals(400, cr.getStatus());
        }
    }

    @Ignore("This class is not a test class & must be ignored by the Enclosed test runner.")
    public abstract static class CR implements ContextResolver<JAXBContext> {

        private final JAXBContext context;

        private final Class[] classes = {JaxbBean.class, JaxbBeanType.class};

        private final Set<Class> types = new HashSet<>(Arrays.asList(classes));

        public CR() {
            try {
                context = configure(classes);
            } catch (JAXBException ex) {
                throw new RuntimeException(ex);
            }
        }

        protected abstract JAXBContext configure(Class[] classes) throws JAXBException;

        public JAXBContext getContext(Class<?> objectType) {
            return (types.contains(objectType)) ? context : null;
        }
    }

    public static class MappedJettisonCRTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(MappedJettisonCR.class, Resource.class).register(new JettisonFeature());
        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.register(JettisonFeature.class);
        }

        public static class MappedJettisonCR extends CR {

            protected JAXBContext configure(Class[] classes) throws JAXBException {
                return new JettisonJaxbContext(JettisonConfig.mappedJettison().build(), classes);
            }
        }

        @Test
        public void testMappedJettisonCR() {
            _test(target());
        }
    }

    public static class BadgerFishCRTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(BadgerFishCR.class, Resource.class).register(new JettisonFeature());
        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.register(JettisonFeature.class);
        }

        public static class BadgerFishCR extends CR {

            protected JAXBContext configure(Class[] classes) throws JAXBException {
                return new JettisonJaxbContext(JettisonConfig.badgerFish().build(), classes);
            }
        }

        @Test
        public void testBadgerFishCR() {
            _test(target());
        }
    }

    public static void _test(WebTarget target) {
        Response cr = target.request().post(Entity.entity(null, "application/json"));
        assertEquals(400, cr.getStatus());

        cr = target.path("type").request().post(Entity.entity(null, "application/json"));
        assertEquals(400, cr.getStatus());

        cr = target.path("list-bean").request().post(Entity.entity(null, "application/json"));
        assertEquals(400, cr.getStatus());

        cr = target.path("list-type").request().post(Entity.entity(null, "application/json"));
        assertEquals(400, cr.getStatus());

        cr = target.path("array-bean").request().post(Entity.entity(null, "application/json"));
        assertEquals(400, cr.getStatus());

        cr = target.path("array-type").request().post(Entity.entity(null, "application/json"));
        assertEquals(400, cr.getStatus());
    }
}
