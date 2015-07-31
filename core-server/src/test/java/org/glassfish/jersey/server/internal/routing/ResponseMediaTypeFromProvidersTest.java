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

package org.glassfish.jersey.server.internal.routing;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
public class ResponseMediaTypeFromProvidersTest {

    @Path("resource/subresource/sub")
    public static class AnotherSubResource {

        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        public String sub() {
            return getClass().getSimpleName();
        }

        @POST
        public String subsub() {
            return sub() + sub();
        }

        @GET
        public String get() {
            return sub();
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getget() {
            return subsub();
        }

        @GET
        @Produces("text/*")
        public String getTextStar() {
            return "text/*";
        }

        @POST
        @Consumes("text/*")
        public String postTextStar() {
            return "text/*";
        }
    }

    @Test
    public void testSubResource() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(AnotherSubResource.class);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        final ContainerResponse response = applicationHandler.apply(
                RequestContextBuilder.from("/resource/subresource/sub", "POST").header("Accept", "text/plain").build()).get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getHeaderString("Content-Type"), equalTo("text/plain"));
        assertThat((String) response.getEntity(), equalTo("AnotherSubResource"));
    }
}
