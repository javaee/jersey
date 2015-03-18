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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;

import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;

import org.glassfish.jersey.Beta;

/**
 * Custom implementation of a JUnit {@link Runner} that allows parameterized
 * tests to run in parallel. This runner will probably
 * be merged into {@link ConcurrentRunner} in the future.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Beta
public class ConcurrentParameterizedRunner extends BlockJUnit4ClassRunner {

    public final int FINISH_WAIT_CYCLE_MS = 2000;
    public final int TEST_THREADS = 124;

    private static final Logger LOGGER = Logger.getLogger(ConcurrentParameterizedRunner.class.getName());

    private final ExecutorService executor = Executors.newFixedThreadPool(TEST_THREADS);

    /**
     * Create a new runner for given test class.
     *
     * @param clazz test class
     * @throws Throwable
     */
    public ConcurrentParameterizedRunner(Class<?> clazz) throws Throwable {
        super(clazz);
    }

    @Override
    protected void validateTestMethods(List<Throwable> errors) {
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        notifier.fireTestStarted(describeChild(method));

        final Object testInstance;
        try {

            // get the test parameter iterator first
            final List<FrameworkMethod> parameterMethods = getTestClass().getAnnotatedMethods(Parameterized.Parameters.class);
            final Iterable<Object[]> parameters = (Iterable<Object[]>) parameterMethods.get(0).getMethod().invoke(null);

            // then create the test instance
            testInstance = super.createTest();

            // now run the before methods
            List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);
            for (FrameworkMethod before : befores) {
                before.getMethod().invoke(testInstance);
            }

            // and launch as meny test method invocations as many parameters is available
            final Iterator<Object[]> paramIterator = parameters.iterator();
            final Method javaTestMethod = method.getMethod();

            final AtomicInteger submitted = new AtomicInteger(0);

            while (paramIterator.hasNext()) {

                final Object[] javaMethodArgs = paramIterator.next();
                submitted.incrementAndGet();
                executor.submit(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            javaTestMethod.invoke(testInstance, javaMethodArgs);
                        } catch (IllegalAccessException ex) {
                            notifier.fireTestFailure(new Failure(describeChild(method), ex));
                        } catch (IllegalArgumentException ex) {
                            notifier.fireTestFailure(new Failure(describeChild(method), ex));
                        } catch (InvocationTargetException ex) {
                            notifier.fireTestFailure(new Failure(describeChild(method), ex));
                        } finally {
                            submitted.decrementAndGet();
                        }
                    }
                });
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
                after.getMethod().invoke(testInstance);
            }
        } catch (Exception ex) {
            notifier.fireTestFailure(new Failure(describeChild(method), ex));
            return;
        }
        notifier.fireTestFinished(describeChild(method));
    }
}
