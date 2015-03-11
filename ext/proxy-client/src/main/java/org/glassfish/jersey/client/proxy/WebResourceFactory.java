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

package org.glassfish.jersey.client.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Factory for client-side representation of a resource.
 * See the <a href="package-summary.html">package overview</a>
 * for an example on how to use this class.
 *
 * @author Martin Matula
 */
public final class WebResourceFactory implements InvocationHandler {

    private static final String[] EMPTY = {};

    private final WebTarget target;
    private final MultivaluedMap<String, Object> headers;
    private final List<Cookie> cookies;
    private final Form form;

    private static final MultivaluedMap<String, Object> EMPTY_HEADERS = new MultivaluedHashMap<>();
    private static final Form EMPTY_FORM = new Form();
    private static final List<Class> PARAM_ANNOTATION_CLASSES = Arrays.<Class>asList(PathParam.class, QueryParam.class,
            HeaderParam.class, CookieParam.class, MatrixParam.class, FormParam.class);

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     * <p/>
     * Calling this method has the same effect as calling {@code WebResourceFactory.newResource(resourceInterface, rootTarget,
     *false)}.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param target WebTarget pointing to the resource or the parent of the resource.
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    public static <C> C newResource(final Class<C> resourceInterface, final WebTarget target) {
        return newResource(resourceInterface, target, false, EMPTY_HEADERS, Collections.<Cookie>emptyList(), EMPTY_FORM);
    }

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param target WebTarget pointing to the resource or the parent of the resource.
     * @param ignoreResourcePath If set to true, ignores path annotation on the resource interface (this is used when creating
     * sub-resources)
     * @param headers Header params collected from parent resources (used when creating a sub-resource)
     * @param cookies Cookie params collected from parent resources (used when creating a sub-resource)
     * @param form Form params collected from parent resources (used when creating a sub-resource)
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    @SuppressWarnings("unchecked")
    public static <C> C newResource(final Class<C> resourceInterface,
                                    final WebTarget target,
                                    final boolean ignoreResourcePath,
                                    final MultivaluedMap<String, Object> headers,
                                    final List<Cookie> cookies,
                                    final Form form) {

        return (C) Proxy.newProxyInstance(AccessController.doPrivileged(ReflectionHelper.getClassLoaderPA(resourceInterface)),
                new Class[] {resourceInterface},
                new WebResourceFactory(ignoreResourcePath ? target : addPathFromAnnotation(resourceInterface, target),
                        headers, cookies, form));
    }

