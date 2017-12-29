/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.proxy;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Tests for https://java.net/jira/browse/JERSEY-3023 fix (work-around).
 *
 * The overloaded factory methods for {@code WebResourceFactory} allows
 * the developer to pass the resource method parameter index of the entity
 * if necessary.
 */
public class WebResourceFactoryEntityIndexTest extends JerseyTest {

    private static final String NAME = "Jersey";

    final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<String, Object>() {
        {
            add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        }
    };
    final List<Cookie> cookies = Collections.<Cookie>emptyList();

    @Override
    protected ResourceConfig configure() {
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory
        // mvn test -Djersey.config.test.container.factory=org.glassfish.jersey.test.simple.SimpleTestContainerFactory
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(MyResource.class);
    }

    /**
     * <pre>
     *  MyBean entityAndSecurityContext(MyBean bean, @Context SecurityContext sc);
     * </pre>
     */
    @Test
    public void testEntityAndSecurityContext() {
        final MyBean bean = new MyBean();
        bean.name = NAME;
        final int entityIndex = 0;
        MyResourceIfc resource = WebResourceFactory.newResource(
                MyResourceIfc.class, target(), false, headers, cookies, new Form(), entityIndex);
        assertEquals(NAME, resource.entityAndSecurityContext(bean, null).name);

        resource = WebResourceFactory.newResource(MyResourceIfc.class, target(), entityIndex);
        assertEquals(NAME, resource.entityAndSecurityContext(bean, null).name);
    }

    /**
     * <pre>
     *  MyBean securityContextAndEntity(@Context SecurityContext sc, MyBean bean);
     * </pre>
     */
    @Test
    public void testSecurityContextAndEntity() {
        final MyBean bean = new MyBean();
        bean.name = NAME;
        final int entityIndex = 1;
        MyResourceIfc resource = WebResourceFactory.newResource(
                MyResourceIfc.class, target(), false, headers, cookies, new Form(), entityIndex);
        assertEquals(NAME, resource.securityContextAndEntity(null, bean).name);

        resource = WebResourceFactory.newResource(MyResourceIfc.class, target(), entityIndex);
        assertEquals(NAME, resource.securityContextAndEntity(null, bean).name);
    }

    /**
     * <pre>
     *  MyBean headerAndEntityAndSecurityContext(@HeaderParam("Content-Type") String header,
     *                                           MyBean bean,
     *                                           @Context SecurityContext sc);
     * </pre>
     */
    @Test
    public void testHeaderAndEntityAndSecurityContext() {
        final MyBean bean = new MyBean();
        bean.name = NAME;
        final int entityIndex = 1;
        MyResourceIfc resource = WebResourceFactory.newResource(
                MyResourceIfc.class, target(), false, headers, cookies, new Form(), entityIndex);
        assertEquals(NAME, resource.headerAndEntityAndSecurityContext(null, bean, null).name);

        resource = WebResourceFactory.newResource(MyResourceIfc.class, target(), entityIndex);
        assertEquals(NAME, resource.headerAndEntityAndSecurityContext(null, bean, null).name);
    }
}
