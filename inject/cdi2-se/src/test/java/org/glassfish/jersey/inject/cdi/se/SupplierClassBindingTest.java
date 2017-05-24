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

import java.util.function.Supplier;

import javax.enterprise.inject.Vetoed;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.InjectionManager;

import org.hamcrest.core.StringStartsWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * Tests that {@link Supplier} can be registered as a class-factory.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@Vetoed
public class SupplierClassBindingTest {

    private InjectionManager injectionManager;

    @Before
    public void setup() {
        injectionManager = BindingTestHelper.createInjectionManager();
    }

    @After
    public void teardown() {
        injectionManager.shutdown();
    }

    @Test
    public void testMessages() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Conversation conversation = injectionManager.getInstance(Conversation.class);
        assertThat(conversation.greeting.getGreeting(), StringStartsWith.startsWith(CzechGreeting.GREETING));
    }

    @Test
    public void testSupplierPerLookupInstancePerLookup() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Conversation conversation1 = injectionManager.getInstance(Conversation.class);
        Greeting greeting1 = conversation1.greeting;
        Conversation conversation2 = injectionManager.getInstance(Conversation.class);
        Greeting greeting2 = conversation2.greeting;
        Conversation conversation3 = injectionManager.getInstance(Conversation.class);
        Greeting greeting3 = conversation3.greeting;

        assertNotSame(greeting1, greeting2);
        assertNotSame(greeting2, greeting3);

        Supplier<Greeting> supplier1 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier2 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier3 = injectionManager.getInstance(Conversation.class).greetingSupplier;

        assertNotSame(supplier1, supplier2);
        assertNotSame(supplier2, supplier3);
    }

    @Test
    public void testSupplierSingletonInstancePerLookup() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class, Singleton.class).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Greeting greeting1 = injectionManager.getInstance(Conversation.class).greeting;
        Greeting greeting2 = injectionManager.getInstance(Conversation.class).greeting;
        Greeting greeting3 = injectionManager.getInstance(Conversation.class).greeting;

        assertNotSame(greeting1, greeting2);
        assertNotSame(greeting2, greeting3);

        Supplier<Greeting> supplier1 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier2 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier3 = injectionManager.getInstance(Conversation.class).greetingSupplier;

        assertSame(supplier1, supplier2);
        assertSame(supplier2, supplier3);
    }

    @Test
    public void testSupplierPerLookupInstanceSingleton() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(Greeting.class).in(Singleton.class);
            binder.bindAsContract(Conversation.class);
        });

        Greeting greeting1 = injectionManager.getInstance(Conversation.class).greeting;
        Greeting greeting2 = injectionManager.getInstance(Conversation.class).greeting;
        Greeting greeting3 = injectionManager.getInstance(Conversation.class).greeting;

        assertSame(greeting1, greeting2);
        assertSame(greeting2, greeting3);

        Supplier<Greeting> supplier1 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier2 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier3 = injectionManager.getInstance(Conversation.class).greetingSupplier;

        assertNotSame(supplier1, supplier2);
        assertNotSame(supplier2, supplier3);
    }

    @Test
    public void testSupplierSingletonInstanceSingleton() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class, Singleton.class).to(Greeting.class).in(Singleton.class);
            binder.bindAsContract(Conversation.class);
        });

        Greeting greeting1 = injectionManager.getInstance(Conversation.class).greeting;
        Greeting greeting2 = injectionManager.getInstance(Conversation.class).greeting;
        Greeting greeting3 = injectionManager.getInstance(Conversation.class).greeting;

        assertSame(greeting1, greeting2);
        assertSame(greeting2, greeting3);

        Supplier<Greeting> supplier1 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier2 = injectionManager.getInstance(Conversation.class).greetingSupplier;
        Supplier<Greeting> supplier3 = injectionManager.getInstance(Conversation.class).greetingSupplier;

        assertSame(supplier1, supplier2);
        assertSame(supplier2, supplier3);
    }
}