    private WebResourceFactory(final WebTarget target, final MultivaluedMap<String, Object> headers,
                               final List<Cookie> cookies, final Form form) {
        this.target = target;
        this.headers = headers;
        this.cookies = cookies;
        this.form = form;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (args == null && method.getName().equals("toString")) {
            return toString();
        }

        // get the interface describing the resource
        final Class<?> proxyIfc = proxy.getClass().getInterfaces()[0];

        // response type
        final Class<?> responseType = method.getReturnType();

        // determine method name
        String httpMethod = getHttpMethodName(method);
        if (httpMethod == null) {
            for (final Annotation ann : method.getAnnotations()) {
                httpMethod = getHttpMethodName(ann.annotationType());
                if (httpMethod != null) {
                    break;
                }
            }
        }

        // create a new UriBuilder appending the @Path attached to the method
        WebTarget newTarget = addPathFromAnnotation(method, target);

        if (httpMethod == null) {
            if (newTarget == target) {
                // no path annotation on the method -> fail
                throw new UnsupportedOperationException("Not a resource method.");
            } else if (!responseType.isInterface()) {
                // the method is a subresource locator, but returns class,
                // not interface - can't help here
                throw new UnsupportedOperationException("Return type not an interface");
            }
        }

        // process method params (build maps of (Path|Form|Cookie|Matrix|Header..)Params
        // and extract entity type
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<String, Object>(this.headers);
        final LinkedList<Cookie> cookies = new LinkedList<>(this.cookies);
        final Form form = new Form();
        form.asMap().putAll(this.form.asMap());
        final Annotation[][] paramAnns = method.getParameterAnnotations();
        Object entity = null;
        Type entityType = null;
        for (int i = 0; i < paramAnns.length; i++) {
            final Map<Class, Annotation> anns = new HashMap<>();
            for (final Annotation ann : paramAnns[i]) {
                anns.put(ann.annotationType(), ann);
            }
            Annotation ann;
            Object value = args[i];
            if (!hasAnyParamAnnotation(anns)) {
                entityType = method.getGenericParameterTypes()[i];
                entity = value;
            } else {
                if (value == null && (ann = anns.get(DefaultValue.class)) != null) {
                    value = ((DefaultValue) ann).value();
                }

                if (value != null) {
                    if ((ann = anns.get(PathParam.class)) != null) {
                        newTarget = newTarget.resolveTemplate(((PathParam) ann).value(), value);
                    } else if ((ann = anns.get((QueryParam.class))) != null) {
                        if (value instanceof Collection) {
                            newTarget = newTarget.queryParam(((QueryParam) ann).value(), convert((Collection) value));
                        } else {
                            newTarget = newTarget.queryParam(((QueryParam) ann).value(), value);
                        }
                    } else if ((ann = anns.get((HeaderParam.class))) != null) {
                        if (value instanceof Collection) {
                            headers.addAll(((HeaderParam) ann).value(), convert((Collection) value));
                        } else {
                            headers.addAll(((HeaderParam) ann).value(), value);
                        }

                    } else if ((ann = anns.get((CookieParam.class))) != null) {
                        final String name = ((CookieParam) ann).value();
                        Cookie c;
                        if (value instanceof Collection) {
                            for (final Object v : ((Collection) value)) {
                                if (!(v instanceof Cookie)) {
                                    c = new Cookie(name, v.toString());
                                } else {
                                    c = (Cookie) v;
                                    if (!name.equals(((Cookie) v).getName())) {
                                        // is this the right thing to do? or should I fail? or ignore the difference?
                                        c = new Cookie(name, c.getValue(), c.getPath(), c.getDomain(), c.getVersion());
                                    }
                                }
                                cookies.add(c);
                            }
                        } else {
                            if (!(value instanceof Cookie)) {
                                cookies.add(new Cookie(name, value.toString()));
                            } else {
                                c = (Cookie) value;
                                if (!name.equals(((Cookie) value).getName())) {
                                    // is this the right thing to do? or should I fail? or ignore the difference?
                                    cookies.add(new Cookie(name, c.getValue(), c.getPath(), c.getDomain(), c.getVersion()));
                                }
                            }
                        }
                    } else if ((ann = anns.get((MatrixParam.class))) != null) {
                        if (value instanceof Collection) {
                            newTarget = newTarget.matrixParam(((MatrixParam) ann).value(), convert((Collection) value));
                        } else {
                            newTarget = newTarget.matrixParam(((MatrixParam) ann).value(), value);
                        }
                    } else if ((ann = anns.get((FormParam.class))) != null) {
                        if (value instanceof Collection) {
                            for (final Object v : ((Collection) value)) {
                                form.param(((FormParam) ann).value(), v.toString());
                            }
                        } else {
                            form.param(((FormParam) ann).value(), value.toString());
                        }
                    }
                }
            }
        }

        if (httpMethod == null) {
            // the method is a subresource locator
            return WebResourceFactory.newResource(responseType, newTarget, true, headers, cookies, form);
        }

        // accepted media types
        Produces produces = method.getAnnotation(Produces.class);
        if (produces == null) {
            produces = proxyIfc.getAnnotation(Produces.class);
        }
        final String[] accepts = (produces == null) ? EMPTY : produces.value();

        // determine content type
        String contentType = null;
        if (entity != null) {
            final List<Object> contentTypeEntries = headers.get(HttpHeaders.CONTENT_TYPE);
            if ((contentTypeEntries != null) && (!contentTypeEntries.isEmpty())) {
                contentType = contentTypeEntries.get(0).toString();
            } else {
                Consumes consumes = method.getAnnotation(Consumes.class);
                if (consumes == null) {
                    consumes = proxyIfc.getAnnotation(Consumes.class);
                }
                if (consumes != null && consumes.value().length > 0) {
                    contentType = consumes.value()[0];
                }
            }
        }

        Invocation.Builder builder = newTarget.request()
                .headers(headers) // this resets all headers so do this first
                .accept(accepts); // if @Produces is defined, propagate values into Accept header; empty array is NO-OP

        for (final Cookie c : cookies) {
            builder = builder.cookie(c);
        }

        final Object result;

        if (entity == null && !form.asMap().isEmpty()) {
            entity = form;
            contentType = MediaType.APPLICATION_FORM_URLENCODED;
        } else {
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }
            if (!form.asMap().isEmpty()) {
                if (entity instanceof Form) {
                    ((Form) entity).asMap().putAll(form.asMap());
                } else {
                    // TODO: should at least log some warning here
                }
            }
        }

        final GenericType responseGenericType = new GenericType(method.getGenericReturnType());
        if (entity != null) {
            if (entityType instanceof ParameterizedType) {
                entity = new GenericEntity(entity, entityType);
            }
            result = builder.method(httpMethod, Entity.entity(entity, contentType), responseGenericType);
        } else {
            result = builder.method(httpMethod, responseGenericType);
        }

        return result;
    }

    private boolean hasAnyParamAnnotation(final Map<Class, Annotation> anns) {
        for (final Class paramAnnotationClass : PARAM_ANNOTATION_CLASSES) {
            if (anns.containsKey(paramAnnotationClass)) {
                return true;
            }
        }
        return false;
    }

    private Object[] convert(final Collection value) {
        return value.toArray();
    }

    private static WebTarget addPathFromAnnotation(final AnnotatedElement ae, WebTarget target) {
        final Path p = ae.getAnnotation(Path.class);
        if (p != null) {
            target = target.path(p.value());
        }
        return target;
    }

    @Override
    public String toString() {
        return target.toString();
    }

    private static String getHttpMethodName(final AnnotatedElement ae) {
        final HttpMethod a = ae.getAnnotation(HttpMethod.class);
        return a == null ? null : a.value();
    }
}
