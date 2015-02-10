/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.wadl.config;

import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerLocatorFactory;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;

import org.glassfish.hk2.api.ServiceLocator;

import org.junit.Assert;
import org.junit.Test;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Method;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * TODO: DESCRIBE ME<br>
 * Created on: Aug 2, 2008<br>
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 * @author Miroslav Fuksa
 */
public class WadlGeneratorConfigurationLoaderTest {


    @Test
    public void testLoadConfigClass() throws URISyntaxException {
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.WADL_GENERATOR_CONFIG,
                MyWadlGeneratorConfig.class.getName());

        final ServiceLocator locator = ServerLocatorFactory.createLocator(resourceConfig.getProperties());
        final WadlGenerator wadlGenerator = WadlGeneratorConfigLoader.loadWadlGeneratorsFromConfig(resourceConfig.getProperties())
                .createWadlGenerator(locator);
        Assert.assertEquals(MyWadlGenerator.class, wadlGenerator.getClass());

    }

    @Test
    public void testLoadConfigInstance() {
        final WadlGeneratorConfig config = WadlGeneratorConfig.generator(MyWadlGenerator.class).build();

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property(ServerProperties.WADL_GENERATOR_CONFIG, config);
        final ServiceLocator locator = ServerLocatorFactory.createLocator(resourceConfig.getProperties());
        final WadlGenerator wadlGenerator = WadlGeneratorConfigLoader.loadWadlGeneratorsFromConfig(resourceConfig.getProperties())
                .createWadlGenerator(locator);
        Assert.assertTrue(wadlGenerator instanceof MyWadlGenerator);
    }

    public static class MyWadlGenerator implements WadlGenerator {

        private String _foo;

        /**
         * @return the foo
         */
        public String getFoo() {
            return _foo;
        }

        /**
         * @param foo the foo to set
         */
        public void setFoo(String foo) {
            _foo = foo;
        }

        public Application createApplication() {
            return null;
        }

        public Method createMethod(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
            return null;
        }

        public Request createRequest(org.glassfish.jersey.server.model.Resource r,
                                     ResourceMethod m) {
            return null;
        }

        public Param createParam(org.glassfish.jersey.server.model.Resource r,
                                 ResourceMethod m, Parameter p) {
            return null;
        }

        public Representation createRequestRepresentation(
                org.glassfish.jersey.server.model.Resource r, ResourceMethod m,
                MediaType mediaType) {
            return null;
        }

        public Resource createResource(org.glassfish.jersey.server.model.Resource r, String path) {
            return null;
        }

        public Resources createResources() {
            return null;
        }

        public List<Response> createResponses(org.glassfish.jersey.server.model.Resource r,
                                              ResourceMethod m) {
            return null;
        }

        public String getRequiredJaxbContextPath() {
            return null;
        }

        public void init() {

        }

        public void setWadlGeneratorDelegate(WadlGenerator delegate) {
        }


        @Override
        public ExternalGrammarDefinition createExternalGrammar() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void attachTypes(ApplicationDescription egd) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }


    public static class MyWadlGeneratorConfig extends WadlGeneratorConfig {

        @Override
        public List<WadlGeneratorDescription> configure() {
            return generator(MyWadlGenerator.class)
                    .prop("foo", "bar")
                    .descriptions();
        }
    }

}
