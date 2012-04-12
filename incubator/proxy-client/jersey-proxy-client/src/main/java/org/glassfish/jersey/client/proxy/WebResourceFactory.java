/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Target;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Maps;

/**
 * Factory for client-side representation of a resource.
 * See the <a href="package-summary.html">package overview</a>
 * for an example on how to use this class.
 *
 * @author Martin Matula (martin.matula at oracle.com)
 */
public final class WebResourceFactory implements InvocationHandler {
    private final Target target;

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     *
     * Calling this method has the same effect as calling {@code WebResourceFactory.newResource(resourceInterface, rootTarget, false)}.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param target Target pointing to the resource or the parent of the resource.
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    public static <C> C newResource(Class<C> resourceInterface, Target target) {
        return newResource(resourceInterface, target, false);
    }

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param target Target pointing to the resource or the parent of the resource.
     * @param ignoreResourcePath If set to true, ignores path annotation on the resource interface (this is used when creating sub-resources)
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    public static <C> C newResource(Class<C> resourceInterface, Target target, boolean ignoreResourcePath) {
        return (C) Proxy.newProxyInstance(resourceInterface.getClassLoader(),
                new Class[] {resourceInterface}, new WebResourceFactory(ignoreResourcePath ? target : addPathFromAnnotation(resourceInterface, target)));
    }

    private WebResourceFactory(Target target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // get the interface describing the resource
        Class<?> proxyIfc = proxy.getClass().getInterfaces()[0];

        // response type
        Class<?> responseType = method.getReturnType();

        // determine method name
        String httpMethod = getHttpMethodName(method);
        if (httpMethod == null) {
            for (Annotation ann : method.getAnnotations()) {
                httpMethod = getHttpMethodName(ann.annotationType());
                if (httpMethod != null) {
                    break;
                }
            }
        }

        // create a new UriBuilder appending the @Path attached to the method
        Target newTarget = addPathFromAnnotation(method, target);

        if (httpMethod == null) {
            if (newTarget == target) {
                // no path annotation on the method -> fail
                throw new UnsupportedOperationException("Not a resource method.");
            } else if (!responseType.isInterface()) {
                // the method is a subresource locator, but returns class,
                // not interface - can't help here
                throw new UnsupportedOperationException("Return type not an interface");
            } else {
                // the method is a subresource locator
                return WebResourceFactory.newResource(responseType, newTarget, true);
            }
        }

        // accepted media types
        Produces produces = method.getAnnotation(Produces.class);
        if (produces == null) {
            produces = proxyIfc.getAnnotation(Produces.class);
        }
        String[] accepts = produces == null ? null : produces.value();

        // process method params (build maps of (Path|Form|Cookie|Matrix|..)Params
        // and extract entity type
        Annotation[][] paramAnns = method.getParameterAnnotations();
        Object entity = null;
        for (int i = 0; i < paramAnns.length; i++) {
            Map<Class, Annotation> anns = Maps.newHashMap();
            for (Annotation ann : paramAnns[i]) {
                anns.put(ann.annotationType(), ann);
            }
            Annotation ann;
            Object value = args[i];
            if (anns.isEmpty()) {
                entity = value;
            } else {
                if (value == null && (ann = anns.get(DefaultValue.class)) != null) {
                    value = ((DefaultValue) ann).value();
                }
                if ((ann = anns.get(PathParam.class)) != null) {
                    newTarget = newTarget.pathParam(((PathParam) ann).value(), value);
                } else if ((ann = anns.get((QueryParam.class))) != null) {
                    newTarget = newTarget.queryParam(((QueryParam) ann).value(), value);
                }
                // TODO: add support for FormParam, MatrixParam, CookieParam and others
            }
        }

        // determine content type
        String contentType = null;
        if (entity != null) {
            Consumes consumes = method.getAnnotation(Consumes.class);
            if (consumes == null) {
                consumes = proxyIfc.getAnnotation(Consumes.class);
            }
            if (consumes != null && consumes.value().length > 0) {
                // TODO: should consider q/qs instead of picking the first one
                contentType = consumes.value()[0];
            }
        }

        Invocation.Builder b;
        if (accepts != null) {
            b = newTarget.request(accepts);
        } else {
            b = newTarget.request();
        }

        Object result;
        if (entity != null) {
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
            }
            result = b.method(httpMethod, Entity.entity(entity, contentType), responseType);
        } else {
            result = b.method(httpMethod, responseType);
        }
        return result;
    }

    private static Target addPathFromAnnotation(AnnotatedElement ae, Target target) {
        Path p = ae.getAnnotation(Path.class);
        if (p != null) {
            target = target.path(p.value());
        }
        return target;
    }

    private static String getHttpMethodName(AnnotatedElement ae) {
        HttpMethod a = ae.getAnnotation(HttpMethod.class);
        return a == null ? null : a.value();
    }
}
