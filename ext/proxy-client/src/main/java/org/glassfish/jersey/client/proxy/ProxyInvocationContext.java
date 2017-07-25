/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.ParamConverterFactory;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.model.internal.ComponentBag;

/**
 * Helper class for client proxy invocations, factored out from WebResourceFactory.
 *
 * @author Martin Matula
 * @author Harald Wellmann
 */
class ProxyInvocationContext {

    private static final List<Class<?>> PARAM_ANNOTATION_CLASSES = Arrays.asList(PathParam.class,
        QueryParam.class, HeaderParam.class, CookieParam.class, MatrixParam.class, FormParam.class);

    private Method method;
    private Object[] args;
    private MultivaluedMap<String, Object> headers;
    private List<Cookie> cookies;
    private Form form = new Form();
    private Annotation[][] paramAnns;
    private Object entity;
    private Type entityType;
    private WebTarget target;
    private Map<Class<?>, Annotation> anns;

    private ParamConverterFactory paramConverterFactory;

    private Class<?> paramType;
    private Type genericParamType;

    private Annotation[] paramAnnotations;

    private InjectionManager injectionManager;

    public ProxyInvocationContext(Method method, Object[] args,
        MultivaluedMap<String, Object> headers, List<Cookie> cookies, Form form, WebTarget target) {
        this.method = method;
        this.args = args;
        // There is a compiler error if we use the diamond operator here.
        this.headers = new MultivaluedHashMap<String, Object>(headers);
        this.cookies = new LinkedList<>(cookies);
        this.paramAnns = method.getParameterAnnotations();
        this.form.asMap().putAll(form.asMap());
        this.target = target;
    }

    public Object getEntity() {
        return entity;
    }

    public Type getEntityType() {
        return entityType;
    }

    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public Form getForm() {
        return form;
    }

    public WebTarget getTarget() {
        return target;
    }

    public void convertParameters() {
        buildInjectionManager(target.getConfiguration());
        findParameterConverters();
        for (int i = 0; i < paramAnns.length; i++) {
            convertParameter(i);
        }
    }

    private boolean convertParameter(int pos) {
        paramType = method.getParameterTypes()[pos];
        genericParamType = method.getGenericParameterTypes()[pos];
        paramAnnotations = paramAnns[pos];
        anns = mapAnnotations(pos);
        Object value = args[pos];
        if (!hasAnyParamAnnotation(anns)) {
            entityType = method.getGenericParameterTypes()[pos];
            entity = value;
            return false;
        }

        if (value == null) {
            DefaultValue ann = getAnnotation(DefaultValue.class);
            if (ann != null) {
                value = ann.value();
            }
        }

        if (value == null) {
            return false;
        }

        return convertPathParam(value) || convertQueryParam(value) || convertHeaderParam(value)
            || convertCookieParam(value) || convertMatrixParam(value) || convertFormParam(value);
    }

    private void buildInjectionManager(Configuration config) {
        ComponentBag componentBag = ComponentBag.newInstance(ComponentBag.INCLUDE_ALL);
        for (Class<?> klass : config.getClasses()) {
            componentBag.register(klass, ComponentBag.AS_IS);
        }
        for (Object instance : config.getInstances()) {
            componentBag.register(instance, ComponentBag.AS_IS);
        }
        injectionManager = Injections.createInjectionManager();
        ProviderBinder.bindProviders(componentBag, RuntimeType.CLIENT, null, injectionManager);

        injectionManager.completeRegistration();
    }

    private void findParameterConverters() {
        Set<ParamConverterProvider> providers = Providers.getProviders(injectionManager,
            ParamConverterProvider.class);
        Set<ParamConverterProvider> customProviders = Providers.getCustomProviders(injectionManager,
            ParamConverterProvider.class);
        paramConverterFactory = new ParamConverterFactory(providers, customProviders);
    }

