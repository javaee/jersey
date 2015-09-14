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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.server.internal.LocalizationMessages;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Validator checking resource methods and sub resource locators. The validator mainly checks the parameters of resource
 * methods and sub resource locators.
 *
 * @author Miroslav Fuksa
 */
class ResourceMethodValidator extends AbstractResourceModelVisitor {

    private final ServiceLocator locator;

    /**
     * Create new resource method validator.
     *
     * @param locator HK2 service locator.
     */
    public ResourceMethodValidator(ServiceLocator locator) {
        this.locator = locator;
    }

    @Override
    public void visitResourceMethod(final ResourceMethod method) {
        switch (method.getType()) {
            case RESOURCE_METHOD:
                visitJaxrsResourceMethod(method);
                break;
            case SUB_RESOURCE_LOCATOR:
                visitSubResourceLocator(method);
                break;
        }

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
                Errors.hint(method, LocalizationMessages.GET_RETURNS_VOID(invocable.getHandlingMethod()));
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
        List<String> httpMethodAnnotations = new LinkedList<>();
        for (Annotation a : invocable.getHandlingMethod().getDeclaredAnnotations()) {
            if (null != a.annotationType().getAnnotation(HttpMethod.class)) {
                httpMethodAnnotations.add(a.toString());
            }
        }

        if (httpMethodAnnotations.size() > 1) {
            Errors.fatal(method, LocalizationMessages.MULTIPLE_HTTP_METHOD_DESIGNATORS(invocable.getHandlingMethod(),
                    httpMethodAnnotations.toString()));
        }

        final Type responseType = invocable.getResponseType();
        if (!isConcreteType(responseType)) {
            Errors.warning(invocable.getHandlingMethod(),
                    LocalizationMessages.TYPE_OF_METHOD_NOT_RESOLVABLE_TO_CONCRETE_TYPE(responseType,
                            invocable.getHandlingMethod().toGenericString()));
        }

        final Path pathAnnotation = invocable.getHandlingMethod().getAnnotation(Path.class);
        if (pathAnnotation != null) {
            final String path = pathAnnotation.value();
            if (path == null || path.isEmpty() || "/".equals(path)) {

                Errors.warning(invocable.getHandlingMethod(),
                        LocalizationMessages.METHOD_EMPTY_PATH_ANNOTATION(
                                invocable.getHandlingMethod().getName(), invocable.getHandler().getHandlerClass().getName()));

            }
        }

    }

    private void checkValueProviders(ResourceMethod method) {
        final List<? extends Factory<?>> valueProviders = method.getInvocable().getValueProviders(locator);
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
    }

    private void checkParameters(ResourceMethod method) {
        final Invocable invocable = method.getInvocable();
        final Method handlingMethod = invocable.getHandlingMethod();
        int paramCount = 0;
        int nonAnnotatedParameters = 0;

        for (Parameter p : invocable.getParameters()) {
            validateParameter(p, handlingMethod, handlingMethod.toGenericString(), Integer.toString(++paramCount), false);
            if (method.getType() == ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR
                    && Parameter.Source.ENTITY == p.getSource()) {
                Errors.fatal(method, LocalizationMessages.SUBRES_LOC_HAS_ENTITY_PARAM(invocable.getHandlingMethod()));
            } else if (p.getAnnotations().length == 0) {
                nonAnnotatedParameters++;
                if (nonAnnotatedParameters > 1) {
                    Errors.fatal(method, LocalizationMessages.AMBIGUOUS_NON_ANNOTATED_PARAMETER(invocable.getHandlingMethod(),
                            invocable.getHandlingMethod().getDeclaringClass()));
                }
            }
        }
    }

    private static final Set<Class> PARAM_ANNOTATION_SET = createParamAnnotationSet();

    private static Set<Class> createParamAnnotationSet() {
        Set<Class> set = new HashSet<>(6);
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
     * @param injectionsForbidden   {@code true} if parameters cannot be injected by
     *                              parameter annotations, eg. {@link HeaderParam @HeaderParam}.
     */
    static void validateParameter(final Parameter parameter,
                                  final Object source,
                                  final String reportedSourceName,
                                  final String reportedParameterName,
                                  final boolean injectionsForbidden) {
        Errors.processWithException(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
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
}
