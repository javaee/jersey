/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.ext.cdi1x.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.MultiException;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Class analyzer that ignores given injection points.
 * Used for CDI integration, where we need to avoid HK2 replacing CDI injected entities.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public final class InjecteeSkippingAnalyzer implements ClassAnalyzer {

    private final ClassAnalyzer defaultAnalyzer;
    private final Map<Class<?>, Set<Method>> methodsToSkip;
    private final Map<Class<?>, Set<Field>> fieldsToSkip;

    public InjecteeSkippingAnalyzer(ClassAnalyzer defaultAnalyzer,
                                    Map<Class<?>, Set<Method>> methodsToSkip,
                                    Map<Class<?>, Set<Field>> fieldsToSkip) {
        this.defaultAnalyzer = defaultAnalyzer;
        this.methodsToSkip = methodsToSkip;
        this.fieldsToSkip = fieldsToSkip;
    }

    @Override
    public <T> Constructor<T> getConstructor(Class<T> type) throws MultiException, NoSuchMethodException {
        throw new IllegalStateException(LocalizationMessages.CDI_CLASS_ANALYZER_MISUSED());
    }

    @Override
    public <T> Set<Method> getInitializerMethods(Class<T> type) throws MultiException {
        final Set<Method> originalMethods = defaultAnalyzer.getInitializerMethods(type);
        final Set<Method> skippedMethods = getMembersToSkip(type, methodsToSkip);
        return Sets.difference(originalMethods, skippedMethods);
    }

    @Override
    public <T> Set<Field> getFields(Class<T> type) throws MultiException {
        final Set<Field> originalFields = defaultAnalyzer.getFields(type);
        final Set<Field> skippedFields = getMembersToSkip(type, fieldsToSkip);
        return Sets.difference(originalFields, skippedFields);
    }

    @Override
    public <T> Method getPostConstructMethod(Class<T> type) throws MultiException {
        throw new IllegalStateException(LocalizationMessages.CDI_CLASS_ANALYZER_MISUSED());
    }

    @Override
    public <T> Method getPreDestroyMethod(Class<T> type) throws MultiException {
        throw new IllegalStateException(LocalizationMessages.CDI_CLASS_ANALYZER_MISUSED());
    }

    private <M extends Member> Set<M> getMembersToSkip(final Class<?> type, final Map<Class<?>, Set<M>> skippedMembers) {

        final Set<M> directResult = skippedMembers.get(type);

        if (directResult != null) {
            return directResult;
        }

        // fallback for GLASSFISH-20255
        final Set<M> compositeResult = new HashSet<>();
        for (Entry<Class<?>, Set<M>> type2Method : skippedMembers.entrySet()) {

            if (type2Method.getKey().isAssignableFrom(type)) {
                compositeResult.addAll(type2Method.getValue());
            }
        }

        return compositeResult;
    }
}