    private boolean convertQueryParam(Object value) {
        QueryParam ann = getAnnotation(QueryParam.class);
        if (ann == null) {
            return false;
        }

        if (value instanceof Collection) {
            target = target.queryParam(ann.value(), convertCollection((Collection<?>) value));
        } else {
            target = target.queryParam(ann.value(), convert(value));
        }
        return true;
    }

    private boolean convertFormParam(Object value) {
        FormParam ann = getAnnotation(FormParam.class);
        if (ann == null) {
            return false;
        }

        if (value instanceof Collection) {
            for (final Object v : ((Collection<?>) value)) {
                form.param(ann.value(), convert(v).toString());
            }
        } else {
            form.param(ann.value(), convert(value).toString());
        }
        return true;
    }

    private boolean convertMatrixParam(Object value) {
        MatrixParam ann = getAnnotation(MatrixParam.class);
        if (ann == null) {
            return false;
        }

        if (value instanceof Collection) {
            target = target.matrixParam(ann.value(), convertCollection((Collection<?>) value));
        } else {
            target = target.matrixParam(ann.value(), convert(value));
        }
        return true;
    }

    private boolean convertCookieParam(Object value) {
        CookieParam ann = getAnnotation(CookieParam.class);
        if (ann == null) {
            return false;
        }

        final String name = ann.value();
        Cookie c;
        if (value instanceof Collection) {
            for (final Object v : ((Collection<?>) value)) {
                if (!(v instanceof Cookie)) {
                    c = new Cookie(name, convert(v).toString());
                } else {
                    c = (Cookie) v;
                    if (!name.equals(((Cookie) v).getName())) {
                        // is this the right thing to do? or should I fail? or
                        // ignore the difference?
                        c = new Cookie(name, c.getValue(), c.getPath(), c.getDomain(),
                            c.getVersion());
                    }
                }
                cookies.add(c);
            }
        } else {
            if (!(value instanceof Cookie)) {
                cookies.add(new Cookie(name, convert(value).toString()));
            } else {
                c = (Cookie) value;
                if (!name.equals(((Cookie) value).getName())) {
                    // is this the right thing to do? or should I fail? or ignore
                    // the difference?
                    cookies.add(
                        new Cookie(name, c.getValue(), c.getPath(), c.getDomain(), c.getVersion()));
                }
            }
        }
        return true;
    }

    private boolean convertHeaderParam(Object value) {
        HeaderParam ann = getAnnotation(HeaderParam.class);
        if (ann == null) {
            return false;
        }
        if (value instanceof Collection) {
            headers.addAll(ann.value(), convertCollection((Collection<?>) value));
        } else {
            headers.addAll(ann.value(), convert(value));
        }
        return true;
    }

    private boolean convertPathParam(Object value) {
        PathParam ann = getAnnotation(PathParam.class);
        if (ann == null) {
            return false;
        }
        target = target.resolveTemplate(ann.value(), convert(value));
        return true;
    }

    private Map<Class<?>, Annotation> mapAnnotations(int pos) {
        final Map<Class<?>, Annotation> anns = new HashMap<>();
        for (final Annotation ann : paramAnns[pos]) {
            anns.put(ann.annotationType(), ann);
        }
        return anns;
    }

    private boolean hasAnyParamAnnotation(final Map<Class<?>, Annotation> anns) {
        for (final Class<?> paramAnnotationClass : PARAM_ANNOTATION_CLASSES) {
            if (anns.containsKey(paramAnnotationClass)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T getAnnotation(Class<T> klass) {
        return (T) anns.get(klass);
    }

    private Object[] convertCollection(final Collection<?> values) {
        Object[] result = new Object[values.size()];
        int i = 0;
        for (Object v : values) {
            result[i] = convert(v);
            i++;
        }
        return result;
    }

    private <T> Object convert(T value) {
        @SuppressWarnings("unchecked")
        ParamConverter<T> converter = (ParamConverter<T>) paramConverterFactory
            .getConverter(paramType, genericParamType, paramAnnotations);
        if (converter == null) {
            return value;
        } else {
            return converter.toString(value);
        }
    }
}
