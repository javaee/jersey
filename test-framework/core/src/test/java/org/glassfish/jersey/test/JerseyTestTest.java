/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
import java.security.AccessController;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * {@link org.glassfish.jersey.test.JerseyTest} unit tests.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
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
        public TestContainer create(final URI baseUri, final DeploymentContext context) throws IllegalArgumentException {
            return new TestContainer() {

                @Override
                public ClientConfig getClientConfig() {
                    return null;
                }

                @Override
                public URI getBaseUri() {
                    return baseUri;
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

    private static class MyJerseyTest extends JerseyTest {
        @Override
        protected Application configure() {
            return new ResourceConfig(MyResource.class);
        }

        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            return new MyTestContainerFactory();
        }
    }

    @Test
    public void testCustomTestContainerFactory() {
        MyJerseyTest myJerseyTest = new MyJerseyTest();

        assertEquals(myJerseyTest.getTestContainerFactory().getClass(), MyTestContainerFactory.class);
    }

    @Test
    public void testOverridePortNumber() {
        final int newPort = TestProperties.DEFAULT_CONTAINER_PORT + 1;
        MyJerseyTest myJerseyTest = new MyJerseyTest() {
            @Override
            protected Application configure() {
                forceSet(TestProperties.CONTAINER_PORT, Integer.toString(newPort));
                return super.configure();
            }
        };

        assertEquals(newPort, myJerseyTest.getPort());
    }

    @Test
    public void testThatDefaultContainerPortIsUsed() {
        MyJerseyTest myJerseyTest = new MyJerseyTest();

        String portValue = AccessController.doPrivileged(PropertiesHelper.getSystemProperty(TestProperties.CONTAINER_PORT,
                String.valueOf(TestProperties.DEFAULT_CONTAINER_PORT)));

        assertEquals(Integer.valueOf(portValue).intValue(), myJerseyTest.getPort());
    }

}
