/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ServerLocatorFactory;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModelComponent;

import org.glassfish.hk2.api.ServiceLocator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/**
 * @author Jakub Podlesak
 */
public class ResourceMethodDispatcherFactoryTest {

    private ResourceMethodDispatcherFactory rmdf;
    private ResourceMethodInvocationHandlerFactory rmihf;

    @Before
    public void setupApplication() {
        ServiceLocator locator = ServerLocatorFactory.createLocator();

        rmdf = locator.getService(ResourceMethodDispatcherFactory.class);
        rmihf = locator.getService(ResourceMethodInvocationHandlerFactory.class);
    }

    @Test
    public void testBasicDispatchers() throws InterruptedException, ExecutionException {
        final Resource.Builder rb = Resource.builder();

        final Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPrivate(method.getModifiers())) {
                // class-based
                rb.addMethod("GET").handledBy(this.getClass(), method);
                // instance-based
                rb.addMethod("GET").handledBy(this, method);
            }
        }

        for (ResourceModelComponent component : rb.build().getComponents()) {
            if (component instanceof ResourceMethod) {
                Invocable invocable = ((ResourceMethod) component).getInvocable();
                assertNotNull("No dispatcher found for invocable " + invocable.toString(),
                        rmdf.create(invocable, rmihf.create(invocable), null));
            }
        }

    }

    private void voidVoid() {
        // do nothing
    }

    private String voidString() {
        // do nothing
        return null;
    }

    private Response voidResponse() {
        // do nothing
        return null;
    }

    private void stringVoid(String s) {
        // do nothing
    }

    private String stringString(String s) {
        // do nothing
        return null;
    }

    private void requestVoid(Request s) {
        // do nothing
    }

    private String requestString(Request s) {
        // do nothing
        return null;
    }

    private Response requestResponse(Request s) {
        // do nothing
        return null;
    }
}
