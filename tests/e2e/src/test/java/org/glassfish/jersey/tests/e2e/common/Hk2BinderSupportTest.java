/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests that HK2Binder and Jersey Binder work together.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class Hk2BinderSupportTest extends JerseyTest {

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig();
        config.register(HelloWorldResource.class);
        config.register(new Hk2Binder());
        config.register(new JerseyBinder());
        return config;
    }

    @Test
    public void testResponse() {
        String s = target().path("helloworld").request().get(String.class);
        assertEquals(Hk2Binder.HK2_HELLO_MESSAGE + "/" + JerseyBinder.JERSEY_HELLO_MESSAGE, s);
    }

    private static class Hk2Binder extends AbstractBinder {
        private static final String HK2_HELLO_MESSAGE = "Hello HK2!";

        @Override
        protected void configure() {
            bind(HK2_HELLO_MESSAGE).to(String.class).named("hk2");
        }
    }

    private static class JerseyBinder extends org.glassfish.jersey.internal.inject.AbstractBinder {
        private static final String JERSEY_HELLO_MESSAGE = "Hello Jersey!";

        @Override
        protected void configure() {
            bind(JERSEY_HELLO_MESSAGE).to(String.class).named("jersey");
        }
    }

    @Path("helloworld")
    public static class HelloWorldResource {

        @Inject
        @Named("hk2")
        private String hk2Hello;

        @Inject
        @Named("jersey")
        private String jerseyHello;

        @GET
        public String getHello() {
            return hk2Hello + "/" + jerseyHello;
        }
    }
}
