/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.internal.ModelHelper;

/**
 * Utility class for constructing resource model from JAX-RS annotated POJO.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
final class IntrospectionModeller {

    private static final Logger LOGGER = Logger.getLogger(IntrospectionModeller.class.getName());

    // introspected annotated JAX-RS resource class
    private final Class<?> handlerClass;
    // validation flag
    private final boolean disableValidation;

    /**
     * Create a new introspection modeller for a given JAX-RS resource class.
     *
     * @param handlerClass      JAX-RS resource (handler) class.
     * @param disableValidation if set to {@code true}, then any model validation checks will be disabled.
     */
    public IntrospectionModeller(Class<?> handlerClass, boolean disableValidation) {
        this.handlerClass = handlerClass;
        this.disableValidation = disableValidation;
    }

    /**
     * Create a new resource model builder for the introspected class.
     * <p>
     * The model returned is filled with the introspected data.
     * </p>
     *
     * @return new resource model builder for the introspected class.
     */
    public Resource.Builder createResourceBuilder() {
        return Errors.processWithException(new Producer<Resource.Builder>() {
            @Override
            public Resource.Builder call() {
                return doCreateResourceBuilder();
            }
        });
    }

    private Resource.Builder doCreateResourceBuilder() {
        if (!disableValidation) {
            checkForNonPublicMethodIssues();
        }

        final Class<?> annotatedResourceClass = ModelHelper.getAnnotatedResourceClass(handlerClass);
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

        final List<Parameter> resourceClassParameters = new LinkedList<>();
        checkResourceClassSetters(methodList, keepEncodedParams, resourceClassParameters);
        checkResourceClassFields(keepEncodedParams, InvocableValidator.isSingleton(handlerClass), resourceClassParameters);

        Resource.Builder resourceBuilder;

        if (null != rPathAnnotation) {
            resourceBuilder = Resource.builder(rPathAnnotation.value());
        } else {
            resourceBuilder = Resource.builder();
        }

        boolean extended = false;
        if (handlerClass.isAnnotationPresent(ExtendedResource.class)) {
            resourceBuilder.extended(true);
            extended = true;
        }

        resourceBuilder.name(handlerClass.getName());

        addResourceMethods(resourceBuilder, methodList, resourceClassParameters, keepEncodedParams,
                defaultConsumedTypes, defaultProducedTypes, defaultNameBindings, extended);
        addSubResourceMethods(resourceBuilder, methodList, resourceClassParameters, keepEncodedParams,
                defaultConsumedTypes, defaultProducedTypes, defaultNameBindings, extended);
        addSubResourceLocators(resourceBuilder, methodList, resourceClassParameters, keepEncodedParams, extended);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(LocalizationMessages.NEW_AR_CREATED_BY_INTROSPECTION_MODELER(
                    resourceBuilder.toString()));
        }

        return resourceBuilder;
    }

    private void checkForNonPublicMethodIssues() {
        final MethodList allDeclaredMethods = new MethodList(getAllDeclaredMethods(handlerClass));

        // non-public resource methods
        for (AnnotatedMethod m : allDeclaredMethods.withMetaAnnotation(HttpMethod.class)
                .withoutAnnotation(Path.class).isNotPublic()) {
            Errors.warning(handlerClass, LocalizationMessages.NON_PUB_RES_METHOD(m.getMethod().toGenericString()));
        }
        // non-public sub-resource methods
        for (AnnotatedMethod m : allDeclaredMethods.withMetaAnnotation(HttpMethod.class)
                .withAnnotation(Path.class).isNotPublic()) {
            Errors.warning(handlerClass, LocalizationMessages.NON_PUB_SUB_RES_METHOD(m.getMethod().toGenericString()));
        }
        // non-public sub-resource locators
        for (AnnotatedMethod m : allDeclaredMethods.withoutMetaAnnotation(HttpMethod.class)
                .withAnnotation(Path.class).isNotPublic()) {
            Errors.warning(handlerClass, LocalizationMessages.NON_PUB_SUB_RES_LOC(m.getMethod().toGenericString()));
        }
    }

    private void checkResourceClassSetters(final MethodList methodList,
                                           final boolean encodedFlag,
                                           Collection<Parameter> injectableParameters) {

        for (AnnotatedMethod method : methodList.withoutMetaAnnotation(HttpMethod.class)
                .withoutAnnotation(Path.class)
                .hasNumParams(1)
                .hasReturnType(void.class)
                .nameStartsWith("set")) {
            Parameter p = Parameter.create(
                    handlerClass,
                    method.getMethod().getDeclaringClass(),
                    encodedFlag || method.isAnnotationPresent(Encoded.class),
                    method.getParameterTypes()[0],
                    method.getGenericParameterTypes()[0],
                    method.getAnnotations());
            if (null != p) {
                ResourceMethodValidator.validateParameter(p, method.getMethod(), method.getMethod().toGenericString(), "1",
                        InvocableValidator.isSingleton(handlerClass));

                // we do not inject entity parameters into class instance fields and properties.
                if (p.getSource() != Parameter.Source.ENTITY) {
                    injectableParameters.add(p);
                }
            }
        }
    }

    private void checkResourceClassFields(final boolean encodedFlag,
                                          boolean isInSingleton,
                                          Collection<Parameter> injectableParameters) {

        for (Field field : AccessController.doPrivileged(ReflectionHelper.getDeclaredFieldsPA(handlerClass))) {
            if (field.getDeclaredAnnotations().length > 0) {
                Parameter p = Parameter.create(
                        handlerClass,
                        field.getDeclaringClass(),
                        encodedFlag || field.isAnnotationPresent(Encoded.class),
                        field.getType(),
                        field.getGenericType(),
                        field.getAnnotations());
                if (null != p) {
                    ResourceMethodValidator.validateParameter(p, field, field.toGenericString(), field.getName(),
                            isInSingleton);
                    // we do not inject entity and unknown parameters into class instance fields and properties.
                    if (p.getSource() != Parameter.Source.ENTITY) {
                        injectableParameters.add(p);
                    }
                }
            }
        }
    }

    private List<Method> getAllDeclaredMethods(final Class<?> clazz) {
        final List<Method> result = new LinkedList<>();

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

        final List<MediaType> types = new ArrayList<>(values.length);
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
            final List<Parameter> resourceClassParameters, // parameters derived from fields and setters on the resource class
            final boolean encodedParameters,
            final List<MediaType> defaultConsumedTypes,
            final List<MediaType> defaultProducedTypes,
            final Collection<Class<? extends Annotation>> defaultNameBindings,
            final boolean extended
    ) {
        for (AnnotatedMethod am : methodList.withMetaAnnotation(HttpMethod.class).withoutAnnotation(Path.class)) {
            ResourceMethod.Builder methodBuilder =
                    resourceBuilder.addMethod(am.getMetaMethodAnnotations(HttpMethod.class).get(0).value())
                            .consumes(resolveConsumedTypes(am, defaultConsumedTypes))
                            .produces(resolveProducedTypes(am, defaultProducedTypes))
                            .encodedParameters(encodedParameters || am.isAnnotationPresent(Encoded.class))
                            .nameBindings(defaultNameBindings)
                            .nameBindings(am.getAnnotations())
                            .handledBy(handlerClass, am.getMethod())
                            .handlingMethod(am.getDeclaredMethod())
                            .handlerParameters(resourceClassParameters)
                            .extended(extended || am.isAnnotationPresent(ExtendedResource.class));

            introspectAsyncFeatures(am, methodBuilder);
        }
    }

    private void addSubResourceMethods(
            final Resource.Builder resourceBuilder,
            final MethodList methodList,
            final List<Parameter> resourceClassParameters, // parameters derived from fields and setters on the resource class
            final boolean encodedParameters,
            final List<MediaType> defaultConsumedTypes,
            final List<MediaType> defaultProducedTypes,
            final Collection<Class<? extends Annotation>> defaultNameBindings,
            final boolean extended
    ) {

        for (AnnotatedMethod am : methodList.withMetaAnnotation(HttpMethod.class).withAnnotation(Path.class)) {
            Resource.Builder childResourceBuilder = resourceBuilder.addChildResource(am.getAnnotation(Path.class).value());

            ResourceMethod.Builder methodBuilder =
                    childResourceBuilder.addMethod(am.getMetaMethodAnnotations(HttpMethod.class).get(0).value())
                            .consumes(resolveConsumedTypes(am, defaultConsumedTypes))
                            .produces(resolveProducedTypes(am, defaultProducedTypes))
                            .encodedParameters(encodedParameters || am.isAnnotationPresent(Encoded.class))
                            .nameBindings(defaultNameBindings)
                            .nameBindings(am.getAnnotations())
                            .handledBy(handlerClass, am.getMethod())
                            .handlingMethod(am.getDeclaredMethod())
                            .handlerParameters(resourceClassParameters)
                            .extended(extended || am.isAnnotationPresent(ExtendedResource.class));

            introspectAsyncFeatures(am, methodBuilder);
        }
    }

    private void addSubResourceLocators(
            final Resource.Builder resourceBuilder,
            final MethodList methodList,
            final List<Parameter> resourceClassParameters, // parameters derived from fields and setters on the resource class
            final boolean encodedParameters,
            final boolean extended) {

        for (AnnotatedMethod am : methodList.withoutMetaAnnotation(HttpMethod.class).withAnnotation(Path.class)) {
            final String path = am.getAnnotation(Path.class).value();
            Resource.Builder builder = resourceBuilder;
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                builder = resourceBuilder.addChildResource(path);
            }

            builder.addMethod()
                    .encodedParameters(encodedParameters || am.isAnnotationPresent(Encoded.class))
                    .handledBy(handlerClass, am.getMethod())
                    .handlingMethod(am.getDeclaredMethod())
                    .handlerParameters(resourceClassParameters)
                    .extended(extended || am.isAnnotationPresent(ExtendedResource.class));
        }
    }
}
