/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.container;

import java.net.URI;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Ignore;
import org.junit.Test;
import static junit.framework.TestCase.assertEquals;

/**
 * Test Jersey container implementation of URL resolving.
 * JDK Server does not support multiple slashes before the context-path,
 * in case of multiple slashes server does not match the processing handler and
 * therefore Jersey Request cannot be created and processed.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class JdkSlashesWithContextPathTest extends AbstractSlashesWithContextPathTest {

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new JdkHttpServerTestContainerFactory();
    }

    @Test
    public void testSimpleSlashes() {
        Response result = call(CONTEXT_PATH + "/simple");
        assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));

        result = call("/" + CONTEXT_PATH + "/simple");
        assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithPathParam() {
        Response result = call(CONTEXT_PATH + "/pathparam/Container/Response/test");
        assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithEmptyPathParam() {
        Response result = call(CONTEXT_PATH + "/pathparam///test");
        assertEquals("-", result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithBeginningEmptyPathParam() {
        Response result = call(CONTEXT_PATH + "///test");
        assertEquals("-", result.readEntity(String.class));
    }

    @Test
    public void testSlashesWithBeginningEmptyPathParamWithQueryParams() {
        URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
        WebTarget target = client().target(hostPort).path(CONTEXT_PATH + "///testParams")
                .queryParam("bar", "Container")
                .queryParam("baz", "Response");

        Response result = target.request().get();
        assertEquals("PATH PARAM: -, QUERY PARAM Container-Response", result.readEntity(String.class));
    }

    @Test
    @Ignore("failing with JDK container.")
    public void testEncodedQueryParams() {
        URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
        WebTarget target = client().target(hostPort).path("//" + CONTEXT_PATH + "///encoded")
                .queryParam("query", "%dummy23+a");

        Response response = target.request().get();
        assertEquals(200, response.getStatus());
        assertEquals("true:%25dummy23%2Ba", response.readEntity(String.class));
    }
}
