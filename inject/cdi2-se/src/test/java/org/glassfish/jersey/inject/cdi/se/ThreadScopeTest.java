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

package org.glassfish.jersey.inject.cdi.se;

import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.parsers.SAXParserFactory;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.PerThread;
import org.glassfish.jersey.process.internal.RequestScope;

import org.hamcrest.core.StringStartsWith;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

/**
 * Testing thread scope integration.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ThreadScopeTest {

    @Test
    public void testThreadScopedInDifferentThread() throws InterruptedException {
        InjectionManager injectionManager = BindingTestHelper.createInjectionManager();
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindAsContract(SingletonObject.class)
                    .in(Singleton.class);

            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .in(PerThread.class);
        });

        SingletonObject instance1 = injectionManager.getInstance(SingletonObject.class);
        Greeting greeting1 = instance1.getGreeting();
        String greetingString1 = greeting1.getGreeting();
        assertThat(greetingString1, StringStartsWith.startsWith(CzechGreeting.GREETING));

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            // Precisely the same object
            SingletonObject instance2 = injectionManager.getInstance(SingletonObject.class);
            Greeting greeting2 = instance2.getGreeting();
            String greetingString2 = greeting2.getGreeting();
            assertThat(greetingString2, StringStartsWith.startsWith(CzechGreeting.GREETING));

            assertNotEquals(greetingString1, greetingString2);
            latch.countDown();
        }).start();

        latch.await();

        SingletonObject instance3 = injectionManager.getInstance(SingletonObject.class);
        assertEquals(instance3.getGreeting().getGreeting(), greetingString1);
    }

    @Test
    public void testThreadScopedInRequestScope() {
        InjectionManager injectionManager = BindingTestHelper.createInjectionManager();
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindAsContract(RequestScopedInterface.class)
                    .in(RequestScoped.class);

            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .in(PerThread.class);
        });

        RequestScope request = injectionManager.getInstance(RequestScope.class);
        request.runInScope(() -> {
            RequestScopedInterface instance1 = injectionManager.getInstance(RequestScopedInterface.class);
            Greeting greeting1 = instance1.getGreeting();
            assertNotNull(greeting1);

            // Precisely the same object
            RequestScopedInterface instance2 = injectionManager.getInstance(RequestScopedInterface.class);
            Greeting greeting2 = instance2.getGreeting();
            assertNotNull(greeting2);

            assertEquals(greeting1, greeting2);
        });
    }

    @Test
    public void testThreadScopedInRequestScopeImplementation() {
        InjectionManager injectionManager = BindingTestHelper.createInjectionManager();
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindAsContract(RequestScopedCzech.class)
                    .in(RequestScoped.class);

            binder.bindFactory(new SupplierGreeting())
                    .to(CzechGreeting.class)
                    .in(PerThread.class);
        });

        RequestScope request = injectionManager.getInstance(RequestScope.class);
        request.runInScope(() -> {
            RequestScopedCzech instance1 = injectionManager.getInstance(RequestScopedCzech.class);
            CzechGreeting greeting1 = instance1.getGreeting();
            assertNotNull(greeting1);

            // Precisely the same object
            RequestScopedCzech instance2 = injectionManager.getInstance(RequestScopedCzech.class);
            CzechGreeting greeting2 = instance2.getGreeting();
            assertNotNull(greeting2);

            assertEquals(greeting1, greeting2);
        });
    }

    @Test
    public void testThreadScopedInRequestTwoTypes() {
        InjectionManager injectionManager = BindingTestHelper.createInjectionManager();
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindAsContract(RequestScopedCzech.class)
                    .in(RequestScoped.class);

            binder.bindAsContract(RequestScopedEnglish.class)
                    .in(RequestScoped.class);

            binder.bindFactory(new SupplierGreeting(CzechGreeting.GREETING))
                    .to(CzechGreeting.class)
                    .in(PerThread.class);

            binder.bindFactory(new SupplierGreeting(EnglishGreeting.GREETING))
                    .to(EnglishGreeting.class)
                    .in(PerThread.class);
        });

        RequestScope request = injectionManager.getInstance(RequestScope.class);
        request.runInScope(() -> {
            RequestScopedCzech instance1 = injectionManager.getInstance(RequestScopedCzech.class);
            CzechGreeting greeting1 = instance1.getGreeting();
            assertNotNull(greeting1);

            // Precisely the same object
            RequestScopedEnglish instance2 = injectionManager.getInstance(RequestScopedEnglish.class);
            EnglishGreeting greeting2 = instance2.getGreeting();
            assertNotNull(greeting2);

            assertNotSame(greeting1, greeting2);
        });
    }

    @Test
    public void testThreadScopedInSingletonScope() {
        InjectionManager injectionManager = BindingTestHelper.createInjectionManager();
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindAsContract(SingletonObject.class)
                    .in(Singleton.class);

            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .in(PerThread.class);
        });

        SingletonObject instance1 = injectionManager.getInstance(SingletonObject.class);
        Greeting greeting1 = instance1.getGreeting();
        assertNotNull(greeting1);

        // Precisely the same object
        SingletonObject instance2 = injectionManager.getInstance(SingletonObject.class);
        Greeting greeting2 = instance2.getGreeting();
        assertNotNull(greeting2);

        assertEquals(greeting1, greeting2);
    }

    @RequestScoped
    public static class RequestScopedProvider {

        @Inject
        Provider<SAXParserFactory> greeting;

        public Provider<SAXParserFactory> provider() {
            return greeting;
        }
    }

    @RequestScoped
    public static class RequestScopedInterface {

        @Inject
        Greeting greeting;

        public Greeting getGreeting() {
            return greeting;
        }
    }

    @RequestScoped
    public static class RequestScopedCzech {

        @Inject
        CzechGreeting greeting;

        public CzechGreeting getGreeting() {
            return greeting;
        }
    }

    @RequestScoped
    public static class RequestScopedEnglish {

        @Inject
        EnglishGreeting greeting;

        public EnglishGreeting getGreeting() {
            return greeting;
        }
    }

    @Singleton
    public static class SingletonObject {

        @Inject
        Greeting greeting;

        public Greeting getGreeting() {
            return greeting;
        }
    }
}
