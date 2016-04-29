/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.common;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.uri.UriComponent;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Libor Kamolis (libor.kramolis at oracle.com)
 */
public class UriComponentTest extends JerseyTest {

    private static final String VALUE_PREFIX = "-<#[";
    private static final String VALUE_SUFFIX = "]#>-";

    @Path("/test")
    public static class MyResource {
        @GET
        @Path("text")
        public String getTextValue(@QueryParam("text") String text) {
            return VALUE_PREFIX + text + VALUE_SUFFIX;
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(MyResource.class);
        resourceConfig.register(LoggingFeature.class);
        return resourceConfig;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(LoggingFeature.class);
        super.configureClient(config);
    }

    /**
     * Reproducer for JERSEY-2260
     */
    @Test
    public void testText() {
        final String QUERY_RESERVED_CHARS = ";/?:@&=+,$";
        final String OTHER_SPECIAL_CHARS = "\"\t- \n'";

        testTextImpl("query reserved characters", QUERY_RESERVED_CHARS);
        testTextImpl("other special characters", OTHER_SPECIAL_CHARS);

        testTextImpl("query reserved characters between template brackets", "{abc" + QUERY_RESERVED_CHARS + "XYZ}");
        testTextImpl("other special characters between template brackets", "{abc" + OTHER_SPECIAL_CHARS + "XYZ}");

        testTextImpl("json - double quote", "{ \"jmeno\" : \"hodnota\" }");
        testTextImpl("json - single quote", "{ 'jmeno' : 'hodnota' }");
    }

    private void testTextImpl(String message, final String text) {
        String encoded = UriComponent.encode(text, UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);
        WebTarget target = target("test/text");
        Response response = target.queryParam("text", encoded).request().get();
        Assert.assertEquals(200, response.getStatus());
        String actual = response.readEntity(String.class);
        Assert.assertEquals(message, VALUE_PREFIX + text + VALUE_SUFFIX, actual);
    }

}
