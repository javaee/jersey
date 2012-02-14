/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Target;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.RequestFilter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientTest {

    private Client client;

    public ClientTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        this.client = (Client) ClientFactory.newClient();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateClient() {
        assertNotNull(client);
    }

    @Test
    public void testClose() {
        client.close();
        assertTrue(client.isClosed());
        client.close(); // closing multiple times is ok

        try {
            client.configuration();
            fail("IllegalStateException expected if a method is called on a closed client instance.");
        } catch (IllegalStateException ex) {
            // ignored
        }
        try {
            client.target("http://jersey.java.net/examples");
            fail("IllegalStateException expected if a method is called on a closed client instance.");
        } catch (IllegalStateException ex) {
            // ignored
        }
    }

    @Test
    public void testConfiguration() {
        final Configuration configuration = client.configuration();
        assertNotNull(configuration);

        configuration.setProperty("hello", "world");

        assertEquals("world", client.configuration().getProperty("hello"));
    }

    @Test
    public void testTarget() {
        final Target target = client.target("http://jersey.java.net/examples");
        assertNotNull(target);
        assertEquals(client.configuration(), target.configuration());
    }

    @Test
    public void testTargetConfigUpdate() {
        final Target target = client.target("http://jersey.java.net/examples");
        target.configuration().register(new RequestFilter() {

            @Override
            public void preFilter(FilterContext context) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        assertEquals(1, target.configuration().getProviderInstances().size());
    }
}
