/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
public class EscapedUriTest extends JerseyContainerTest {

    private static final String RESPONSE = "CONTENT";

    @Path("x%20y")
    public static class EscapedUriResource {

        private final String context;

        @SuppressWarnings("UnusedDeclaration")
        public EscapedUriResource() {
            this("");
        }

        public EscapedUriResource(final String context) {
            this.context = context;
        }

        @GET
        public String get(@Context final UriInfo info) {
            assertEquals(context + "/x%20y", info.getAbsolutePath().getRawPath());
            assertEquals("/", info.getBaseUri().getRawPath());
            assertEquals(context + "/x y", "/" + info.getPath());
            assertEquals(context + "/x%20y", "/" + info.getPath(false));

            return RESPONSE;
        }
    }

    @Path("non/x y")
    public static class NonEscapedUriResource extends EscapedUriResource {

        public NonEscapedUriResource() {
            super("/non");
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EscapedUriResource.class, NonEscapedUriResource.class);
    }

    @Test
    public void testEscaped() {
        assertThat(target("x%20y").request().get(String.class), is(RESPONSE));
    }

    @Test
    public void testNonEscaped() {
        assertThat(target("non/x y").request().get(String.class), is(RESPONSE));
    }
}
