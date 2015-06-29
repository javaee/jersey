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

package org.glassfish.jersey.process.internal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledThreadPoolExecutorProvider;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.Unqualified;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.jvnet.hk2.annotations.Optional;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * ExecutorProviders unit tests.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ExecutorProvidersTest extends AbstractBinder {

    /**
     * Custom scheduler injection qualifier.
     */
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface CustomScheduler {

    }

    /**
     * Custom scheduler provider.
     */
    @CustomScheduler
    public static class CustomSchedulerProvider extends ScheduledThreadPoolExecutorProvider {

        /**
         * Create a new instance of the scheduled thread pool executor provider.
         */
        public CustomSchedulerProvider() {
            super("custom-scheduler");
        }
    }

    /**
     * Custom named scheduler provider.
     */
    @Named("custom-scheduler")
    public static class CustomNamedSchedulerProvider extends ScheduledThreadPoolExecutorProvider {

        /**
         * Create a new instance of the scheduled thread pool executor provider.
         */
        public CustomNamedSchedulerProvider() {
            super("custom-named-scheduler");
        }
    }

    /**
     * Custom executor injection qualifier.
     */
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface CustomExecutor {

    }

    /**
     * Custom executor provider.
     */
    @CustomExecutor
    public static class CustomExecutorProvider extends ThreadPoolExecutorProvider {

        /**
         * Create a new instance of the thread pool executor provider.
         */
        public CustomExecutorProvider() {
            super("custom-executor");
        }
    }

    /**
     * Custom named executor provider.
     */
    @Named("custom-executor")
    public static class CustomNamedExecutorProvider extends ThreadPoolExecutorProvider {

        /**
         * Create a new instance of the thread pool executor provider.
         */
        public CustomNamedExecutorProvider() {
            super("custom-named-executor");
        }
    }

    /**
     * A task to retrieve the current thread name.
     */
    public static class CurrentThreadNameRetrieverTask implements Producer<String> {

        @Override
        public String call() {
            return Thread.currentThread().getName();
        }
    }

    /**
     * Notifier of pre-destroy method invocation.
     */
    public static class PreDestroyNotifier {

        private final CountDownLatch latch = new CountDownLatch(1);

        @PreDestroy
        public void preDestroy() {
            latch.countDown();
        }

        public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    /**
     * Injectable executor client class.
     */
    public static class InjectedExecutorClient {

        @Inject
        private PreDestroyNotifier preDestroyNotifier;

        @Inject
        @Unqualified // this will prevent HK2 from injecting using qualified injection bindings
        @Optional // This will prevent HK2 to fail due to an unsatisfied injection binding
        private ExecutorService unqualifiedExecutor;

        @Inject
        @Unqualified // this will prevent HK2 from injecting using qualified injection bindings
        @Optional // This will prevent HK2 to fail due to an unsatisfied injection binding
        private ScheduledExecutorService unqualifiedScheduler;

        @Inject
        @CustomExecutor
        private ExecutorService customExecutor;

        @Inject
        @Named("custom-executor")
        private ExecutorService customNamedExecutor;

        @Inject
        @Optional
        @CustomExecutor
        private ScheduledExecutorService customExecutorAsScheduler;

        @Inject
        @Named("custom-executor")
        @Optional
        private ScheduledExecutorService customNamedExecutorAsScheduler;

        @Inject
        @CustomScheduler
        private ScheduledExecutorService customScheduler;

        @Inject
        @CustomScheduler
        private ExecutorService customSchedulerAsExecutor;

        @Inject
        @Named("custom-scheduler")
        private ScheduledExecutorService customNamedScheduler;

        @Inject
        @Named("custom-scheduler")
        private ScheduledExecutorService customNamedSchedulerAsExecutor;

    }

    private ServiceLocator locator;

    @Override
    protected void configure() {
        bind(CustomExecutorProvider.class).to(ExecutorServiceProvider.class).in(Singleton.class);
        bind(CustomNamedExecutorProvider.class).to(ExecutorServiceProvider.class).in(Singleton.class);
        bind(CustomSchedulerProvider.class).to(ScheduledExecutorServiceProvider.class).in(Singleton.class);
        bind(CustomNamedSchedulerProvider.class).to(ScheduledExecutorServiceProvider.class).in(Singleton.class);
        bindAsContract(PreDestroyNotifier.class).in(Singleton.class);
    }

    /**
     * Set-up the tests.
     */
    @Before
    public void setup() {
        locator = Injections.createLocator(this);
        ExecutorProviders.createInjectionBindings(locator);
    }

    /**
     * Test executor and scheduler injection as well as the proper shutdown when service locator is closed.
     *
     * @throws Exception in case of a test error.
     */
    @Test
    public void testExecutorInjectionAndReleasing() throws Exception {
        final InjectedExecutorClient executorClient = Injections.getOrCreate(locator, InjectedExecutorClient.class);

        // Check expected injection points state
        assertThat(executorClient.unqualifiedExecutor, Matchers.nullValue());
        assertThat(executorClient.unqualifiedScheduler, Matchers.nullValue());

        assertThat(executorClient.customExecutor, Matchers.notNullValue());
        assertThat(executorClient.customNamedExecutor, Matchers.notNullValue());
        assertThat(executorClient.customExecutorAsScheduler, Matchers.nullValue());
        assertThat(executorClient.customNamedExecutorAsScheduler, Matchers.nullValue());

        assertThat(executorClient.customScheduler, Matchers.notNullValue());
        assertThat(executorClient.customNamedScheduler, Matchers.notNullValue());
        assertThat(executorClient.customSchedulerAsExecutor, Matchers.notNullValue());
        assertThat(executorClient.customNamedSchedulerAsExecutor, Matchers.notNullValue());

        CurrentThreadNameRetrieverTask nameRetrieverTask = new CurrentThreadNameRetrieverTask();

        // Test authenticity of injected executors
        assertThat(executorClient.customExecutor.submit(nameRetrieverTask).get(),
                Matchers.startsWith("custom-executor-"));
        assertThat(executorClient.customNamedExecutor.submit(nameRetrieverTask).get(),
                Matchers.startsWith("custom-named-executor-"));

        // Test authenticity of injected schedulers
        assertThat(executorClient.customScheduler.submit(nameRetrieverTask).get(),
                Matchers.startsWith("custom-scheduler-"));
        assertThat(executorClient.customNamedScheduler.submit(nameRetrieverTask).get(),
                Matchers.startsWith("custom-named-scheduler-"));
        assertThat(executorClient.customSchedulerAsExecutor.submit(nameRetrieverTask).get(),
                Matchers.startsWith("custom-scheduler-"));
        assertThat(executorClient.customNamedSchedulerAsExecutor.submit(nameRetrieverTask).get(),
                Matchers.startsWith("custom-named-scheduler-"));

        // Test proper executor shutdown when locator is shut down.
        Injections.shutdownLocator(locator);

        assertThat("Waiting for pre-destroy timed out.",
                executorClient.preDestroyNotifier.await(3, TimeUnit.SECONDS), Matchers.is(true));

        testShutDown("customExecutor", executorClient.customExecutor);
        testShutDown("customNamedExecutor", executorClient.customNamedExecutor);
        testShutDown("customScheduler", executorClient.customScheduler);
        testShutDown("customNamedScheduler", executorClient.customNamedScheduler);
        testShutDown("customSchedulerAsExecutor", executorClient.customSchedulerAsExecutor);
        testShutDown("customNamedSchedulerAsExecutor", executorClient.customNamedSchedulerAsExecutor);
    }

    private void testShutDown(String name, ExecutorService executorService) throws InterruptedException {
        assertTrue(name + " not shutdown", executorService.isShutdown());
        assertTrue(name + " not terminated", executorService.isTerminated());
    }

}
