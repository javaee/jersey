/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.spi.Errors;

/**
 * Utility class for constructing resource model from JAX-RS annotated POJO.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
final class IntrospectionModeller {

    private static final Logger LOGGER = Logger.getLogger(IntrospectionModeller.class.getName());

    // introspected annotated JAX-RS resource class
    private final Class<?> handlerClass;
    // if not null then introspection modeller is instance based
    private final Object handlerInstance;


    /**
     * Create a new introspection modeller for a given JAX-RS resource class.
     *
     * @param handlerClass JAX-RS resource (handler) class.
     */
    public IntrospectionModeller(Class<?> handlerClass) {
        this.handlerClass = handlerClass;
        this.handlerInstance = null;
    }

    /**
     * Create a new introspection modeller for a given JAX-RS resource instance. The instance will be used for
     * introspection and also created {@link MethodHandler method handlers} will be based on this instance. In other
     * words the final resource will be singleton resource based on the given instance.
     *
     * @param handlerInstance Instance of JAX-RS resource class.
     */
    public IntrospectionModeller(Object handlerInstance) {
        if (handlerInstance instanceof Class<?>) {
            this.handlerClass = (Class) handlerInstance;
            this.handlerInstance = null;
        } else {
            this.handlerInstance = handlerInstance;
            this.handlerClass = handlerInstance.getClass();
        }
    }

    /**
     * Create a new resource model builder for the introspected class.
     *
     * The model returned is filled with the introspected data.
     *
     * @param skipAcceptableCheck if {@code true}, the check if the introspected class
     *                            is a valid, {@link Resource#isAcceptable(Class) acceptable}
     *                            resource class is skipped.
     * @return new resource model builder for the introspected class.
     */
    public Resource.Builder createResourceBuilder(final boolean skipAcceptableCheck) {
        return Errors.processWithException(new Errors.Closure<Resource.Builder>() {
            @Override
            public Resource.Builder invoke() {
                return doCreateResourceBuilder(skipAcceptableCheck);
            }
        });
    }

    private Resource.Builder doCreateResourceBuilder(final boolean skipAcceptableCheck) {
        if (!skipAcceptableCheck && !Resource.isAcceptable(handlerClass)) {
            Errors.fatal(handlerClass, LocalizationMessages.NON_INSTANTIABLE_CLASS(handlerClass));
        }

        checkForNonPublicMethodIssues();

        final Class<?> annotatedResourceClass = getAnnotatedResourceClass(handlerClass);
        final Path rPathAnnotation = annotatedResourceClass.getAnnotation(Path.class);

        final boolean keepEncodedParams =
                (null != annotatedResourceClass.getAnnotation(Encoded.class));

        final List<MediaType> defaultConsumedTypes =
                extractMediaTypes(annotatedResourceClass.getAnnotation(Consumes.class));
        final List<MediaType> defaultProducedTypes =
                extractMediaTypes(annotatedResourceClass.getAnnotation(Produces.class));
        final Collection<Class<? extends Annotation>> defaultNameBindings =
                ReflectionHelper.getAnnotationTypes(annotatedResourceClass, NameBinding.class);

        final MethodList methodList = new MethodList(handlerClass);

        checkResourceClassSetters(methodList, keepEncodedParams);
        checkResourceClassFields(keepEncodedParams, BasicValidator.isSingleton(handlerClass));

        Resource.Builder resourceBuilder;

        if (null != rPathAnnotation) {
            resourceBuilder = Resource.builder(rPathAnnotation.value());
        } else {
            resourceBuilder = Resource.builder();
        }

        resourceBuilder.name(handlerClass.getName());

        addResourceMethods(resourceBuilder, methodList, keepEncodedParams,
                defaultConsumedTypes, defaultProducedTypes, defaultNameBindings);
        addSubResourceMethods(resourceBuilder, methodList, keepEncodedParams,
                defaultConsumedTypes, defaultProducedTypes, defaultNameBindings);
        addSubResourceLocators(resourceBuilder, methodList, keepEncodedParams);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(LocalizationMessages.NEW_AR_CREATED_BY_INTROSPECTION_MODELER(
                    resourceBuilder.toString()));
        }

        return resourceBuilder;
    }

    private void checkForNonPublicMethodIssues() {
        final MethodList allDeclaredMethods = new MethodList(getAllDeclaredMethods(handlerClass));

        // non-public resource methods
        for (AnnotatedMethod m : allDeclaredMethods.withMetaAnnotation(HttpMethod.class).
                withoutAnnotation(Path.class).isNotPublic()) {
            Errors.warning(handlerClass, LocalizationMessages.NON_PUB_RES_METHOD(m.getMethod().toGenericString()));
        }
        // non-public subres methods
        for (AnnotatedMethod m : allDeclaredMethods.withMetaAnnotation(HttpMethod.class).
                withAnnotation(Path.class).isNotPublic()) {
            Errors.warning(handlerClass, LocalizationMessages.NON_PUB_SUB_RES_METHOD(m.getMethod().toGenericString()));
        }
        // non-public subres locators
        for (AnnotatedMethod m : allDeclaredMethods.withoutMetaAnnotation(HttpMethod.class).
                withAnnotation(Path.class).isNotPublic()) {
            Errors.warning(handlerClass, LocalizationMessages.NON_PUB_SUB_RES_LOC(m.getMethod().toGenericString()));
        }
    }

    private void checkResourceClassSetters(final MethodList methodList, final boolean encodedFlag) {
        for (AnnotatedMethod method : methodList.withoutMetaAnnotation(HttpMethod.class).
                withoutAnnotation(Path.class).
                hasNumParams(1).
                hasReturnType(void.class).
                nameStartsWith("set")) {
            Parameter p = Parameter.create(
                    handlerClass,
                    method.getMethod().getDeclaringClass(),
                    encodedFlag || method.isAnnotationPresent(Encoded.class),
                    method.getParameterTypes()[0],
                    method.getGenericParameterTypes()[0],
                    method.getAnnotations());
            if (null != p) {
                BasicValidator.validateParameter(p, method.getMethod(), method.getMethod().toGenericString(), "1",
                        BasicValidator.isSingleton(handlerClass));
            }
        }
    }

    private void checkResourceClassFields(final boolean encodedFlag, boolean isInSingleton) {
        for (Field field : handlerClass.getDeclaredFields()) {
            if (field.getDeclaredAnnotations().length > 0) {
                Parameter p = Parameter.create(
                        handlerClass,
                        field.getDeclaringClass(),
                        encodedFlag || field.isAnnotationPresent(Encoded.class),
                        field.getType(),
                        field.getGenericType(),
                        field.getAnnotations());
                if (null != p) {
                    BasicValidator.validateParameter(p, field, field.toGenericString(), field.getName(),
                            isInSingleton);
                }
            }
        }
    }


    private List<Method> getAllDeclaredMethods(final Class<?> clazz) {
        final List<Method> result = new LinkedList<Method>();

        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            @Override
            public Object run() {
                Class current = clazz;
                while (current != Object.class && current != null) {
                    result.addAll(Arrays.asList(current.getDeclaredMethods()));
                    current = current.getSuperclass();
                }
                return null;
            }
        });

        return result;
    }

    /**
     * Get the class in the provided resource class ancestor hierarchy that
     * is actually annotated with the {@link Path &#64;Path} annotation.
     *
     * @param resourceClass resource class.
     * @return resource class or it's ancestor that is annotated with the {@link Path &#64;Path}
     *         annotation.
     */
    static Class<?> getAnnotatedResourceClass(Class<?> resourceClass) {
        if (resourceClass.isAnnotationPresent(Path.class)) {
            return resourceClass;
        }

        for (Class<?> i : resourceClass.getInterfaces()) {
            if (i.isAnnotationPresent(Path.class)) {
                return i;
            }
        }

        return resourceClass;
    }

    private static List<MediaType> resolveConsumedTypes(final AnnotatedMethod am, final List<MediaType> defaultConsumedTypes) {
        // Override default types if the annotation is present on the method
        if (am.isAnnotationPresent(Consumes.class)) {
            return extractMediaTypes(am.getAnnotation(Consumes.class));
        }

        return defaultConsumedTypes;
    }

    private static List<MediaType> resolveProducedTypes(final AnnotatedMethod am, final List<MediaType> defaultProducedTypes) {
        // Override default types if the annotation is present on the method
        if (am.isAnnotationPresent(Produces.class)) {
            return extractMediaTypes(am.getAnnotation(Produces.class));
        }

        return defaultProducedTypes;
    }

    private static List<MediaType> extractMediaTypes(final Consumes annotation) {
        return (annotation != null) ? extractMediaTypes(annotation.value()) : Collections.<MediaType>emptyList();
    }

    private static List<MediaType> extractMediaTypes(final Produces annotation) {
        return (annotation != null) ? extractMediaTypes(annotation.value()) : Collections.<MediaType>emptyList();
    }

    private static List<MediaType> extractMediaTypes(final String[] values) {
        if (values.length == 0) {
            return Collections.emptyList();
        }

        final List<MediaType> types = new ArrayList<MediaType>(values.length);
        for (final String mtEntry : values) {
            for (final String mt : Tokenizer.tokenize(mtEntry, ",")) {
                types.add(MediaType.valueOf(mt));
            }
        }

        return types;
    }

    private static void introspectAsyncFeatures(AnnotatedMethod am, ResourceMethod.Builder resourceMethodBuilder) {
        if (am.isAnnotationPresent(ManagedAsync.class)) {
            resourceMethodBuilder.managedAsync();
        }

        for (Annotation[] annotations : am.getParameterAnnotations()) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == Suspended.class) {
                    resourceMethodBuilder.suspended(AsyncResponse.NO_TIMEOUT, TimeUnit.MILLISECONDS);
                    return;
                }
            }
        }
    }

    private void addResourceMethods(
            final Resource.Builder resourceBuilder,
            final MethodList methodList,
            final boolean encodedParameters,
            final List<MediaType> defaultConsumedTypes,
            final List<MediaType> defaultProducedTypes,
            final Collection<Class<? extends Annotation>> defaultNameBindings
    ) {
        for (AnnotatedMethod am : methodList.withMetaAnnotation(HttpMethod.class).withoutAnnotation(Path.class)) {
            ResourceMethod.Builder methodBuilder =
                    resourceBuilder.addMethod(am.getMetaMethodAnnotations(HttpMethod.class).get(0).value())
                            .consumes(resolveConsumedTypes(am, defaultConsumedTypes))
                            .produces(resolveProducedTypes(am, defaultProducedTypes))
                            .encodedParameters(encodedParameters || am.isAnnotationPresent(Encoded.class))
                            .nameBindings(defaultNameBindings)
                            .nameBindings(am.getAnnotations());
            if (handlerInstance != null) {
                methodBuilder.handledBy(handlerInstance, am.getMethod());
            } else {
                methodBuilder.handledBy(handlerClass, am.getMethod());
            }

            introspectAsyncFeatures(am, methodBuilder);
        }
    }

    private void addSubResourceMethods(
            final Resource.Builder resourceBuilder,
            final MethodList methodList,
            final boolean encodedParameters,
            final List<MediaType> defaultConsumedTypes,
            final List<MediaType> defaultProducedTypes,
            final Collection<Class<? extends Annotation>> defaultNameBindings
    ) {
        for (AnnotatedMethod am : methodList.withMetaAnnotation(HttpMethod.class).withAnnotation(Path.class)) {
            ResourceMethod.Builder methodBuilder =
                    resourceBuilder.addMethod(am.getMetaMethodAnnotations(HttpMethod.class).get(0).value())
                            .path(am.getAnnotation(Path.class).value())
                            .consumes(resolveConsumedTypes(am, defaultConsumedTypes))
                            .produces(resolveProducedTypes(am, defaultProducedTypes))
                            .encodedParameters(encodedParameters || am.isAnnotationPresent(Encoded.class))
                            .nameBindings(defaultNameBindings)
                            .nameBindings(am.getAnnotations());
            if (handlerInstance != null) {
                methodBuilder.handledBy(handlerInstance, am.getMethod());
            } else {
                methodBuilder.handledBy(handlerClass, am.getMethod());
            }
            introspectAsyncFeatures(am, methodBuilder);
        }
    }

    private void addSubResourceLocators(
            Resource.Builder resourceBuilder,
            MethodList methodList,
            boolean encodedParameters) {

        for (AnnotatedMethod am : methodList.withoutMetaAnnotation(HttpMethod.class).withAnnotation(Path.class)) {
            resourceBuilder.addMethod().path(am.getAnnotation(Path.class).value())
                    .encodedParameters(encodedParameters || am.isAnnotationPresent(Encoded.class))
                    .handledBy(handlerClass, am.getMethod());
        }
    }
}
