/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.monitoring;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class ReloadApplicationEventTest extends JerseyTest {

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class, AppEventListener.class);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        return resourceConfig;
    }

    @Path("resource")
    @Singleton
    public static class TestResource implements ContainerLifecycleListener {
        public static boolean reloaded;
        private volatile Container container;

        @GET
        public String get() {
            container.reload();
            return "get";
        }

        @Override
        public void onStartup(Container container) {
            this.container = container;
        }

        @Override
        public void onReload(Container container) {
            reloaded = true;
        }

        @Override
        public void onShutdown(Container container) {
        }
    }

    public static class AppEventListener implements ApplicationEventListener {
        public static boolean reloadEventCalled;
        public static boolean initFinishedCalled;

        @Override
        public void onEvent(ApplicationEvent event) {
            switch (event.getType()) {
                case INITIALIZATION_FINISHED:
                    initFinishedCalled = true;
                    break;
                case RELOAD_FINISHED:
                    reloadEventCalled = true;
                    break;
            }
        }

        @Override
        public RequestEventListener onRequest(RequestEvent requestEvent) {
            return null;
        }
    }

    @Test
    public void testApplicationEvents() {
        final Response response = target().path("resource").request().get();
        assertEquals(200, response.getStatus());
        assertTrue(TestResource.reloaded);
        assertTrue(AppEventListener.initFinishedCalled);
        assertTrue(AppEventListener.reloadEventCalled);
    }
}