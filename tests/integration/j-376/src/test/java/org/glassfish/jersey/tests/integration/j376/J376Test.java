/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.j376;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class J376Test {
    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(GrizzlyApp.getBaseUri());

    @BeforeClass
    public static void setUpTest() {
        GrizzlyApp.start();
    }

    @AfterClass
    public static void tearDownTest() {
        GrizzlyApp.stop();
    }

    @Test
    public void testConstructorInjection() {
        final String response = target.path("constructor").request().post(Entity.entity("name=John&age=32",
                MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        assertEquals("John:32:Hello:constructor", response);
    }

    @Test
    public void testFieldInjection() {
        final String response = target.path("field").request().post(Entity.entity("name=Bill&age=21",
                MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        assertEquals("Bill:21:Hello:field", response);
    }

    @Test
    public void testMethodInjection() {
        final String response = target.path("method").request().post(Entity.entity("name=Mike&age=42",
                MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        assertEquals("Mike:42:Hello:method", response);
    }

    @Test
    public void testAppScopedBeanInReqScopedResource() {
        final String response = target.path("field/appScoped").request().get(String.class);
        assertEquals("ApplicationScopedBean:Hello", response);
    }

    @Test
    public void testAppScopedResource() {
        String response = target.path("appScope/msg").request().get(String.class);
        assertEquals("ApplicationScopedBean:Hello", response);
        response = target.path("appScope/uri").request().get(String.class);
        assertEquals("appScope/uri", response);
        response = target.path("appScope/req").request().get(String.class);
        assertEquals("Hello", response);
    }

    @Test
    public void testBeanParamInAppScoped() {
        final String response = target.path("appScope").request().post(Entity.entity("name=John&age=35",
                MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        assertEquals("John:35:Hello:appScope", response);
    }

    @Test
    public void testContextInjectionInAppScopedBean() {
        final String response = target.path("field/appScopedUri").request().get(String.class);
        assertEquals("field/appScopedUri", response);

    }
}
