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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.CookieParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.AnnotatedMethod;
import org.glassfish.jersey.internal.util.MethodList;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.uri.UriTemplate;

/**
 * <p>Performs a basic check on abstract resources. Validity check populates a list of potential issues
 * with the given resource. The issues are divided into two categories: fatal and non-fatal issues.
 * The former type prevents the resource to be deployed and makes the whole web application
 * deployment fail.
 *
 * <p>To check a single resource class, one could
 * use the {@link IntrospectionModeller#createResource(java.lang.Class)} method
 * to get an abstract resource model. {@link ResourceModelValidator#validate(org.glassfish.jersey.server.model.ResourceModelComponent)}
 * method then populates the issue list, which could be then obtained by the {@link ResourceModelValidator#getIssueList()}.
 * Unless you explicitly clear the list, subsequent calls to the validate method will add new items to the list,
 * so that you can build the issue list for more than one resource. To clear the list, you may want to call
 * {@link ResourceModelValidator#cleanIssueList()} method.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class BasicValidator extends ResourceModelValidator {

    private MessageBodyWorkers workers;

    public BasicValidator() {};

    public BasicValidator(MessageBodyWorkers workers) {
        this.workers = workers;
    }

    @Override
    public void visitResourceClass(ResourceClass resource) {
        // uri template of the resource, if present should not contain null value
        if (resource.isRootResource() && ((null == resource.getPath()) || (null == resource.getPath().getValue()))) {
            issueList.add(new ResourceModelIssue(
                    resource,
                    LocalizationMessages.RES_URI_PATH_INVALID(resource.getResourceClass(), resource.getPath()),
                    true)); // TODO: is it really a fatal issue?
        }

        checkNonPublicMethods(resource);

        final Class<?> resourceClass = resource.getResourceClass();

        if(resourceClass != null) {
            checkResourceClassSetters(resourceClass);
            checkResourceClassFields(resourceClass);
        }

        checkConsumesProducesAmbiguities(resource);
        checkSRLAmbiguities(resource);
    }

    private void checkResourceClassSetters(Class<?> rc) {
        final MethodList methodList = new MethodList(rc);

        for (AnnotatedMethod m : methodList.
                hasNotMetaAnnotation(HttpMethod.class).
                hasNotAnnotation(Path.class).
                hasNumParams(1).
                hasReturnType(void.class).
                nameStartsWith("set")) {
            Parameter p = IntrospectionModeller.createParameter(
                    rc,
                    m.getMethod().getDeclaringClass(),
                    rc.isAnnotationPresent(Encoded.class),
                    m.getParameterTypes()[0],
                    m.getGenericParameterTypes()[0],
                    m.getAnnotations());
            if (null != p) {
                checkParameter(p, m.getMethod(), m.getMethod().toGenericString(), "1");
            }
        }
    }

    @Override
    public void visitResourceConstructor(ResourceConstructor constructor) {
        // TODO check parameters
    }

    private void checkResourceClassFields(Class<?> rc) {
        for (Field f : rc.getDeclaredFields()) {
            if (f.getDeclaredAnnotations().length > 0) {
                    Parameter p = IntrospectionModeller.createParameter(
                            rc,
                            f.getDeclaringClass(),
                            rc.isAnnotationPresent(Encoded.class),
                            f.getType(),
                            f.getGenericType(),
                            f.getAnnotations());
                    if (null != p) {
                        checkParameter(p, f, f.toGenericString(), f.getName());
                    }
            }
        }
    }


    @Override
    public void visitResourceMethod(ResourceMethod method) {
        checkMethod(method);
    }

    public void checkMethod(AbstractResourceMethod method) {

        final boolean isInvocable = method instanceof InvocableResourceMethod;

        if (!isInvocable) {
            return;
        }

        final InvocableResourceMethod invocable = (InvocableResourceMethod)method;

        checkParameters(invocable, invocable.getMethod());

        if ("GET".equals(method.getHttpMethod())) {
            // ensure GET returns non-void value
            if (void.class == invocable.getMethod().getReturnType()) {
                issueList.add(new ResourceModelIssue(
                        method,
                        LocalizationMessages.GET_RETURNS_VOID(invocable.getMethod()),
                        false));
            }

            // ensure GET does not consume an entity parameter
            if (invocable.hasEntity()) {
                issueList.add(new ResourceModelIssue(
                        method,
                        LocalizationMessages.GET_CONSUMES_ENTITY(invocable.getMethod()),
                        false));
            }
            // ensure GET does not consume any @FormParam annotated parameter
            for (Parameter p : invocable.getParameters()) {
                if (p.isAnnotationPresent(FormParam.class)) {
                    issueList.add(new ResourceModelIssue(
                            method,
                            LocalizationMessages.GET_CONSUMES_FORM_PARAM(invocable.getMethod()),
                            true));
                    break;
                }
            }
        }

        // ensure there is not multiple HTTP method designators specified on the method
        List<String> httpAnnotList = new LinkedList<String>();
        for (Annotation a : invocable.getMethod().getDeclaredAnnotations()) {
            if (null != a.annotationType().getAnnotation(HttpMethod.class)) {
                httpAnnotList.add(a.toString());
            } else if ((a.annotationType() == Path.class) && !(method instanceof AbstractSubResourceMethod)) {
                issueList.add(new ResourceModelIssue(
                        method, LocalizationMessages.SUB_RES_METHOD_TREATED_AS_RES_METHOD(invocable.getMethod(), ((Path)a).value()), false));
            }
        }
        if (httpAnnotList.size() > 1) {
            issueList.add(new ResourceModelIssue(
                    method,
                    LocalizationMessages.MULTIPLE_HTTP_METHOD_DESIGNATORS(invocable.getMethod(), httpAnnotList.toString()),
                    true));
        }

        final Type t = invocable.getGenericReturnType();
        if (!isConcreteType(t)) {
            issueList.add(new ResourceModelIssue(
                    invocable.getMethod(),
                    LocalizationMessages.TYPE_OF_METHOD_NOT_RESOLVABLE_TO_CONCRETE_TYPE(t, invocable.getMethod().toGenericString()),
                    false));
        }
    }

    @Override
    public void visitSubResourceMethod(SubResourceMethod method) {
        // check the same things that are being checked for resource methods
        checkMethod(method);
        // and make sure the Path is not null
        if ((null == method.getPath()) || (null == method.getPath().getValue()) || (method.getPath().getValue().length() == 0)) {
            issueList.add(new ResourceModelIssue(
                    method,
                    LocalizationMessages.SUBRES_METHOD_URI_PATH_INVALID(method.getMethod(), method.getPath()),
                    true));
        }
    }

    @Override
    public void visitSubResourceLocator(SubResourceLocator locator) {
        checkParameters(locator, locator.getMethod());
        if (void.class == locator.getMethod().getReturnType()) {
            issueList.add(new ResourceModelIssue(
                    locator,
                    LocalizationMessages.SUBRES_LOC_RETURNS_VOID(locator.getMethod()),
                    true));
        }
        if ((null == locator.getPath()) || (null == locator.getPath().getValue()) || (locator.getPath().getValue().length() == 0)) {
            issueList.add(new ResourceModelIssue(
                    locator,
                    LocalizationMessages.SUBRES_LOC_URI_PATH_INVALID(locator.getMethod(), locator.getPath()),
                    true));
        }
        // Sub-resource locator can not have an entity parameter
        for (Parameter parameter : locator.getParameters()) {
            if (Parameter.Source.ENTITY == parameter.getSource()) {
                issueList.add(new ResourceModelIssue(
                        locator,
                        LocalizationMessages.SUBRES_LOC_HAS_ENTITY_PARAM(locator.getMethod()),
                        true));
            }
        }
    }
    private static final Set<Class> ParamAnnotationSET = createParamAnnotationSet();

    private static Set<Class> createParamAnnotationSet() {
        Set<Class> set = new HashSet<Class>(6);
        set.add(Context.class);
        set.add(HeaderParam.class);
        set.add(CookieParam.class);
        set.add(MatrixParam.class);
        set.add(QueryParam.class);
        set.add(PathParam.class);
        return Collections.unmodifiableSet(set);
    }

    private void checkParameter(Parameter p, Object source, String nameForLogging, String paramNameForLogging) {
        int annotCount = 0;
        final Type t = p.getParameterType();
        final Annotation[] annotations = p.getAnnotations();
        for (Annotation a : annotations) {
            if (ParamAnnotationSET.contains(a.annotationType())) {
                annotCount++;
                if (annotCount > 1) {
                    issueList.add(new ResourceModelIssue(
                            source,
                            LocalizationMessages.AMBIGUOUS_PARAMETER(nameForLogging, paramNameForLogging),
                            false));
                    break;
                }
            }
        }

        if (!isConcreteType(t)) {
            issueList.add(new ResourceModelIssue(
                    source,
                    "Parameter " + paramNameForLogging + " of type " + t + " from " + nameForLogging + " is not resolvable to a concrete type",
                    false));
        }
    }

    private boolean isConcreteType(Type t) {
        if (t instanceof ParameterizedType) {
            return isConcreteParameterizedType((ParameterizedType)t);
        } else if (!(t instanceof Class)) {
            // GenericArrayType, WildcardType, TypeVariable
            return false;
        }

        return true;
    }

    private boolean isConcreteParameterizedType(ParameterizedType pt) {
        boolean isConcrete = true;
        for (Type t : pt.getActualTypeArguments()) {
            isConcrete &= isConcreteType(t);
        }

        return isConcrete;
    }

    private void checkParameters(Parameterized pl, Method m) {
        int paramCount = 0;
        for (Parameter p : pl.getParameters()) {
            checkParameter(p, m, m.toGenericString(), Integer.toString(++paramCount));
        }
    }


    private List<Method> getDeclaredMethods(final Class _c) {
        final List<Method> ml = new ArrayList<Method>();

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            Class c = _c;
            @Override
            public Object run() {
                while (c != Object.class && c != null) {
                    ml.addAll(Arrays.asList(c.getDeclaredMethods()));
                    c = c.getSuperclass();
                }
                return null;
            }
        });

        return ml;
    }

    private void checkNonPublicMethods(final ResourceClass ar) {

        final Class<?> resourceClass = ar.getResourceClass();

        if (resourceClass == null) {
            return;
        }

        final MethodList declaredMethods = new MethodList(getDeclaredMethods(resourceClass));

        // non-public resource methods
        for (AnnotatedMethod m : declaredMethods.hasMetaAnnotation(HttpMethod.class).
                hasNotAnnotation(Path.class).isNotPublic()) {
            issueList.add(new ResourceModelIssue(ar, LocalizationMessages.NON_PUB_RES_METHOD(m.getMethod().toGenericString()), false));
        }
        // non-public subres methods
        for (AnnotatedMethod m : declaredMethods.hasMetaAnnotation(HttpMethod.class).
                hasAnnotation(Path.class).isNotPublic()) {
            issueList.add(new ResourceModelIssue(ar, LocalizationMessages.NON_PUB_SUB_RES_METHOD(m.getMethod().toGenericString()), false));
        }
        // non-public subres locators
        for (AnnotatedMethod m : declaredMethods.hasNotMetaAnnotation(HttpMethod.class).
                hasAnnotation(Path.class).isNotPublic()) {
            issueList.add(new ResourceModelIssue(ar, LocalizationMessages.NON_PUB_SUB_RES_LOC(m.getMethod().toGenericString()), false));
        }
    }

    @Override
    public void visitInflectorResourceMethod(InflectorBasedResourceMethod method) {
    }

    private void checkConsumesProducesAmbiguities(ResourceClass resource) {

        final List<AbstractResourceMethod> resourceMethods = resource.getResourceMethods();

        if (resourceMethods.size() >= 2) {
            for (AbstractResourceMethod m1 : resourceMethods.subList(0, resourceMethods.size()-1)) {
                for (AbstractResourceMethod m2 : resourceMethods.subList(resourceMethods.indexOf(m1)+1, resourceMethods.size())) {
                    final boolean bothAreRealJavaMethods = (m1 instanceof InvocableResourceMethod)
                                                            && (m2 instanceof InvocableResourceMethod);
                    if (sameHttpMethod(m1, m2) && bothAreRealJavaMethods) {
                        checkIntersectingMediaTypes(resource, m1.getHttpMethod(), (InvocableResourceMethod)m1, (InvocableResourceMethod)m2, issueList);
                    }
                }
            }
        }

        final List<SubResourceMethod> subResourceMethods = resource.getSubResourceMethods();

        if (subResourceMethods.size() >= 2) {
            for (SubResourceMethod m1 : subResourceMethods.subList(0, subResourceMethods.size()-1)) {
                for (SubResourceMethod m2 : subResourceMethods.subList(subResourceMethods.indexOf(m1)+1, subResourceMethods.size())) {
                    final boolean bothAreRealJavaMethods = (m1 instanceof InvocableResourceMethod)
                                                            && (m2 instanceof InvocableResourceMethod);
                    if (samePath(m1, m2) && sameHttpMethod(m1, m2) && bothAreRealJavaMethods) {
                        checkIntersectingMediaTypes(resource, m1.getHttpMethod(), (InvocableResourceMethod)m1, (InvocableResourceMethod)m2, issueList);
                    }
                }
            }
        }
    }


    private void checkSRLAmbiguities(ResourceClass resource) {

        final List<SubResourceLocator> subResourceLocators = resource.getSubResourceLocators();

        if (subResourceLocators.size() >= 2) {
            for (SubResourceLocator m1 : subResourceLocators.subList(0, subResourceLocators.size()-1)) {
                for (SubResourceLocator m2 : subResourceLocators.subList(subResourceLocators.indexOf(m1)+1, subResourceLocators.size())) {
                    if (samePath(m1, m2)) {
                        issueList.add(new ResourceModelIssue(resource, LocalizationMessages.AMBIGUOUS_SRLS(resource.getResourceClass().getName(), m1.getPath(), m2.getPath()), true));
                    }
                }
            }
        }
    }


    private void checkIntersectingMediaTypes(ResourceClass resource, String httpMethod, InvocableResourceMethod im1, InvocableResourceMethod im2, List<ResourceModelIssue> issueList) {
        final List<MediaType> inputTypes1 = getEffectiveInputTypes(im1);
        final List<MediaType> inputTypes2 = getEffectiveInputTypes(im2);
        final List<MediaType> outputTypes1 = getEffectiveOutputTypes(im1);
        final List<MediaType> outputTypes2 = getEffectiveOutputTypes(im2);

        if (intersectingMediaTypes(inputTypes1, inputTypes2, outputTypes1, outputTypes2)) {
            final String rcName = resource.getResourceClass().getName();
            if (im1.hasEntity()) {
                issueList.add(new ResourceModelIssue(resource, LocalizationMessages.AMBIGUOUS_RMS_IN(rcName, httpMethod, im1.getMethod(), im2.getMethod()), true));
            } else {
                issueList.add(new ResourceModelIssue(resource, LocalizationMessages.AMBIGUOUS_RMS_OUT(rcName, httpMethod, im1.getMethod(), im2.getMethod()), true));
            }
        }
    }

    private static final List<MediaType> StarTypeList = Arrays.asList(new MediaType("*", "*"));

    private List getEffectiveInputTypes(final InvocableResourceMethod invocableMethod) {
        if (!invocableMethod.getSupportedInputTypes().isEmpty()) {
            return invocableMethod.getSupportedInputTypes();
        }
        List<MediaType> result = new LinkedList<MediaType>();
        if (workers != null) {
            for (Parameter p : invocableMethod.getParameters()) {
                if (p.getSource() == Parameter.Source.ENTITY) {
                    final Type paramType = p.getParameterType();
                    final Class paramClass = p.getParameterClass();
                    result.addAll(workers.getMessageBodyReaderMediaTypes(paramClass, paramType, p.getDeclaredAnnotations()));
                }
            }
        }
        return result.isEmpty() ? StarTypeList : result;
    }

    private List getEffectiveOutputTypes(final InvocableResourceMethod invocableMethod) {
        if (!invocableMethod.getSupportedOutputTypes().isEmpty()) {
            return invocableMethod.getSupportedOutputTypes();
        }
        List<MediaType> result = new LinkedList<MediaType>();
        if (workers != null) {
            result.addAll(workers.getMessageBodyWriterMediaTypes(
                            invocableMethod.getReturnType(),
                            invocableMethod.getGenericReturnType(),
                            invocableMethod.getMethod().getDeclaredAnnotations()));
        }
        return result.isEmpty() ? StarTypeList : result;
    }

    private boolean sameHttpMethod(AbstractResourceMethod m1, AbstractResourceMethod m2) {
        return m1.getHttpMethod().equals(m2.getHttpMethod());
    }

    private boolean intersectingMediaTypes(List<MediaType> i1, List<MediaType> i2, List<MediaType> o1, List<MediaType> o2) {
        return MediaTypes.intersect(i1, i2)
                        && MediaTypes.intersect(o1, o2);
    }

    private boolean samePath(PathAnnotated m1, PathAnnotated m2) {
        return new UriTemplate(m1.getPath().getValue()).equals(new UriTemplate(m2.getPath().getValue()));
    }
}