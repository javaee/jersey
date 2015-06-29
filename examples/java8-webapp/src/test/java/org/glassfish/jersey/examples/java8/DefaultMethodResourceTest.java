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

package org.glassfish.jersey.examples.java8;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test usage of Java8's interface default methods as resource methods.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class DefaultMethodResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new Java8Application();
    }

    /**
     * Test that JDK8 default methods do work as common JAX-RS resource methods.
     */
    @Test
    public void testDefaultMethods() {
        final WebTarget defaultMethodTarget = target("default-method");

        // test default method with no @Path annotation
        String response = defaultMethodTarget.request().get(String.class);
        assertEquals("interface-root", response);

        // test default method with with @Path annotation
        response = defaultMethodTarget.path("path").request().get(String.class);
        assertEquals("interface-path", response);
    }

    /**
     * Test, that resource methods defined in the class implementing the interface with default method do work normally.
     */
    @Test
    public void testImplementingClass() throws Exception {
        final String response = target("default-method").path("class").request().get(String.class);
        assertEquals("class", response);
    }
}
