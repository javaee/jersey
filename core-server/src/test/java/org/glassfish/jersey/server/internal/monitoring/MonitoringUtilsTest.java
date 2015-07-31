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

package org.glassfish.jersey.server.internal.monitoring;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Miroslav Fuksa
 */
public class MonitoringUtilsTest {

    @Path("resource")
    public static class MyResource {

        @GET
        public String get() {
            return "get";
        }

        @GET
        @Produces(MediaType.TEXT_HTML)
        @Path("sub")
        public String subGet() {
            return "sub";
        }

        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_XML)
        @POST
        public String post() {
            return "xml";
        }
    }

    @Test
    public void testGetMethodUniqueId() {
        final Resource resource = Resource.builder(MyResource.class).build();
        Assert.assertEquals("[]|[]|GET|resource|get",
                MonitoringUtils.getMethodUniqueId(getMethod(resource, "get")));
        Assert.assertEquals("[text/html]|[]|GET|resource.sub|subGet",
                MonitoringUtils.getMethodUniqueId(getMethod(resource, "subGet")));
        Assert.assertEquals("[text/html]|[]|GET|resource.sub|subGet",
                MonitoringUtils.getMethodUniqueId(getMethod(resource, "subGet")));
        Assert.assertEquals("[text/xml]|[text/plain]|POST|resource|post",
                MonitoringUtils.getMethodUniqueId(getMethod(resource, "post")));

    }

    private ResourceMethod getMethod(Resource resource, String javaName) {

        for (ResourceMethod method : resource.getResourceMethods()) {
            if (method.getInvocable().getHandlingMethod().getName().equals(javaName)) {
                return method;
            }
        }
        for (Resource child : resource.getChildResources()) {
            final ResourceMethod childMethod = getMethod(child, javaName);
            if (childMethod != null) {
                return childMethod;
            }
        }
        return null;
    }
}
