/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spring.test;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.SpringLifecycleListener;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * Base class for JAX-RS resource tests.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public abstract class AccountResourceTestBase extends JerseyTest {
    private static final String TEST_WEBAPP_CONTEXT_PATH = "jersey.spring.test.contextPath";
    private static final String TEST_CONTAINER_FACTORY_EXTERNAL = "org.glassfish.jersey.test.external.ExternalTestContainerFactory";

    @Override
    protected Application configure() {
        ResourceConfig rc = new ResourceConfig()
                .register(SpringLifecycleListener.class)
                .register(RequestContextFilter.class)
                ;
        TestUtil.registerHK2Services(rc);
        rc.property("contextConfigLocation", "classpath:applicationContext.xml");
        return configure(rc);
    }

    protected abstract ResourceConfig configure(ResourceConfig rc);

    protected abstract String getResourcePath();

    protected String getResourceFullPath() {
        String containerFactory = System.getProperty(TestProperties.CONTAINER_FACTORY);
        if(TEST_CONTAINER_FACTORY_EXTERNAL.equals(containerFactory)) {
            return System.getProperty(TEST_WEBAPP_CONTEXT_PATH)+getResourcePath();
        }
        return getResourcePath();
    }

    // test singleton scoped Spring bean injection using @Inject + @Autowired
    @Test
    public void testSingletonScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target(getResourceFullPath());

        t.path("/singleton/xyz123").request().put(Entity.entity(newBalance.toString(), MediaType.TEXT_PLAIN_TYPE));
        BigDecimal balance = t.path("/singleton/autowired/xyz123").request().get(BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testRequestScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target(getResourceFullPath());
        BigDecimal balance = t.path("request/abc456").request().put(Entity.text(newBalance), BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testPrototypeScopedSpringService() {
        BigDecimal newBalance = new BigDecimal(Math.random());
        WebTarget t = target(getResourceFullPath());
        BigDecimal balance = t.path("prototype/abc456").request().put(Entity.text(newBalance), BigDecimal.class);
        assertEquals(new BigDecimal("987.65"), balance);
    }

}
