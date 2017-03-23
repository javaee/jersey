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

package org.glassfish.jersey.tests.e2e.server.mvc;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Michal Gajdos
 */
public class ImplicitTemplateProgrammaticTest extends JerseyTest {

    @Template
    public static class Handler {

        private String constructor = "no-arg";

        @SuppressWarnings("UnusedDeclaration")
        public Handler() {
        }

        public Handler(final String constructor) {
            this.constructor = constructor;
        }

        @Override
        public String toString() {
            return "Resource_" + constructor;
        }
    }

    @Override
    protected Application configure() {
        Method toStringMethod = null;

        try {
            toStringMethod = Handler.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            // Eat that.
        }

        final Resource.Builder resourceBuilder = Resource.builder("implicit");

        resourceBuilder.addMethod(HttpMethod.POST).consumes("application/foo").handledBy(Handler.class, toStringMethod);
        resourceBuilder.addMethod(HttpMethod.POST).consumes("application/bar").handledBy(new Handler("arg"), toStringMethod);

        return new ResourceConfig()
                .registerResources(resourceBuilder.build())
                .register(MvcFeature.class)
                .register(TestViewProcessor.class);
    }

    @Test
    public void testImplicitHandlerClass() throws Exception {
        Properties p = new Properties();
        p.load(target("implicit").request().get(InputStream.class));
        assertEquals("/org/glassfish/jersey/tests/e2e/server/mvc/ImplicitTemplateProgrammaticTest/Handler/index.testp",
                p.getProperty("path"));
        assertEquals("Resource_no-arg", p.getProperty("model"));
    }
}
