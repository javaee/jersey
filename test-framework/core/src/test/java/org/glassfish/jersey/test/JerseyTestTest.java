/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test;

import java.net.URI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class JerseyTestTest {

    @Path("/")
    public static class MyResource {

        @GET
        public String get() {
            return "xxx";
        }
    }

    public static class MyTestContainerFactory implements TestContainerFactory {

        @Override
        public TestContainer create(URI baseUri, ApplicationHandler application) throws IllegalArgumentException {
            return new TestContainer() {

                @Override
                public ClientConfig getClientConfig() {
                    return null;
                }

                @Override
                public URI getBaseUri() {
                    return null;
                }

                @Override
                public void start() {
                }

                @Override
                public void stop() {
                }
            };
        }
    }

    public static class MyBinder extends AbstractBinder {

        @Override
        public void configure() {
            bind(MyTestContainerFactory.class).to(TestContainerFactory.class);
        }
    }

    private static class MyJerseyTest extends JerseyTest {

        private MyJerseyTest() throws TestContainerException {
            super(new ResourceConfig(MyResource.class).register(new MyBinder()));
        }
    }

    @Test
    public void testCustomTestContainerFactory() {
        MyJerseyTest myJerseyTest = new MyJerseyTest();

        assertEquals(myJerseyTest.getTestContainerFactory().getClass(), MyTestContainerFactory.class);
    }

    @Test
    public void testOverridePortNumber() {
        MyJerseyTest myJerseyTest = new MyJerseyTest();
        int newPort = TestProperties.DEFAULT_CONTAINER_PORT + 1;
        myJerseyTest.set(TestProperties.CONTAINER_PORT, newPort);

        assertEquals(newPort, myJerseyTest.getPort());
    }

    @Test
    public void testThatDefaultContainerPortIsUsed() {
        MyJerseyTest myJerseyTest = new MyJerseyTest();
        assertEquals(TestProperties.DEFAULT_CONTAINER_PORT, myJerseyTest.getPort());
    }

}
