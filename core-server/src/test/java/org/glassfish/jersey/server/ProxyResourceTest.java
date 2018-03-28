/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.server;

import org.glassfish.jersey.server.model.Resource;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for creating an application with interface resource and proxy instance
 * via {@link Resource}'s programmatic API.
 *
 * @author DangCat (fan.shutian@zte.com.cn)
 */
public class ProxyResourceTest {
    @Path("/srv1")
    public interface ServiceOne {
        @GET
        String getName();

        @PUT
        @Path("{name}")
        void setName(@PathParam("name") String name);
    }

    public static class ServiceOneImpl implements ServiceOne {
        private String name = null;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }
    }

    private ApplicationHandler createApplication() {
        final ResourceConfig resourceConfig = new ResourceConfig();
        final ServiceOne serviceOne = new ServiceOneImpl();
        Object proxyService = Proxy.newProxyInstance(ServiceOneImpl.class.getClassLoader(),
                new Class[]{ServiceOne.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return method.invoke(serviceOne, args);
                    }
                });
        resourceConfig.registerResources(Resource.from(ServiceOne.class, proxyService, false));
        return new ApplicationHandler(resourceConfig);
    }

    @Test
    public void testProxyResource() throws InterruptedException, ExecutionException {
        ApplicationHandler application = createApplication();
        application.apply(RequestContextBuilder.from("/srv1/" + ServiceOne.class.getName(),
                "PUT")
                .build());
        Object result = application.apply(RequestContextBuilder.from("/srv1",
                "GET")
                .build())
                .get().getEntity();
        assertEquals(ServiceOne.class.getName(), result);
    }
}
