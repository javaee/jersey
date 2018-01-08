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
package org.glassfish.jersey.tests.e2e.inject.cdi.se.scopes;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests CDI resources.
 */
public class ScopesTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(RequestScopedResource.class, SingletonScopedResource.class);
    }

    @Test
    public void testCheckRequest() throws InterruptedException {
        String[] response1 = target().path("request").path("James").request().get(String.class).split(" ");
        String[] response2 = target().path("request").path("Marcus").request().get(String.class).split(" ");
        assertResponses("request", response1, response2);
        assertNotEquals(response1[3], response2[3]);
    }

    @Test
    public void testCheckSingleton() throws InterruptedException {
        String[] response1 = target().path("singleton").path("James").request().get(String.class).split(" ");
        String[] response2 = target().path("singleton").path("Marcus").request().get(String.class).split(" ");
        assertResponses("singleton", response1, response2);
        assertEquals(response1[3], response2[3]);
    }

    private void assertResponses(String type, String[] response1, String[] response2) {
        assertEquals("Hello_James", response1[0]);
        assertEquals("[1]", response1[1]);
        assertEquals("[" + type + "/James]", response1[2]);

        assertEquals("Hello_Marcus", response2[0]);
        assertEquals("[2]", response2[1]);
        assertEquals("[" + type + "/Marcus]", response2[2]);
    }
}
