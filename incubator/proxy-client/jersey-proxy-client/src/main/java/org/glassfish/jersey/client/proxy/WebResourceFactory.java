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
import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Target;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.Maps;

/**
 * Factory for client-side representation of a resource.
 * See the <a href="package-summary.html">package overview</a>
 * for an example on how to use this class.
 *
 * @author Martin Matula (martin.matula at oracle.com)
 */
public final class WebResourceFactory implements InvocationHandler {
    private final Client client;
    private final URI rootUri;

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param client Client to be used for making requests to the server.
     * @param rootUri Root URI the resource is available at.
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    public static <C> C newResource(Class<C> resourceInterface, Client client, URI rootUri) {
        // TODO: which classloader should I use??
        return (C) Proxy.newProxyInstance(resourceInterface.getClassLoader(),
                new Class[] {resourceInterface}, new WebResourceFactory(client, rootUri));
    }

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param client Client to be used for making requests to the server.
     * @param rootUri Root URI the resource is available at.
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    public static <C> C newResource(Class<C> resourceInterface, Client client, String rootUri) {
        return newResource(resourceInterface, client, URI.create(rootUri));
    }

    private WebResourceFactory(Client client, URI rootUri) {
        this.client = client;
        this.rootUri = rootUri;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // get the interface describing the resource
        Class<?> proxyIfc = proxy.getClass().getInterfaces()[0];

        final StringBuilder logMessage = new StringBuilder();

        // get the base uri (rootUri + uri in @Path annotation on the interface)
        UriBuilder ub = UriBuilder.fromUri(rootUri);
        addPathFromAnnotation(proxyIfc, ub);

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
        boolean methodPath = addPathFromAnnotation(method, ub);

        if (httpMethod == null) {
            if (!methodPath) {
                // no path annotation on the method -> fail
                throw new UnsupportedOperationException("Not a resource method.");
            } else if (!responseType.isInterface()) {
                // the method is a subresource locator, but returns class,
                // not interface - can't help here
                throw new UnsupportedOperationException("Return type not an interface");
            } else {
                // the method is a subresource locator
                return newResource(responseType, client, ub.build());
            }
        }

        Target target = client.target(ub);
        logMessage.append("Invoking: client.target(\"").append(ub.build().toString()).append("\")");

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
                    target = target.pathParam(((PathParam) ann).value(), value);
                    logMessage.append(".pathParam(").append(((PathParam) ann).value())
                            .append(", ").append(value).append(")");
                } else if ((ann = anns.get((QueryParam.class))) != null) {
                    target = target.queryParam(((QueryParam) ann).value(), value);
                    logMessage.append(".queryParam(").append(((QueryParam) ann).value())
                            .append(", ").append(value).append(")");
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
        logMessage.append(".request(");
        if (accepts != null) {
            b = target.request(accepts);
            boolean first = true;
            for (String accept : accepts) {
                if (!first) logMessage.append(", ");
                logMessage.append("\"").append(accept).append("\"");
                first = false;
            }
        } else {
            b = target.request();
        }
        logMessage.append(").method(\"").append(httpMethod).append("\", ");

        Object result;
        if (entity != null) {
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
            }
            logMessage.append("Entity.entity(\"").append(entity).append("\", \"")
                    .append(contentType).append("\"), ");
            result = b.method(httpMethod, Entity.entity(entity, contentType), responseType);
        } else {
            result = b.method(httpMethod, responseType);
        }
        logMessage.append(responseType.getName()).append(".class)");
        Logger.getLogger(WebResourceFactory.class.getName()).finer(logMessage.toString());
        return result;
    }

    private boolean addPathFromAnnotation(AnnotatedElement ae, UriBuilder ub) {
        Path p = ae.getAnnotation(Path.class);
        if (p != null) {
            ub.path(p.value());
            return true;
        }
        return false;
    }

    private String getHttpMethodName(AnnotatedElement ae) {
        HttpMethod a = ae.getAnnotation(HttpMethod.class);
        return a == null ? null : a.value();
    }
}
