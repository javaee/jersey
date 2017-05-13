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
package org.glassfish.jersey.server.spring.test;

import java.math.BigDecimal;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Base class for JAX-RS resource tests.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public abstract class AccountResourceTestBase extends ResourceTestBase {

    // test singleton scoped Spring bean injection using @Inject + @Autowired
    @Test
    public void testSingletonScopedSpringService() {
        final BigDecimal newBalance = new BigDecimal(Math.random());
        final WebTarget t = target(getResourceFullPath());

        t.path("/singleton/xyz123").request().put(Entity.entity(newBalance.toString(), MediaType.TEXT_PLAIN_TYPE));
        final BigDecimal balance = t.path("/singleton/autowired/xyz123").request().get(BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testRequestScopedSpringService() {
        final BigDecimal newBalance = new BigDecimal(Math.random());
        final WebTarget t = target(getResourceFullPath());
        final BigDecimal balance = t.path("request/abc456").request().put(Entity.text(newBalance), BigDecimal.class);
        assertEquals(newBalance, balance);
    }

    @Test
    public void testPrototypeScopedSpringService() {
        final BigDecimal newBalance = new BigDecimal(Math.random());
        final WebTarget t = target(getResourceFullPath());
        final BigDecimal balance = t.path("prototype/abc456").request().put(Entity.text(newBalance), BigDecimal.class);
        assertEquals(new BigDecimal("987.65"), balance);
    }

    @Test
    public void testServletInjection() {
        final WebTarget t = target(getResourceFullPath());

        String server = t.path("server").request().get(String.class);
        assertThat(server, startsWith("PASSED: "));

        server = t.path("singleton/server").request().get(String.class);
        assertThat(server, startsWith("PASSED: "));

        server = t.path("singleton/autowired/server").request().get(String.class);
        assertThat(server, startsWith("PASSED: "));

        server = t.path("request/server").request().get(String.class);
        assertThat(server, startsWith("PASSED: "));

        server = t.path("prototype/server").request().get(String.class);
        assertThat(server, startsWith("PASSED: "));
    }
}
