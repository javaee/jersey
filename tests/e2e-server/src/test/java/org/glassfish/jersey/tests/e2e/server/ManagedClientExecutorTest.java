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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import javax.inject.Inject;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.server.ClientBinding;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.Uri;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test, that managed client uses the custom executor service.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class ManagedClientExecutorTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(TestResource.class);
    }

    /**
     * Test JAX-RS resource.
     */
    @Path(value = "/test")
    public static class TestResource {
        /**
         * Invoke an asynchronous request using the injected managed client.
         * <p>
         * The client is configured to use custom message body reader, that checks its worker thread and replaces the original
         * response with the transfer object containing the thread name. The name is than used to identify if the correct
         * (custom) executor service is used.
         *
         * @return name of the thread, that executed the asynchronous request
         */
        @GET
        @Path("executor")
        public String managedClientWithExecutor(@ClientA @Uri("http://localhost:9998/") WebTarget target)
                throws ExecutionException, InterruptedException {
            Future<ThreadName> nameFuture = target.path("test/dummy").request().async().get(ThreadName.class);
            return nameFuture.get().getThreadName();
        }

        /**
         * Invoke an asynchronous request using the injected managed client.
         * <p>
         * The client is configured to use custom message body reader, that uses injected scheduled executor service and
         * schedules a task, that checks its worker thread name, then it does the same with the injected scheduled executor
         * service provider (to check that both injection approaches produce same result) and returns both thread names in a
         * transfer objects. The names are than used to identify if the correct (custom) scheduled executor service(s) is (are)
         * used.
         *
         * @return names of the threads, that executed the scheduled tasks
         */
        @GET
        @Path("scheduledExecutor")
        public String managedClientWithScheduledExecutor(@ClientA @Uri("http://localhost:9998/") WebTarget target)
                throws ExecutionException, InterruptedException {
            Future<SchedulerThreadName> namesFuture = target.path("test/dummy").request().async().get(SchedulerThreadName.class);
            return namesFuture.get().getThreadNameFromService() + " " + namesFuture.get().getThreadNameFromProvider();
        }

        /**
         * Dummy method, that is invoked by the managed client.
         *
         * @return dummy response, does not really matter
         */
        @GET
        @Path("dummy")
        public String dummyMethod() {
            return "nothing";
        }
    }

    /**
     * Managed client binding.
     */
    @ClientBinding(configClass = MyClientAConfig.class)
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    public @interface ClientA {
    }

    /**
     * Managed client config.
     */
    public static class MyClientAConfig extends ClientConfig {

        public MyClientAConfig() {
            final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                    .setNameFormat("foo-executor-service-%d")
                    .build());

            final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("bar-executor-service")
                    .build());

            this.register(new ThreadNameReader())
                    .register(SchedulerThreadNameReader.class)
                    .executorService(executor)
                    .scheduledExecutorService(scheduler);
        }
    }

    /**
     * Transfer object for executor service name.
     */
    static class ThreadName {
        private String threadName;

        ThreadName(String threadName) {
            this.threadName = threadName;
        }

        String getThreadName() {
            return threadName;
        }
    }

    /**
     * Transfer object for scheduled executor service(s) name(s), both resolved from directly injected service and via provider.
     */
    static class SchedulerThreadName {
        private String threadNameFromService;
        private String threadNameFromProvider;

        SchedulerThreadName(String threadNameFromService, String threadNameFromProvider) {
            this.threadNameFromService = threadNameFromService;
            this.threadNameFromProvider = threadNameFromProvider;
        }

        String getThreadNameFromService() {
            return threadNameFromService;
        }

        String getThreadNameFromProvider() {
            return threadNameFromProvider;
        }
    }

    /**
     * Custom reader, that checks its own thread's name and returns it as an entity.
     */
    public static class ThreadNameReader implements MessageBodyReader<ThreadName> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public ThreadName readFrom(Class<ThreadName> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                                   MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            return new ThreadName(Thread.currentThread().getName());
        }
    }

    /**
     * Custom reader, that schedules one task on each injected scheduler (directly and via provider) and returns the names of
     * the threads that executed those tasks as an entity.
     */
    public static class SchedulerThreadNameReader implements MessageBodyReader<SchedulerThreadName> {

        @Inject
        ScheduledExecutorServiceProvider injectedProvider;

        @Inject
        ScheduledExecutorService injectedService;

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public SchedulerThreadName readFrom(Class<SchedulerThreadName> type, Type genericType, Annotation[] annotations,
                                            MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                            InputStream entityStream) throws IOException, WebApplicationException {

            AtomicReference<String> nameFromService = new AtomicReference<>();
            AtomicReference<String> nameFromProvider = new AtomicReference<>();
            CountDownLatch cdl = new CountDownLatch(2);

            injectedService.schedule(() -> {
                nameFromService.set(Thread.currentThread().getName());
                cdl.countDown();
            }, 1, TimeUnit.MILLISECONDS);

            injectedProvider.getExecutorService().schedule(() -> {
                nameFromProvider.set(Thread.currentThread().getName());
                cdl.countDown();
            }, 1, TimeUnit.MILLISECONDS);

            try {
                cdl.await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                nameFromService.set("failed");
                nameFromProvider.set("failed");
            }

            return new SchedulerThreadName(nameFromService.get(), nameFromProvider.get());
        }
    }

    @Test
    public void testManagedClientExecutor() {
        final String response = target().path("test/executor").request().get(String.class);
        Assert.assertEquals("foo-executor-service-0", response);
    }

    @Test
    public void testManagedClientScheduledExecutor() {
        final String response = target().path("test/scheduledExecutor").request().get(String.class);
        Assert.assertEquals("bar-executor-service bar-executor-service", response);
        System.out.println(response);
    }
}



