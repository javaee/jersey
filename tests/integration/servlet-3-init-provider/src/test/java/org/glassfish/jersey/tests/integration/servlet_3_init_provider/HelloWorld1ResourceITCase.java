/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.servlet_3_init_provider;

import javax.ws.rs.client.WebTarget;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public class HelloWorld1ResourceITCase extends AbstractHelloWorldResourceTest {

    protected Class<?> getResourceClass() {
        return HelloWorld1Resource.class;
    }

    protected int getIndex() {
        return 1;
    }

    @Test
    public void testRegisteredServletNames() throws Exception {
        WebTarget target = target("application" + getIndex()).path("helloworld" + getIndex()).path("servlets");
        Assert.assertEquals(AbstractHelloWorldResource.NUMBER_OF_APPLICATIONS, (int) target.request().get(Integer.TYPE));

        target = target.path("{name}");
        testRegisteredServletNames(target, "org.glassfish.jersey.tests.integration.servlet_3_init_provider.Application1");
        testRegisteredServletNames(target, "application2");
        testRegisteredServletNames(target, "application3");
        testRegisteredServletNames(target, "org.glassfish.jersey.tests.integration.servlet_3_init_provider.Application4");
        testRegisteredServletNames(target, "javax.ws.rs.core.Application");
    }

    private void testRegisteredServletNames(WebTarget target, String servletName) throws Exception {
        Assert.assertTrue(target.resolveTemplate("name", servletName).request().get(Boolean.TYPE));
    }

    @Test
    public void testImmutableServletNames() {
        WebTarget target = target("application" + getIndex()).path("helloworld" + getIndex()).path("immutableServletNames");
        Assert.assertTrue(target.request().get(Boolean.TYPE));
    }

}
