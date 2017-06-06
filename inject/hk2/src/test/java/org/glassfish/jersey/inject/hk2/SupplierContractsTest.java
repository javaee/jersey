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

package org.glassfish.jersey.inject.hk2;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.InjectionManager;

import org.glassfish.hk2.api.MultiException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Tests that {@link java.util.function.Supplier} can contain multiple contracts.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class SupplierContractsTest {

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
    public void testClassFactoryInstanceInterface() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Conversation conversation = injectionManager.getInstance(Conversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testClassFactoryInstanceImplementation() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(CzechGreeting.class);
            binder.bindAsContract(CzechConversation.class);
        });

        CzechConversation conversation = injectionManager.getInstance(CzechConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testInstanceFactoryInstanceInterface() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting()).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Conversation conversation = injectionManager.getInstance(Conversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testInstanceFactoryInstanceImplementation() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting()).to(CzechGreeting.class);
            binder.bindAsContract(CzechConversation.class);
        });

        CzechConversation conversation = injectionManager.getInstance(CzechConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testClassFactoryMultipleContracts() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class)
                    .to(Greeting.class)
                    .to(Printable.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertNotSame(conversation.greeting, conversation.printable);
        assertNotSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testClassFactorySingletonMultipleContracts() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class, Singleton.class)
                    .to(Greeting.class)
                    .to(Printable.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertNotSame(conversation.greeting, conversation.printable);
        assertSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testClassFactoryMultipleContractsSingleton() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class)
                    .to(Greeting.class)
                    .to(Printable.class)
                    .in(Singleton.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertSame(conversation.greeting, conversation.printable);
        assertNotSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testInstanceFactoryMultipleContracts() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .to(Printable.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertNotSame(conversation.greeting, conversation.printable);
        assertSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testInstanceFactoryMultipleContractsSingleton() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .to(Printable.class)
                    .in(Singleton.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertSame(conversation.greeting, conversation.printable);
        assertSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test(expected = MultiException.class)
    public void testClassFactoryFailedWrongImplementation() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(EnglishGreeting.class);
            binder.bindAsContract(Conversation.class);
        });

        injectionManager.getInstance(Conversation.class);
    }

    @Test(expected = MultiException.class)
    public void testInstanceFactoryFailsWrongImplementation() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting()).to(EnglishGreeting.class);
            binder.bindAsContract(Conversation.class);
        });

        injectionManager.getInstance(Conversation.class);
    }

    @Test(expected = MultiException.class)
    public void testFailsImplementationButInterfaceExpected() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting()).to(CzechGreeting.class);
            binder.bindAsContract(Conversation.class);
        });

        injectionManager.getInstance(Conversation.class);
    }
}
