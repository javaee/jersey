/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.gf.cdi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.MultiException;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Class analyzer that ignores {@link javax.inject.Provider} injection points
 * to avoid replacing CDI provided injections.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public final class ProviderSkippingClassAnalyzer implements ClassAnalyzer {

    /**
     * Name to be used when binding this analyzer to HK2 service locator.
     */
    public static final String NAME = "ProviderSkippingClassAnalyzer";

    private final ClassAnalyzer defaultAnalyzer;

    @Inject
    public ProviderSkippingClassAnalyzer(@Named(ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME) ClassAnalyzer defaultAnalyzer) {
        this.defaultAnalyzer = defaultAnalyzer;
    }


    @Override
    public <T> Constructor<T> getConstructor(Class<T> type) throws MultiException, NoSuchMethodException {
        throw new IllegalStateException(LocalizationMessages.CDI_CLASS_ANALYZER_MISUSED());
    }

    @Override
    public <T> Set<Method> getInitializerMethods(Class<T> type) throws MultiException {
        return removeProviderSetters(defaultAnalyzer.getInitializerMethods(type));
    }

    @Override
    public <T> Set<Field> getFields(Class<T> type) throws MultiException {
        return removeProviderFields(defaultAnalyzer.getFields(type));
    }

    @Override
    public <T> Method getPostConstructMethod(Class<T> type) throws MultiException {
        throw new IllegalStateException(LocalizationMessages.CDI_CLASS_ANALYZER_MISUSED());
    }

    @Override
    public <T> Method getPreDestroyMethod(Class<T> type) throws MultiException {
        throw new IllegalStateException(LocalizationMessages.CDI_CLASS_ANALYZER_MISUSED());
    }

    /**
     * Filter out methods that take exactly one parameter typed as {@link Provider}.
     * These methods are expected to be covered by CDI container.
     *
     * @param set of methods to be filtered.
     * @return input set of methods without Provider setters.
     */
    private Set<Method> removeProviderSetters(Set<Method> initializerMethods) {
        return Sets.filter(initializerMethods, new Predicate<Method>(){

            @Override
            public boolean apply(Method m) {
                final Class<?>[] parameterTypes = m.getParameterTypes();
                return parameterTypes == null || parameterTypes.length != 1 || parameterTypes[0] != Provider.class;
            }
        });
    }

    /**
     * Filter out {@link Provider} fields. These are expected to be covered by CDI container.
     * @param fields set of fields to be filtered.
     * @return input set with Providers filtered out.
     */
    private Set<Field> removeProviderFields(Set<Field> fields) {
        return Sets.filter(fields, new Predicate<Field>(){

            @Override
            public boolean apply(Field t) {
                return t.getType() != Provider.class;
            }
        });
    }
}
