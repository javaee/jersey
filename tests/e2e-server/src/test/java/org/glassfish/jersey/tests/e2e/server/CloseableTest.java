/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import javax.inject.Singleton;

import org.glassfish.jersey.server.CloseableService;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Marc Hadley
 */
public class CloseableTest extends JerseyTest {
    private static CountDownLatch perRequestCdl = new CountDownLatch(1);
    private static CountDownLatch singletonCdl = new CountDownLatch(1);

    @Path("per-request")
    public static class PerRequestResource implements Closeable {
        static boolean isClosed;

        @Context
        CloseableService cs;

        @GET
        public String doGet() {
            isClosed = false;
            cs.add(this);
            return "ok";
        }

        public void close() throws IOException {
            isClosed = true;
            perRequestCdl.countDown();
        }
    }

    @Path("singleton")
    @Singleton
    public static class SingletonResource extends PerRequestResource {
        @Override
        public void close() {
            isClosed = true;
            singletonCdl.countDown();
        }
    }

    @Override
    public ResourceConfig configure() {
        return new ResourceConfig(PerRequestResource.class, SingletonResource.class);
    }

    @Test
    public void testPerRequest() throws InterruptedException {
        target().path("per-request").request().get(String.class);
        perRequestCdl.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(PerRequestResource.isClosed);
    }

    @Test
    public void testSingleton() throws InterruptedException {
        target().path("singleton").request().get(String.class);
        perRequestCdl.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(SingletonResource.isClosed);
    }

}
