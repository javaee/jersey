/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.filter;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests injections into provider instances.
 *
 * @author Miroslav Fuksa
 * @author Michal Gajdos
 */
public class ClientProviderInstanceInjectionTest {

    public static class MyInjectee {

        private final String value;

        public MyInjectee(final String value) {
            this.value = value;
        }

        public String getSomething() {
            return value;
        }
    }

    public static class MyInjecteeBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(new MyInjectee("hello"));
        }
    }

    public static class MyFilter implements ClientRequestFilter {

        private final Object field;

        @Inject
        private MyInjectee myInjectee;

        public MyFilter(Object field) {
            this.field = field;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok(myInjectee + "," + field).build());
        }
    }

    public static class MyFilterFeature implements Feature {

        @Inject
        private ServiceLocator locator;

        @Override
        public boolean configure(final FeatureContext context) {
            context.register(new MyFilter(locator));
            return true;
        }
    }

    /**
     * Tests that instance of a feature or other provider will not be injected on the client-side.
     */
    @Test
    public void test() {
        final Client client = ClientBuilder.newBuilder()
                .register(new MyFilterFeature())
                .register(new MyInjecteeBinder())
                .build();
        final Response response = client.target("http://foo.bar").request().get();

        assertEquals(200, response.getStatus());
        assertEquals("null,null", response.readEntity(String.class));
    }
}
