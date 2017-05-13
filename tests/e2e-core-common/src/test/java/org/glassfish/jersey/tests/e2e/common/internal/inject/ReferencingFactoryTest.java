/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common.internal.inject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.GenericType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;

import org.junit.Test;
import static org.junit.Assert.assertSame;

/**
 * Referencing factory test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ReferencingFactoryTest extends AbstractBinder {

    private static class Foo {
        final int value;

        private Foo(int value) {
            this.value = value;
        }
    }

    private static class ValueInjected {

        @Inject
        Foo foo;
        @Inject
        List<Integer> integers;
        @Inject
        List<String> strings;
    }

    private static class RefInjected {

        @Inject
        Ref<Foo> foo;
        @Inject
        Ref<List<Integer>> integers;
        @Inject
        Ref<List<String>> strings;
    }

    //
    private Foo expectedFoo = null;
    private List<Integer> expectedIntegers = null;
    private List<String> expectedStrings = new LinkedList<String>();

    private static final class FooReferencingFactory extends ReferencingFactory<Foo> {
        @Inject
        public FooReferencingFactory(Provider<Ref<Foo>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static final class ListOfIntegerReferencingFactory extends ReferencingFactory<List<Integer>> {
        @Inject
        public ListOfIntegerReferencingFactory(Provider<Ref<List<Integer>>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static final class ListOfStringReferencingFactory extends ReferencingFactory<List<String>> {
        @Inject
        public ListOfStringReferencingFactory(Provider<Ref<List<String>>> referenceFactory) {
            super(referenceFactory);
        }
    }

    @Override
    protected void configure() {
        bindFactory(FooReferencingFactory.class).to(Foo.class);
        bindFactory(ReferencingFactory.referenceFactory()).to(new GenericType<Ref<Foo>>() {}).in(Singleton.class);

        bindFactory(ListOfIntegerReferencingFactory.class).to(new GenericType<List<Integer>>() {});
        bindFactory(ReferencingFactory.referenceFactory()).to(new GenericType<Ref<List<Integer>>>() {
        }).in(Singleton.class);

        bindFactory(ListOfStringReferencingFactory.class).to(new GenericType<List<String>>() {});
        bindFactory(ReferencingFactory.referenceFactory(expectedStrings)).to(new GenericType<Ref<List<String>>>() {
        }).in(Singleton.class);
    }

    /**
     * Referenced binding test.
     */
    @Test
    public void testReferencedBinding() {
        InjectionManager injectionManager = Injections.createInjectionManager(this);
        injectionManager.completeRegistration();

        RefInjected refValues = injectionManager.createAndInitialize(RefInjected.class);
        expectedFoo = new Foo(10);
        refValues.foo.set(expectedFoo);
        expectedIntegers = new LinkedList<Integer>();
        refValues.integers.set(expectedIntegers);
        expectedStrings = new ArrayList<String>();
        refValues.strings.set(expectedStrings);

        ValueInjected updatedValues = injectionManager.createAndInitialize(ValueInjected.class);
        assertSame(expectedFoo, updatedValues.foo);
        assertSame(expectedIntegers, updatedValues.integers);
        assertSame(expectedStrings, updatedValues.strings);
    }
}
