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
package org.glassfish.jersey.server.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import javax.inject.Inject;

import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.BackgroundScheduler;
import org.glassfish.jersey.spi.ScheduledThreadPoolExecutorProvider;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Test basic application behavior.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos
 */
public class BackgroundSchedulerProviderTest {

    private ApplicationHandler createApplication(Class<?>... classes) {
        final ResourceConfig resourceConfig = new ResourceConfig(classes);

        return new ApplicationHandler(resourceConfig);
    }

    public static final class CustomThread extends Thread {
        public CustomThread(Runnable target) {
            super(target);
        }
    }

    @BackgroundScheduler
    public static final class CustomThreadProvider extends ScheduledThreadPoolExecutorProvider {

        public CustomThreadProvider() {
            super("custom-scheduler");
        }

        @Override
        public ThreadFactory getBackingThreadFactory() {
            return new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new CustomThread(r);
                }
            };
        }
    }

    @Path("executors-test")
    public static final class TestResource {
        @Inject
        @BackgroundScheduler
        private ScheduledExecutorService bs;

        @GET
        public int getTestResult() throws ExecutionException, InterruptedException {
            int result = 1; // method invoked

            final Future<Integer> future = bs.submit(new Producer<Integer>() {
                @Override
                public Integer call() {
                    final Thread thread = Thread.currentThread();
                    if (thread instanceof CustomThread) {
                        return 10; // CustomThreadProvider used to provide BackgroundScheduler executor service
                    }

                    return 0;
                }
            });

            result += future.get();

            return result;
        }
    }

    @Test
    public void testCustomRuntimeThreadProviderSupport() throws ExecutionException, InterruptedException {
        ApplicationHandler ah = createApplication(CustomThreadProvider.class, TestResource.class);

        final ContainerResponse response = ah.apply(RequestContextBuilder.from("/executors-test", "GET").build()).get();

        assertEquals(200, response.getStatus());
        assertEquals("Some executor test assertions failed.", 11, response.getEntity());
    }
}
