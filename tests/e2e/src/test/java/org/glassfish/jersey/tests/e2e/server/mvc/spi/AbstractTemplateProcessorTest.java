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

package org.glassfish.jersey.tests.e2e.server.mvc.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.AbstractTemplateProcessor;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Michal Gajdos
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        AbstractTemplateProcessorTest.FactoryInstanceTest.class,
        AbstractTemplateProcessorTest.FactoryInstanceNegativeTest.class,
        AbstractTemplateProcessorTest.FactoryClassTest.class,
        AbstractTemplateProcessorTest.FactoryClassNegativeTest.class,
        AbstractTemplateProcessorTest.FactoryClassNameTest.class,
        AbstractTemplateProcessorTest.FactoryClassNameNegativeTest.class,
        AbstractTemplateProcessorTest.CachePositiveTest.class,
        AbstractTemplateProcessorTest.CachePositiveStringTest.class,
        AbstractTemplateProcessorTest.CacheNegativeTest.class,
        AbstractTemplateProcessorTest.CacheInvalidTest.class
})
public class AbstractTemplateProcessorTest {

    @Path("/")
    public static class Resource {

        @GET
        @Template
        public String get() {
            return "ko";
        }
    }

    public static class TestFactory {

        private final String value;

        public TestFactory() {
            this("Injected Test Factory");
        }

        public TestFactory(final String value) {
            this.value = value;
        }
    }

    public static class FactoryTemplateProcessor extends AbstractTemplateProcessor<String> {

        private final TestFactory factory;

        @Inject
        public FactoryTemplateProcessor(Configuration config, InjectionManager injectionManager) {
            super(config, injectionManager.getInstance(ServletContext.class), "factory", "fct");

            this.factory = getTemplateObjectFactory(injectionManager::createAndInitialize, TestFactory.class, Values.lazy(
                    (Value<TestFactory>) () -> new TestFactory("Default Test Factory")));
        }

        @Override
        protected String resolve(final String templatePath, final Reader reader) throws Exception {
            return factory.value;
        }

        @Override
        public void writeTo(final String templateReference, final Viewable viewable, final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders, final OutputStream out)
                throws IOException {
            out.write(templateReference.getBytes());
        }
    }

    public abstract static class FactoryTest extends JerseyTest {

        private final String expectedMessage;

        FactoryTest(final Object factory, final String expectedMessage) throws TestContainerException {
            super(new ResourceConfig(Resource.class)
                    .register(MvcFeature.class)
                    .register(FactoryTemplateProcessor.class)
                    .property(MvcFeature.TEMPLATE_OBJECT_FACTORY + ".factory", factory));

            this.expectedMessage = expectedMessage;
        }

        @Test
        public void testFactory() throws Exception {
            final Response response = target().request().get();

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is(expectedMessage));
        }
    }

    public static class FactoryInstanceTest extends FactoryTest {

        public FactoryInstanceTest() throws TestContainerException {
            super(new TestFactory(), "Injected Test Factory");
        }
    }

    public static class FactoryInstanceNegativeTest extends FactoryTest {

        public FactoryInstanceNegativeTest() throws TestContainerException {
            super("Default Test Factory", "Default Test Factory");
        }
    }

    public static class FactoryClassTest extends FactoryTest {

        public FactoryClassTest() throws TestContainerException {
            super(TestFactory.class, "Injected Test Factory");
        }
    }

    public static class FactoryClassNegativeTest extends FactoryTest {

        public FactoryClassNegativeTest() throws TestContainerException {
            super(String.class, "Default Test Factory");
        }
    }

    public static class FactoryClassNameTest extends FactoryTest {

        public FactoryClassNameTest() throws TestContainerException {
            super(TestFactory.class.getName(), "Injected Test Factory");
        }
    }

    public static class FactoryClassNameNegativeTest extends FactoryTest {

        public FactoryClassNameNegativeTest() throws TestContainerException {
            super(String.class.getName(), "Default Test Factory");
        }
    }

    public static class CacheTemplateProcessor extends AbstractTemplateProcessor<String> {

        private int i = 0;

        @Inject
        public CacheTemplateProcessor(Configuration config, InjectionManager injectionManager) {
            super(config, injectionManager.getInstance(ServletContext.class), "factory", "fct");
        }

        @Override
        protected String resolve(final String templatePath, final Reader reader) throws Exception {
            return "" + i++;
        }

        @Override
        public void writeTo(final String templateReference, final Viewable viewable, final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream out) throws IOException {
            out.write(templateReference.getBytes());
        }
    }

    public abstract static class CacheTest extends JerseyTest {

        private final boolean cache;

        CacheTest(final Object cache) throws TestContainerException {
            super(new ResourceConfig(Resource.class)
                    .register(MvcFeature.class)
                    .register(CacheTemplateProcessor.class)
                    .property(MvcFeature.CACHE_TEMPLATES + ".factory", cache));

            this.cache = Boolean.valueOf(cache.toString());
        }

        @Test
        public void testCache() throws Exception {
            Response response = target().request().get();

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is("0"));

            response = target().request().get();

            assertThat(response.getStatus(), is(200));
            assertThat(response.readEntity(String.class), is(cache ? "0" : "1"));
        }
    }

    public static class CachePositiveTest extends CacheTest {

        public CachePositiveTest() throws TestContainerException {
            super(true);
        }
    }

    public static class CachePositiveStringTest extends CacheTest {

        public CachePositiveStringTest() throws TestContainerException {
            super("true");
        }
    }

    public static class CacheNegativeTest extends CacheTest {

        public CacheNegativeTest() throws TestContainerException {
            super(false);
        }
    }

    public static class CacheInvalidTest extends CacheTest {

        public CacheInvalidTest() throws TestContainerException {
            super("invalid");
        }
    }
}
