/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test.util.runner;

import java.lang.reflect.Method;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;

import org.glassfish.jersey.Beta;

/**
 * Jersey implementation of a JUnit {@link Runner} that runs
 * all test methods within a single test class in parallel.
 * The main purpose is to avoid having HTTP container
 * launched separately for each individual test.
 * Order in which individual test methods are invoked
 * is not guaranteed and is non-deterministic.
 *
 * Test methods that needs a separate container or needs
 * to run separately for other reasons
 * could be annotated with {@link RunSeparately} annotation.
 * These test methods will then be excluded
 * from parallel processing, and will be invoked
 * as if no special concurrent runner was involved.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */

@Beta
public class ConcurrentRunner extends BlockJUnit4ClassRunner {

    public final int FINISH_WAIT_CYCLE_MS = 2000;
    public final int TEST_THREADS = 124;

    private static final Logger LOGGER = Logger.getLogger(ConcurrentRunner.class.getName());

    private final ExecutorService executor = Executors.newFixedThreadPool(TEST_THREADS);

    private final Semaphore permitToGo = new Semaphore(1);
    private final AtomicInteger invocations = new AtomicInteger(1);

    /**
     * Create a new runner for given test class.
     *
     * @param clazz test class
     * @throws Throwable
     */
    public ConcurrentRunner(Class<?> clazz) throws Throwable {
        super(clazz);
        concurrentTestMethods = new LinkedList<>(super.computeTestMethods());
        concurrentTestMethods.removeAll(getTestClass().getAnnotatedMethods(RunSeparately.class));
        final List<FrameworkMethod> ignored = getTestClass().getAnnotatedMethods(Ignore.class);
        concurrentTestMethods.removeAll(ignored);
    }

    @Override
    protected void validateTestMethods(List<Throwable> errors) {
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        synchronized (permitToGo) {
            if (concurrentTestMethods.contains(method)) {
                if (invocations.compareAndSet(1, 0)) {
                    runThemAll(concurrentTestMethods, notifier);
                }
            } else {
                super.runChild(method, notifier);
            }
        }
    }

    private final List<FrameworkMethod> concurrentTestMethods;


    private void runThemAll(final List<FrameworkMethod> methods, final RunNotifier notifier) {

        final Object testInstance;
        try {
            testInstance = super.createTest();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        // run the before methods
        List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);
        for (FrameworkMethod before : befores) {
            try {
                before.getMethod().invoke(testInstance);
            } catch (Exception ex) {
                LOGGER.log(java.util.logging.Level.SEVERE, null, ex);
            }

            final AtomicInteger submitted = new AtomicInteger(0);

            for (final FrameworkMethod method : methods) {

                try {

                    notifier.fireTestStarted(describeChild(method));

                    final Method javaTestMethod = method.getMethod();

                    final Object[] javaMethodArgs = new Object[]{};
                    submitted.incrementAndGet();
                    executor.submit(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                javaTestMethod.invoke(testInstance, javaMethodArgs);
                            } catch (Exception ex) {
                                notifier.fireTestFailure(new Failure(describeChild(method), ex));
                            } finally {
                                submitted.decrementAndGet();
                            }
                        }
                    });
                } catch (Exception ex) {
                    notifier.fireTestFailure(new Failure(describeChild(method), ex));
                    return;
                }
                notifier.fireTestFinished(describeChild(method));
            }

            // wait until everything is done
            while (submitted.intValue() > 0) {
                LOGGER.info(String.format("Waiting for %d requests to finish...%n", submitted.intValue()));
                try {
                    Thread.sleep(FINISH_WAIT_CYCLE_MS);
                } catch (InterruptedException e) {
                }
            }

            // and launch the after party..
            List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);
            for (FrameworkMethod after : afters) {
                try {
                    after.getMethod().invoke(testInstance);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}