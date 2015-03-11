/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;


import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Parameter model creation test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ParameterTest {

    private static interface GenericContractInterface<T> {
        public abstract T process(T param);
    }

    private abstract static class GenericContractClass<T> {
        public abstract T process(T param);
    }

    private static interface ContractInterface {
        public String process(String param);
    }

    private abstract static class ContractClass {
        public abstract String process(String param);
    }

    private static class StandaloneServiceClass {
        public String process(String param) {
            return null;
        }
    }

    private static class GenericCIService implements GenericContractInterface<String> {

        @Override
        public String process(String param) {
            return null;
        }
    }

    private static class GenericCIGenericService implements GenericContractInterface<List<String>> {

        @Override
        public List<String> process(List<String> param) {
            return null;
        }
    }

    private static class GenericCCService extends GenericContractClass<String> {

        @Override
        public String process(String param) {
            return null;
        }
    }

    private static class GenericCCGenericService extends GenericContractClass<List<String>> {

        @Override
        public List<String> process(List<String> param) {
            return null;
        }
    }

    private static class CIService implements ContractInterface {

        @Override
        public String process(String param) {
            return null;
        }
    }

    private static class CCService extends ContractClass {

        @Override
        public String process(String param) {
            return null;
        }
    }


    private static class GenericCCGenericArrayService extends GenericContractClass<byte[]> {

        @Override
        public byte[] process(byte[] param) {
            return null;
        }
    }

    private static class GenericCIGenericArrayService implements GenericContractInterface<byte[]> {

        @Override
        public byte[] process(byte[] param) {
            return null;
        }
    }

    public void testParameterCreation() throws NoSuchMethodException {
        Class<?> implementing = StandaloneServiceClass.class;
        Class<?> declaring = StandaloneServiceClass.class;
        Method method = declaring.getMethod("process", Object.class);
        final List<Parameter> parameters = Parameter.create(implementing, declaring, method, false);
        assertEquals(1, parameters.size());
        Parameter parameter = parameters.get(0);
        assertEquals(String.class, parameter.getRawType());
        assertEquals(String.class, parameter.getType());
    }

    /**
     * JERSEY-2408 Fix test - missing hashCode() and equals() in {@link Parameter} caused
     * the descriptorCache in {@link org.glassfish.jersey.server.internal.inject.DelegatedInjectionValueFactoryProvider} not to
     * reuse the Parameter instances (Parameter was used as a key in a {@link org.glassfish.hk2.utilities.cache.Cache},
     * which delegates to an underlying {@link java.util.concurrent.ConcurrentHashMap}.
     */
    @Test
    public void testParameterHashCode() {
        Annotation[] annotations = new Annotation[]{new Annotation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Inject.class;
            }
        }};
        Parameter param1 = Parameter.create(String.class, String.class, false, String.class, String.class, annotations);
        Parameter param2 = Parameter.create(String.class, String.class, false, String.class, String.class, annotations);
        Parameter param3 = Parameter.create(Integer.class, Integer.class, false, Integer.class, Integer.class, annotations);

        assertEquals(param1, param2);
        assertEquals(param1.hashCode(), param2.hashCode());
        assertNotEquals(param1, param3);
        assertNotEquals(param1.hashCode(), param3.hashCode());
    }

}
