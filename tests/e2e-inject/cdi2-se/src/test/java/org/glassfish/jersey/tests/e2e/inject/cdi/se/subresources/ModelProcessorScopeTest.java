/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.inject.cdi.se.subresources;

import java.util.concurrent.ExecutionException;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.tests.e2e.inject.cdi.se.RequestContextBuilder;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test scope of resources enhanced by model processors.
 *
 * @author Miroslav Fuksa
 *
 */
public class ModelProcessorScopeTest {

    private void _testCounter(ApplicationHandler applicationHandler, String requestUri, final String prefix,
                              final String expectedSecondHit) throws
            InterruptedException, ExecutionException {
        ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from(requestUri,
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(prefix + ":0", response.getEntity());
        response = applicationHandler.apply(RequestContextBuilder.from(requestUri,
                "GET").build()).get();
        assertEquals(prefix + ":" + expectedSecondHit, response.getEntity());
    }

    @Test
    public void testSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ModelProcessorFeature
                .SingletonResource.class));
        final String requestUri = "/singleton";
        _testCounter(applicationHandler, requestUri, "SingletonResource", "1");
    }

    @Test
    public void testSingletonInModelProcessor() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/singleton";
        _testCounter(applicationHandler, requestUri, "SingletonResource", "1");
    }

    @Test
    public void testSubResourceSingletonInOriginalModel() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        final String requestUri = "/root/sub-resource-singleton";
        _testCounter(applicationHandler, requestUri, "SubResourceSingleton", "1");
    }

    @Test
    public void testSubResourceEnhancedSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        final String requestUri = "/root/sub-resource-singleton/enhanced-singleton";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResourceSingleton", "1");
    }

    @Test
    public void testSubResourceInstanceEnhancedSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        final String requestUri = "/root/sub-resource-instance/enhanced-singleton";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResourceSingleton", "1");
    }

    @Test
    public void testSubResourceInstanceEnhancedSubResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        final String requestUri = "/root/sub-resource-instance/enhanced";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResource", "0");
    }

    @Test
    public void testSubResourceEnhancedSubResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class));
        final String requestUri = "/root/sub-resource-singleton/enhanced";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResource", "0");
    }

    @Test
    public void testInstanceInModelProcessor() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/instance";
        _testCounter(applicationHandler, requestUri, "Inflector", "1");
    }

    @Test
    public void testRootSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                RootSingletonResource.class));
        final String requestUri = "/root-singleton";
        _testCounter(applicationHandler, requestUri, "RootSingletonResource", "1");
    }

    @Test
    public void testRequestScopeResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                RootSingletonResource.class, ModelProcessorFeature.class));
        final String requestUri = "/request-scope";
        _testCounter(applicationHandler, requestUri, "RequestScopeResource", "0");
    }
}
