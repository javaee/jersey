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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import javax.enterprise.inject.InjectionException;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link CachedConstructorAnalyzer}.
 */
public class CachedConstructorAnalyzerTest {

    private static final Collection<Class<? extends Annotation>> ANNOTATIONS =
            Arrays.asList(Context.class, PathParam.class);

    @Test
    public void testDefaultConstructor() {
        CachedConstructorAnalyzer<DefaultConstructor> analyzer =
                new CachedConstructorAnalyzer<>(DefaultConstructor.class, ANNOTATIONS);

        assertEquals(0, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testNoArgsConstructor() {
        CachedConstructorAnalyzer<NoArgsConstructor> analyzer =
                new CachedConstructorAnalyzer<>(NoArgsConstructor.class, ANNOTATIONS);

        assertEquals(0, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testSingleAnnotatedConstructor() {
        CachedConstructorAnalyzer<SingleAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(SingleAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(1, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testSingleMultiAnnotatedConstructor() {
        CachedConstructorAnalyzer<SingleMultiAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(SingleMultiAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(2, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testLargestAnnotatedConstructor() {
        CachedConstructorAnalyzer<LargestAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(LargestAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(3, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testContainsSmallerNonAnnotatedConstructor() {
        CachedConstructorAnalyzer<ContainsSmallerNonAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(ContainsSmallerNonAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(2, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testContainsLargerNonAnnotatedConstructor() {
        CachedConstructorAnalyzer<ContainsLargerNonAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(ContainsLargerNonAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(1, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testSameNonAnnotatedConstructor() {
        CachedConstructorAnalyzer<SameNonAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(SameNonAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(1, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testBothAnnotatedConstructor() {
        CachedConstructorAnalyzer<BothAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(BothAnnotatedConstructor.class, ANNOTATIONS);

        Constructor<BothAnnotatedConstructor> constructor = analyzer.getConstructor();
        assertEquals(1, constructor.getParameterCount());
        assertEquals(Integer.class, constructor.getParameterTypes()[0]);
    }

    @Test
    public void testOneNonAnnotatedConstructor() {
        CachedConstructorAnalyzer<OneNonAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(OneNonAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(1, analyzer.getConstructor().getParameterCount());
    }

    @Test
    public void testMultiAnnotatedConstructor() {
        CachedConstructorAnalyzer<MultiAnnotatedConstructor> analyzer =
                new CachedConstructorAnalyzer<>(MultiAnnotatedConstructor.class, ANNOTATIONS);

        assertEquals(2, analyzer.getConstructor().getParameterCount());
    }

    @Test(expected = InjectionException.class)
    public void testUnknownAnnotatedConstructor() {
        new CachedConstructorAnalyzer<>(UnknownAnnotatedConstructor.class, ANNOTATIONS).getConstructor();
    }

    @Test(expected = InjectionException.class)
    public void testSingleNonAnnotatedConstructor() {
        new CachedConstructorAnalyzer<>(SingleNonAnnotatedConstructor.class, ANNOTATIONS).getConstructor();
    }

    public static class DefaultConstructor {
    }

    public static class NoArgsConstructor {
        public NoArgsConstructor() {
        }
    }

    public static class SingleNonAnnotatedConstructor {
        public SingleNonAnnotatedConstructor(String str) {
        }
    }

    public static class SingleAnnotatedConstructor {
        public SingleAnnotatedConstructor(@Context String str) {
        }
    }

    public static class SingleMultiAnnotatedConstructor {
        public SingleMultiAnnotatedConstructor(@Context String str, @PathParam("name") String name) {
        }
    }

    public static class LargestAnnotatedConstructor {
        public LargestAnnotatedConstructor(@Context String str, @PathParam("name") String name, @Context String str2) {
        }

        public LargestAnnotatedConstructor(@Context String str) {
        }

        public LargestAnnotatedConstructor(@Context String str, @PathParam("name") String name) {
        }
    }

    public static class ContainsSmallerNonAnnotatedConstructor {
        public ContainsSmallerNonAnnotatedConstructor(String str) {
        }

        public ContainsSmallerNonAnnotatedConstructor(@Context String str, @PathParam("name") String name) {
        }
    }

    public static class ContainsLargerNonAnnotatedConstructor {
        public ContainsLargerNonAnnotatedConstructor(@Context String str) {
        }

        public ContainsLargerNonAnnotatedConstructor(String str, String name) {
        }
    }

    public static class SameNonAnnotatedConstructor {
        public SameNonAnnotatedConstructor(@Context String str) {
        }

        public SameNonAnnotatedConstructor(Integer name) {
        }
    }

    public static class BothAnnotatedConstructor {
        public BothAnnotatedConstructor(@Context String str) {
        }

        public BothAnnotatedConstructor(@Context Integer name) {
        }
    }

    public static class OneNonAnnotatedConstructor {
        public OneNonAnnotatedConstructor(@Context String str) {
        }

        public OneNonAnnotatedConstructor(@Context Integer name, String str) {
        }
    }

    public static class MultiAnnotatedConstructor {
        public MultiAnnotatedConstructor(@Context Integer name, @PathParam("str") @Context String str) {
        }
    }

    public static class UnknownAnnotatedConstructor {
        public UnknownAnnotatedConstructor(@Context Integer name, @MatrixParam("matrix") String str) {
        }
    }
}
