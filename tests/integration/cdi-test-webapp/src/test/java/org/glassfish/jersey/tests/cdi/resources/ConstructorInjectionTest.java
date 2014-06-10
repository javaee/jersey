/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.resources;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Part of JERSEY-2526 reproducer. Without the fix, the application would
 * not deploy at all. This is just to make sure the JAX-RS parameter producer
 * keeps working as expected without regressions.
 *
 * @author Jakub Podlesak (jakub.podlesak at oralc.com)
 */
public class ConstructorInjectionTest extends CdiTest {

    @Test
    public void testConstructorInjectedResource() {

        final WebTarget target = target().path("ctor-injected");

        final Response pathParamResponse = target.path("pathParam").request().get();
        assertThat(pathParamResponse.getStatus(), is(200));
        assertThat(pathParamResponse.readEntity(String.class), is("pathParam"));

        final Response queryParamResponse = target.path("queryParam").queryParam("q", "123").request().get();
        assertThat(queryParamResponse.getStatus(), is(200));
        assertThat(queryParamResponse.readEntity(String.class), is("123"));

        final Response matrixParamResponse = target.path("matrixParam").matrixParam("m", "456").request().get();
        assertThat(matrixParamResponse.getStatus(), is(200));
        assertThat(matrixParamResponse.readEntity(String.class), is("456"));

        final Response headerParamResponse = target.path("headerParam").request().header("Custom-Header", "789").get();
        assertThat(headerParamResponse.getStatus(), is(200));
        assertThat(headerParamResponse.readEntity(String.class), is("789"));

        final Response cdiParamResponse = target.path("cdiParam").request().get();
        assertThat(cdiParamResponse.getStatus(), is(200));
        assertThat(cdiParamResponse.readEntity(String.class), is("cdi-produced"));
    }
}
