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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.spi.Errors;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Performs a basic check on abstract resources.
 * <p/>
 * Validity check populates a list of potential issues with the given resource.
 * The issues are divided into two categories: fatal and non-fatal issues. The
 * former type prevents the resource to be deployed and makes the whole web
 * application deployment fail.
 * <p/>
 * To check a single resource class, one could use one of the {@link Resource}
 * {@code builder(...)} methods to get a resource model.
 *
 * {@link ResourceModelValidator#validate(ResourceModelComponent)}
 * method then populates the issue list, which could be then obtained by the
 * {@link ResourceModelValidator#getIssueList()}. Unless you explicitly clear
 * the list, subsequent calls to the validate method will add new items to the list,
 * so that you can build the issue list for more than one resource. To clear the
 * list, you may want to call {@link ResourceModelValidator#cleanIssueList()} method.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class BasicValidator extends ResourceModelValidator {
    private static final Set<Class<?>> SCOPE_ANNOTATIONS = getScopeAnnotations();
    private final ServiceLocator locator;

    private static Set<Class<?>> getScopeAnnotations() {
        Set<Class<?>> scopeAnnotations = new HashSet<Class<?>>();
        scopeAnnotations.add(Singleton.class);
        scopeAnnotations.add(PerLookup.class);
        return scopeAnnotations;
    }

    private final MessageBodyWorkers workers;
    /**
     * Classes that have been checked already.
     */
    protected final Set<Class<?>> checkedClasses = new HashSet<Class<?>>();

    /**
     * Construct a new basic validator.
     *
     * @param locator HK2 service locator.
     */
    public BasicValidator(ServiceLocator locator) {
        this.workers = locator.getService(MessageBodyWorkers.class);
        this.locator = locator;
    }

    @Override
    public void visitResourceClass(final Resource resource) {
        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                // uri template of the resource, if present should not contain a null value
                if (resource.isRootResource() && (null == resource.getPath())) {
                    // TODO: is it really a fatal issue?
                    Errors.fatal(resource, LocalizationMessages.RES_URI_PATH_INVALID(resource.getName(), resource.getPath()));
                }

                checkConsumesProducesAmbiguities(resource);
                checkSRLAmbiguities(resource);

                return null;
            }
        });
    }


    @Override
    public void visitResourceHandlerConstructor(final HandlerConstructor constructor) {
        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                Class<?> resClass = constructor.getConstructor().getDeclaringClass();
                boolean isSingleton = isSingleton(resClass);
                int paramCount = 0;
                for (Parameter p : constructor.getParameters()) {
                    validateParameter(p, constructor.getConstructor(), constructor.getConstructor().toGenericString(),
                            Integer.toString(++paramCount), isSingleton);
                }

                return null;
            }
        });
    }

    /**
     * Check if the resource class is declared to be a singleton.
     *
     * @param resourceClass resource class.
     * @return {@code true} if the resource class is a singleton, {@code false} otherwise.
     */
    public static boolean isSingleton(Class<?> resourceClass) {
        return resourceClass.isAnnotationPresent(Singleton.class)
                || (Providers.isProvider(resourceClass) && !resourceClass.isAnnotationPresent(PerLookup.class));
    }

    @Override
    public void visitInvocable(final Invocable invocable) {
        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                // TODO: check invocable.
                Class resClass = invocable.getHandler().getHandlerClass();
                if (resClass != null && !checkedClasses.contains(resClass)) {
                    checkedClasses.add(resClass);
                    final boolean provider = Providers.isProvider(resClass);
                    int counter = 0;
                    for (Annotation annotation : resClass.getAnnotations()) {
                        if (SCOPE_ANNOTATIONS.contains(annotation.annotationType())) {
                            counter++;
                        }
                    }
                    if (counter == 0 && provider) {
                        Errors.warning(resClass, LocalizationMessages.RESOURCE_IMPLEMENTS_PROVIDER(resClass,
                                Providers.getProviderContracts(resClass)));
                    } else if (counter > 1) {
                        Errors.fatal(resClass, LocalizationMessages.RESOURCE_MULTIPLE_SCOPE_ANNOTATIONS(resClass));
                    }
                }

                return null;
            }
        });
    }

    @Override
    public void visitMethodHandler(MethodHandler methodHandler) {
        // TODO: check method handler.
    }

    @Override
    public void visitResourceMethod(final ResourceMethod method) {
        Errors.processWithException(new Errors.Closure<Object>() {
            @Override
            public Object invoke() {
                switch (method.getType()) {
                    case RESOURCE_METHOD:
                        visitJaxrsResourceMethod(method);
                        break;
                    case SUB_RESOURCE_METHOD:
                        visitSubResourceMethod(method);
                        break;
                    case SUB_RESOURCE_LOCATOR:
                        visitSubResourceLocator(method);
                        break;
                }

                return null;
            }
        });
    }

    private void visitJaxrsResourceMethod(ResourceMethod method) {
        checkMethod(method);
    }

    private void checkMethod(ResourceMethod method) {
        checkValueProviders(method);
        final Invocable invocable = method.getInvocable();

        checkParameters(method);

        if ("GET".equals(method.getHttpMethod())) {
            // ensure GET returns non-void value if not suspendable
            if (void.class == invocable.getHandlingMethod().getReturnType() && !method.isSuspendDeclared()) {
                Errors.warning(method, LocalizationMessages.GET_RETURNS_VOID(invocable.getHandlingMethod()));
            }

            // ensure GET does not consume an entity parameter, if not inflector-based
            if (invocable.requiresEntity() && !invocable.isInflector()) {
                Errors.warning(method, LocalizationMessages.GET_CONSUMES_ENTITY(invocable.getHandlingMethod()));
            }
            // ensure GET does not consume any @FormParam annotated parameter
            for (Parameter p : invocable.getParameters()) {
                if (p.isAnnotationPresent(FormParam.class)) {
                    Errors.fatal(method, LocalizationMessages.GET_CONSUMES_FORM_PARAM(invocable.getHandlingMethod()));
                    break;
                }
            }
        }

        // ensure there is not multiple HTTP method designators specified on the method
        List<String> httpMethodAnnotations = new LinkedList<String>();
        for (Annotation a : invocable.getHandlingMethod().getDeclaredAnnotations()) {
            if (null != a.annotationType().getAnnotation(HttpMethod.class)) {
                httpMethodAnnotations.add(a.toString());
            } else if ((a.annotationType() == Path.class) && method.getType() == ResourceMethod.JaxrsType.RESOURCE_METHOD) {
                Errors.warning(method, LocalizationMessages.SUB_RES_METHOD_TREATED_AS_RES_METHOD(invocable.getHandlingMethod(),
                        ((Path) a).value()));
            }
        }

        if (httpMethodAnnotations.size() > 1) {
            Errors.fatal(method, LocalizationMessages.MULTIPLE_HTTP_METHOD_DESIGNATORS(invocable.getHandlingMethod(),
                    httpMethodAnnotations.toString()));
        }

        final Type responseType = invocable.getResponseType();
        if (!isConcreteType(responseType)) {
            Errors.warning(invocable.getHandlingMethod(), LocalizationMessages.TYPE_OF_METHOD_NOT_RESOLVABLE_TO_CONCRETE_TYPE
                    (responseType, invocable.getHandlingMethod().toGenericString()));
        }
    }

    private void visitSubResourceMethod(ResourceMethod method) {
        // check the same things that are being checked for resource methods
        checkMethod(method);

        // and make sure the Path is not null
        if ((null == method.getPath()) || (null == method.getPath()) || (method.getPath().length() == 0)) {
            Errors.fatal(method, LocalizationMessages.SUBRES_METHOD_URI_PATH_INVALID(method.getInvocable().getHandlingMethod(),
                    method.getPath()));
        }
    }

    private void checkValueProviders(ResourceMethod method) {
        final List<Factory<?>> valueProviders = method.getInvocable().getValueProviders(locator);
        if (valueProviders.contains(null)) {
            int index = valueProviders.indexOf(null);
            Errors.fatal(method, LocalizationMessages.ERROR_PARAMETER_MISSING_VALUE_PROVIDER(index, method.getInvocable()
                    .getHandlingMethod()));
        }
    }

    private void visitSubResourceLocator(ResourceMethod locator) {
        checkParameters(locator);
        checkValueProviders(locator);

        final Invocable invocable = locator.getInvocable();
        if (void.class == invocable.getRawResponseType()) {
            Errors.fatal(locator, LocalizationMessages.SUBRES_LOC_RETURNS_VOID(invocable.getHandlingMethod()));
        }
        if ((null == locator.getPath()) || (null == locator.getPath()) || (locator.getPath().length() == 0)) {
            Errors.fatal(locator, LocalizationMessages.SUBRES_LOC_URI_PATH_INVALID(invocable.getHandlingMethod(),
                    locator.getPath()));
        }
    }

    private static final Set<Class> PARAM_ANNOTATION_SET = createParamAnnotationSet();

    private static Set<Class> createParamAnnotationSet() {
        Set<Class> set = new HashSet<Class>(6);
        set.add(HeaderParam.class);
        set.add(CookieParam.class);
        set.add(MatrixParam.class);
        set.add(QueryParam.class);
        set.add(PathParam.class);
        set.add(BeanParam.class);
        return Collections.unmodifiableSet(set);
    }

    /**
     * Validate a single parameter instance.
     *
     * @param parameter             parameter to be validated.
     * @param source                parameter source; used for issue reporting.
     * @param reportedSourceName    source name; used for issue reporting.
     * @param reportedParameterName parameter name; used for issue reporting.
     * @param injectionsForbidden   true if parameters cannot be injected by
     *                              parameter annotations, eg. {@link HeaderParam @HeaderParam}.
     */
    static void validateParameter(final Parameter parameter,
                                  final Object source,
                                  final String reportedSourceName,
                                  final String reportedParameterName, final boolean injectionsForbidden) {
        Errors.processWithException(new Errors.Closure<Void>() {
            @Override
            public Void invoke() {
                int counter = 0;
                final Annotation[] annotations = parameter.getAnnotations();
                for (Annotation a : annotations) {
                    if (PARAM_ANNOTATION_SET.contains(a.annotationType())) {
                        if (injectionsForbidden) {
                            Errors.fatal(source, LocalizationMessages.SINGLETON_INJECTS_PARAMETER(reportedSourceName,
                                    reportedParameterName));
                            break;
                        }
                        counter++;
                        if (counter > 1) {
                            Errors.warning(source, LocalizationMessages.AMBIGUOUS_PARAMETER(reportedSourceName,
                                    reportedParameterName));
                            break;
                        }
                    }
                }

                final Type paramType = parameter.getType();
                if (!isConcreteType(paramType)) {
                    Errors.warning(source, LocalizationMessages.PARAMETER_UNRESOLVABLE(reportedParameterName, paramType,
                            reportedSourceName));
                }

                return null;
            }
        });
    }

    private static boolean isConcreteType(Type t) {
        if (t instanceof ParameterizedType) {
            return isConcreteParameterizedType((ParameterizedType) t);
        } else if (!(t instanceof Class)) {
            // GenericArrayType, WildcardType, TypeVariable
            return false;
        }

        return true;
    }

    private static boolean isConcreteParameterizedType(ParameterizedType pt) {
        boolean isConcrete = true;
        for (Type t : pt.getActualTypeArguments()) {
            isConcrete &= isConcreteType(t);
        }

        return isConcrete;
    }

    private void checkParameters(ResourceMethod method) {
        final Invocable invocable = method.getInvocable();
        final Method handlingMethod = invocable.getHandlingMethod();
        int paramCount = 0;
        int nonAnnotetedParameters = 0;

        for (Parameter p : invocable.getParameters()) {
            validateParameter(p, handlingMethod, handlingMethod.toGenericString(), Integer.toString(++paramCount), false);
            if (method.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR
                    && Parameter.Source.ENTITY == p.getSource()) {
                Errors.fatal(method, LocalizationMessages.SUBRES_LOC_HAS_ENTITY_PARAM(invocable.getHandlingMethod()));
            } else if (p.getAnnotations().length == 0) {
                nonAnnotetedParameters++;
                if (nonAnnotetedParameters > 1) {
                    Errors.fatal(method, LocalizationMessages.AMBIGUOUS_NON_ANNOTATED_PARAMETER(invocable.getHandlingMethod(),
                            invocable.getHandlingMethod().getDeclaringClass()));
                }
            }
        }
    }

    private void checkConsumesProducesAmbiguities(Resource resource) {

        final List<ResourceMethod> resourceMethods = resource.getResourceMethods();

        if (resourceMethods.size() >= 2) {
            for (ResourceMethod m1 : resourceMethods.subList(0, resourceMethods.size() - 1)) {
                for (ResourceMethod m2 : resourceMethods.subList(resourceMethods.indexOf(m1) + 1, resourceMethods.size())) {
                    if (sameHttpMethod(m1, m2)) {
                        checkIntersectingMediaTypes(resource, m1.getHttpMethod(), m1, m2);
                    }
                }
            }
        }

        final List<ResourceMethod> subResourceMethods = resource.getSubResourceMethods();

        if (subResourceMethods.size() >= 2) {
            for (ResourceMethod m1 : subResourceMethods.subList(0, subResourceMethods.size() - 1)) {
                for (ResourceMethod m2 :
                        subResourceMethods.subList(subResourceMethods.indexOf(m1) + 1, subResourceMethods.size())) {
                    if (samePath(m1, m2) && sameHttpMethod(m1, m2)) {
                        checkIntersectingMediaTypes(resource, m1.getHttpMethod(), m1, m2);
                    }
                }
            }
        }
    }

    private void checkSRLAmbiguities(Resource resource) {
        final List<ResourceMethod> subResourceLocators = resource.getSubResourceLocators();

        if (subResourceLocators.size() >= 2) {
            for (ResourceMethod m1 : subResourceLocators.subList(0, subResourceLocators.size() - 1)) {
                for (ResourceMethod m2 :
                        subResourceLocators.subList(subResourceLocators.indexOf(m1) + 1, subResourceLocators.size())) {
                    if (samePath(m1, m2)) {
                        Errors.fatal(resource, LocalizationMessages.AMBIGUOUS_SRLS(
                                resource.getName(),
                                m1.getPath(),
                                m2.getPath()));
                    }
                }
            }
        }
    }

    private void checkIntersectingMediaTypes(
            Resource resource,
            String httpMethod,
            ResourceMethod m1,
            ResourceMethod m2) {

        final List<MediaType> inputTypes1 = getEffectiveInputTypes(m1);
        final List<MediaType> inputTypes2 = getEffectiveInputTypes(m2);
        final List<MediaType> outputTypes1 = getEffectiveOutputTypes(m1);
        final List<MediaType> outputTypes2 = getEffectiveOutputTypes(m2);

        boolean consumesFails;
        boolean consumesOnlyIntersects = false;
        if (m1.getConsumedTypes().isEmpty() || m2.getConsumedTypes().isEmpty()) {
            consumesFails = inputTypes1.equals(inputTypes2);
            if (!consumesFails) {
                consumesOnlyIntersects = MediaTypes.intersect(inputTypes1, inputTypes2);
            }
        } else {
            consumesFails = MediaTypes.intersect(inputTypes1, inputTypes2);
        }

        boolean producesFails;
        boolean producesOnlyIntersects = false;
        if (m1.getProducedTypes().isEmpty() || m2.getProducedTypes().isEmpty()) {
            producesFails = outputTypes1.equals(outputTypes2);
            if (!producesFails) {
                producesOnlyIntersects = MediaTypes.intersect(outputTypes1, outputTypes2);
            }
        } else {
            producesFails = MediaTypes.intersect(outputTypes1, outputTypes2);
        }

        if (consumesFails && producesFails) {
            // fatal
            final String rcName = resource.getName();
            Errors.fatal(resource, LocalizationMessages.AMBIGUOUS_FATAL_RMS(rcName, httpMethod, m1.getInvocable()
                    .getHandlingMethod(), m2.getInvocable().getHandlingMethod()));
        } else if ((producesFails && consumesOnlyIntersects) || (consumesFails && producesOnlyIntersects) ||
                (consumesOnlyIntersects && producesOnlyIntersects)) {
            // warning
            final String rcName = resource.getName();
            if (m1.getInvocable().requiresEntity()) {
                Errors.warning(resource, LocalizationMessages.AMBIGUOUS_RMS_IN(
                        rcName, httpMethod, m1.getInvocable().getHandlingMethod(), m2.getInvocable().getHandlingMethod()));
            } else {
                Errors.warning(resource, LocalizationMessages.AMBIGUOUS_RMS_OUT(
                        rcName, httpMethod, m1.getInvocable().getHandlingMethod(), m2.getInvocable().getHandlingMethod()));
            }
        }

    }

    private static final List<MediaType> StarTypeList = Arrays.asList(new MediaType("*", "*"));

    private List<MediaType> getEffectiveInputTypes(final ResourceMethod resourceMethod) {
        if (!resourceMethod.getConsumedTypes().isEmpty()) {
            return resourceMethod.getConsumedTypes();
        }
        List<MediaType> result = new LinkedList<MediaType>();
        if (workers != null) {
            for (Parameter p : resourceMethod.getInvocable().getParameters()) {
                if (p.getSource() == Parameter.Source.ENTITY) {
                    result.addAll(workers.getMessageBodyReaderMediaTypes(
                            p.getRawType(), p.getType(), p.getDeclaredAnnotations()));
                }
            }
        }
        return result.isEmpty() ? StarTypeList : result;
    }

    private List<MediaType> getEffectiveOutputTypes(final ResourceMethod resourceMethod) {
        if (!resourceMethod.getProducedTypes().isEmpty()) {
            return resourceMethod.getProducedTypes();
        }
        List<MediaType> result = new LinkedList<MediaType>();
        if (workers != null) {
            final Invocable invocable = resourceMethod.getInvocable();
            result.addAll(workers.getMessageBodyWriterMediaTypes(
                    invocable.getRawResponseType(),
                    invocable.getResponseType(),
                    invocable.getHandlingMethod().getDeclaredAnnotations()));
        }
        return result.isEmpty() ? StarTypeList : result;
    }

    private boolean sameHttpMethod(ResourceMethod m1, ResourceMethod m2) {
        return m1.getHttpMethod().equals(m2.getHttpMethod());
    }

    private boolean samePath(Routed m1, Routed m2) {
        return m1.getPathPattern().equals(m2.getPathPattern());
    }

}
