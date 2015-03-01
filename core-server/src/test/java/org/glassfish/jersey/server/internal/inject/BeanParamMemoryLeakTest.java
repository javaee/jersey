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

package org.glassfish.jersey.server.internal.inject;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Reproducer for JERSEY-2800. We need to make sure
 * number of descriptors in HK2 for {@link BeanParam} injected
 * parameter does not grow up in time.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class BeanParamMemoryLeakTest extends AbstractTest {

    public static class ParameterBean {

        @Context
        Request request;
        @QueryParam("q")
        String q;
    }

    @Path("/")
    public static class BeanParamInjectionResource {

        @BeanParam
        ParameterBean bean;

        @GET
        @Path("jaxrs")
        public String getMilkyWay() {
            assertEquals("GET", bean.request.getMethod());
            return bean.q;
        }
    }

    @Test
    public void testBeanParam() throws Exception {

        initiateWebApplication(BeanParamInjectionResource.class);
        final ServiceLocator locator = app().getServiceLocator();

        // we do not expect any descriptor registered yet
        assertEquals(0, locator.getDescriptors(new ParameterBeanFilter()).size());

        // now make one registered via this call
        assertEquals("one", resource("/jaxrs?q=one").getEntity());

        // make sure it got registered
        assertEquals(1, locator.getDescriptors(new ParameterBeanFilter()).size());

        // make another call
        assertEquals("two", resource("/jaxrs?q=two").getEntity());
        assertEquals(1, locator.getDescriptors(new ParameterBeanFilter()).size());

        // and some more
        for (int i = 0; i < 20; i++) {
            assertEquals(Integer.toString(i), resource("/jaxrs?q=" + i).getEntity());
            assertEquals(1, locator.getDescriptors(new ParameterBeanFilter()).size());
        }
    }

    private ContainerResponse resource(String uri) throws Exception {
        return apply(RequestContextBuilder.from(uri, "GET").build());
    }

    private static class ParameterBeanFilter implements Filter {

        public ParameterBeanFilter() {
        }

        @Override
        public boolean matches(Descriptor d) {
            return ParameterBean.class.getName().equals(d.getImplementation());
        }
    }
}
