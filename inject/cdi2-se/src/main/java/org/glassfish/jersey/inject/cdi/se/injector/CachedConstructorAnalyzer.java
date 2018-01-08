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
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.logging.Logger;

import javax.enterprise.inject.InjectionException;
import javax.inject.Inject;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

/**
 * Processes a provided class and selects the valid constructor with the largest number of parameters. Constructor is cached
 * for a later retrieve.
 */
public class CachedConstructorAnalyzer<T> {

    private static final Logger LOGGER = Logger.getLogger(CachedConstructorAnalyzer.class.getName());

    private final LazyValue<Constructor<T>> constructor;
    private final Collection<Class<? extends Annotation>> resolverAnnotations;

    /**
     * Creates a new constructor analyzer which accepts the class that is analyzed.
     *
     * @param clazz       analyzed class.
     * @param annotations all annotations used for an injecting.
     */
    public CachedConstructorAnalyzer(Class<T> clazz, Collection<Class<? extends Annotation>> annotations) {
        this.resolverAnnotations = annotations;
        this.constructor = Values.lazy((Value<Constructor<T>>) () -> getConstructorInternal(clazz));
    }

    public Constructor<T> getConstructor() {
        return constructor.get();
    }

    public boolean hasCompatibleConstructor() {
        try {
            return constructor.get() != null;
        } catch (InjectionException ex) {
            // Compatible constructor was not found.
            return false;
        }
    }

    /**
     * Select the proper constructor of the given {@code clazz}.
     *
     * @return compatible and largest constructor.
     */
    @SuppressWarnings("unchecked")
    private Constructor<T> getConstructorInternal(Class<T> clazz) {
        if (clazz.isLocalClass()) {
            throw new InjectionException(
                    LocalizationMessages.INJECTION_ERROR_LOCAL_CLASS_NOT_SUPPORTED(clazz.getName()));
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new InjectionException(
                    LocalizationMessages.INJECTION_ERROR_NONSTATIC_MEMBER_CLASS_NOT_SUPPORTED(clazz.getName()));
        }

        // At this point, we simply need to find the constructor with the largest number of parameters
        Constructor<?>[] constructors = AccessController.doPrivileged(
                (PrivilegedAction<Constructor<?>[]>) clazz::getDeclaredConstructors);
        Constructor<?> selected = null;
        int selectedSize = 0;
        int maxParams = -1;

        for (Constructor<?> constructor : constructors) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length >= maxParams && isCompatible(constructor)) {
                if (params.length > maxParams) {
                    maxParams = params.length;
                    selectedSize = 0;
                }

                selected = constructor;
                selectedSize++;
            }
        }

        if (selectedSize == 0) {
            throw new InjectionException(LocalizationMessages.INJECTION_ERROR_SUITABLE_CONSTRUCTOR_NOT_FOUND(clazz.getName()));
        }

        if (selectedSize > 1) {
            // Found {0} constructors with {1} parameters in {2} class. Selecting the first found constructor: {3}
            LOGGER.warning(LocalizationMessages.MULTIPLE_MATCHING_CONSTRUCTORS_FOUND(
                    selectedSize, maxParams, clazz.getName(), selected.toGenericString()));
        }

        return (Constructor<T>) selected;
    }

    /**
     * Checks whether the constructor is valid for injection that means that all parameters has an injection annotation.
     *
     * @param constructor constructor to inject.
     * @return True if element contains at least one inject annotation.
     */
    @SuppressWarnings("MagicConstant")
    private boolean isCompatible(Constructor<?> constructor) {
        if (constructor.getAnnotation(Inject.class) != null) {
            // JSR-330 applicable
            return true;
        }

        int paramSize = constructor.getParameterTypes().length;
        if (paramSize != 0 && resolverAnnotations.isEmpty()) {
            return false;
        }

        if (!Modifier.isPublic(constructor.getModifiers())) {
            // return true for a default constructor, return false otherwise.
            return paramSize == 0
                    && (constructor.getDeclaringClass().getModifiers()
                                & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == constructor.getModifiers();
        }

        for (Annotation[] paramAnnotations : constructor.getParameterAnnotations()) {
            boolean found = false;
            for (Annotation paramAnnotation : paramAnnotations) {
                if (resolverAnnotations.contains(paramAnnotation.annotationType())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }
}
