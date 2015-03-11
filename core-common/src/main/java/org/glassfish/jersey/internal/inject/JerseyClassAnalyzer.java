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
package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Implementation of the {@link ClassAnalyzer} that supports selection
 * of the constructor with largest number of parameters as defined in
 * and required by JAX-RS specification.
 *
 * @author John Wells (john.wells at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
@Named(JerseyClassAnalyzer.NAME)
public final class JerseyClassAnalyzer implements ClassAnalyzer {

    /**
     * Name of the analyzer service.
     */
    public static final String NAME = "JerseyClassAnalyzer";

    /**
     * HK2 binder for the JAX-RS compliant class analyzer implementation.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(JerseyClassAnalyzer.class)
                    .analyzeWith(ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME)
                    .named(JerseyClassAnalyzer.NAME)
                    .to(ClassAnalyzer.class)
                    .in(Singleton.class);
        }
    }

    private final ClassAnalyzer defaultAnalyzer;
    private final Set<Class<? extends Annotation>> resolverAnnotations;

    /**
     * Injection constructor.
     *
     * @param defaultAnalyzer default HK2 class analyzer.
     * @param resolvers       configured injection resolvers.
     */
    @Inject
    JerseyClassAnalyzer(@Named(ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME) final ClassAnalyzer defaultAnalyzer,
                        final IterableProvider<InjectionResolver<?>> resolvers) {
        this.defaultAnalyzer = defaultAnalyzer;

        final HashSet<Class<? extends Annotation>> tmp = new HashSet<Class<? extends Annotation>>();
        for (final InjectionResolver<?> resolver : resolvers) {
            if (resolver.isConstructorParameterIndicator()) {
                final ReflectionHelper.DeclaringClassInterfacePair pair =
                        ReflectionHelper.getClass(resolver.getClass(), InjectionResolver.class);
                final Type paramType = ReflectionHelper.getParameterizedTypeArguments(pair)[0];
                final Class<?> paramClass = ReflectionHelper.erasure(paramType);
                if (Annotation.class.isAssignableFrom(paramClass)) {
                    tmp.add(paramClass.asSubclass(Annotation.class));
                }
            }
        }

        this.resolverAnnotations = (tmp.size() > 0)
                ? Collections.unmodifiableSet(tmp) : Collections.<Class<? extends Annotation>>emptySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Constructor<T> getConstructor(final Class<T> clazz) throws MultiException, NoSuchMethodException {
        if (clazz.isLocalClass()) {
            throw new NoSuchMethodException(LocalizationMessages.INJECTION_ERROR_LOCAL_CLASS_NOT_SUPPORTED(clazz.getName()));
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new NoSuchMethodException(
                    LocalizationMessages.INJECTION_ERROR_NONSTATIC_MEMBER_CLASS_NOT_SUPPORTED(clazz.getName()));
        }

        final Constructor<T> retVal;
        try {
            retVal = defaultAnalyzer.getConstructor(clazz);

            final Class<?>[] args = retVal.getParameterTypes();
            if (args.length != 0) {
                return retVal;
            }

            // Is zero length, but is it specifically marked?
            final Inject i = retVal.getAnnotation(Inject.class);
            if (i != null) {
                return retVal;
            }

            // In this case, the default chose a zero-arg constructor since it could find no other
        } catch (final NoSuchMethodException ignored) {
            // In this case, the default failed because it found no constructor it could use
        } catch (final MultiException me) {
            if (me.getErrors().size() != 1 && !(me.getErrors().get(0) instanceof IllegalArgumentException)) {
                throw me;
            }
            // Otherwise, the default failed because it found more than one constructor
        }

        // At this point, we simply need to find the constructor with the largest number of parameters
        final Constructor<?>[] constructors = AccessController.doPrivileged(new PrivilegedAction<Constructor<?>[]>() {
            @Override
            public Constructor<?>[] run() {
                return clazz.getDeclaredConstructors();
            }
        });
        Constructor<?> selected = null;
        int selectedSize = 0;
        int maxParams = -1;

        for (final Constructor<?> constructor : constructors) {
            final Class<?>[] params = constructor.getParameterTypes();
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
            throw new NoSuchMethodException(LocalizationMessages.INJECTION_ERROR_SUITABLE_CONSTRUCTOR_NOT_FOUND(clazz.getName()));
        }

        if (selectedSize > 1) {
            // Found {0} constructors with {1} parameters in {2} class. Selecting the first found constructor: {3}
            Errors.warning(clazz, LocalizationMessages.MULTIPLE_MATCHING_CONSTRUCTORS_FOUND(
                    selectedSize, maxParams, clazz.getName(), selected.toGenericString()));
        }

        return (Constructor<T>) selected;
    }

    @SuppressWarnings("MagicConstant")
    private boolean isCompatible(final Constructor<?> constructor) {
        if (constructor.getAnnotation(Inject.class) != null) {
            // JSR-330 applicable
            return true;
        }

        final int paramSize = constructor.getParameterTypes().length;

        if (paramSize != 0 && resolverAnnotations.isEmpty()) {
            return false;
        }

        if (!Modifier.isPublic(constructor.getModifiers())) {
            // return true for a default constructor, return false otherwise.
            return paramSize == 0
                    && (constructor.getDeclaringClass().getModifiers()
                                & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == constructor.getModifiers();
        }

        for (final Annotation[] paramAnnotations : constructor.getParameterAnnotations()) {
            boolean found = false;
            for (final Annotation paramAnnotation : paramAnnotations) {
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

    @Override
    public <T> Set<Method> getInitializerMethods(final Class<T> clazz) throws MultiException {
        return defaultAnalyzer.getInitializerMethods(clazz);
    }

    @Override
    public <T> Set<Field> getFields(final Class<T> clazz) throws MultiException {
        return defaultAnalyzer.getFields(clazz);
    }

    @Override
    public <T> Method getPostConstructMethod(final Class<T> clazz) throws MultiException {
        return defaultAnalyzer.getPostConstructMethod(clazz);
    }

    @Override
    public <T> Method getPreDestroyMethod(final Class<T> clazz) throws MultiException {
        return defaultAnalyzer.getPreDestroyMethod(clazz);
    }

}
