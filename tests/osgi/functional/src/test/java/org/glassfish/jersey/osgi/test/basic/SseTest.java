/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.osgi.test.basic;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;

import javax.inject.Inject;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Basic test for SSE module OSGification.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(PaxExam.class)
public class SseTest {

    private static final String CONTEXT = "/jersey";

    private static final URI baseUri = UriBuilder
            .fromUri("http://localhost")
            .port(Helper.getPort())
            .path(CONTEXT).build();

    @Inject
    protected BundleContext bundleContext;

    @Configuration
    public static Option[] configuration() {
        List<Option> options = Helper.getCommonOsgiOptions();

        options.addAll(Helper.expandedList(
                // Jersey SSE dependencies
                mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-sse").versionAsInProject()));

        options = Helper.addPaxExamMavenLocalRepositoryProperty(options);
        return Helper.asArray(options);
    }

    @Path("/sse")
    public static class SseResource {

        @GET
        @Produces(SseFeature.SERVER_SENT_EVENTS)
        public EventOutput getIt() throws IOException {
            final EventOutput result = new EventOutput();
            result.write(new OutboundEvent.Builder().name("event1").data(String.class, "ping").build());
            result.write(new OutboundEvent.Builder().name("event2").data(String.class, "pong").build());
            result.close();
            return result;
        }
    }

    @Test
    public void testSse() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(SseResource.class, SseFeature.class);
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig);

        Client c = ClientBuilder.newClient();
        c.register(SseFeature.class);

        final List<String> data = new LinkedList<String>();
        final CountDownLatch latch = new CountDownLatch(2);

        final EventSource eventSource = new EventSource(c.target(baseUri).path("/sse")) {

            @Override
            public void onEvent(InboundEvent event) {
                try {
                    data.add(event.readData());
                    latch.countDown();
                } catch (ProcessingException e) {
                    // ignore
                }
            }
        };

        assertTrue(latch.await(2, TimeUnit.SECONDS));

        eventSource.close();
        assertEquals(2, data.size());

        server.shutdownNow();
    }
}
