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

package org.glassfish.jersey.simple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * Reload and ContainerLifecycleListener support test.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class LifecycleListenerTest extends AbstractSimpleServerTester {

    @Path("/one")
    public static class One {
        @GET
        public String get() {
            return "one";
        }
    }

    @Path("/two")
    public static class Two {
        @GET
        public String get() {
            return "two";
        }
    }

    public static class Reloader extends AbstractContainerLifecycleListener {
        Container container;

        public void reload(ResourceConfig newConfig) {
            container.reload(newConfig);
        }

        public void reload() {
            container.reload();
        }

        @Override
        public void onStartup(Container container) {
            this.container = container;
        }

    }

    @Test
    public void testReload() {
        final ResourceConfig rc = new ResourceConfig(One.class);

        Reloader reloader = new Reloader();
        rc.registerInstances(reloader);

        startServer(rc);

        WebTarget r = ClientBuilder.newClient().target(getUri().path("/").build());

        assertEquals("one", r.path("one").request().get(String.class));
        assertEquals(404, r.path("two").request().get(Response.class).getStatus());

        // add Two resource
        reloader.reload(new ResourceConfig(One.class, Two.class));

        assertEquals("one", r.path("one").request().get(String.class));
        assertEquals("two", r.path("two").request().get(String.class));
    }

    static class StartStopListener extends AbstractContainerLifecycleListener {
        volatile boolean started;
        volatile boolean stopped;

        @Override
        public void onStartup(Container container) {
            started = true;
        }

        @Override
        public void onShutdown(Container container) {
            stopped = true;
        }
    }

    @Test
    public void testStartupShutdownHooks() {
        final StartStopListener listener = new StartStopListener();

        startServer(new ResourceConfig(One.class).register(listener));

        WebTarget r = ClientBuilder.newClient().target(getUri().path("/").build());

        assertThat(r.path("one").request().get(String.class), equalTo("one"));
        assertThat(r.path("two").request().get(Response.class).getStatus(), equalTo(404));

        stopServer();

        assertTrue("ContainerLifecycleListener.onStartup has not been called.", listener.started);
        assertTrue("ContainerLifecycleListener.onShutdown has not been called.", listener.stopped);
    }
}
