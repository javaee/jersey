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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RootRouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterModule.RouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterModule.RouteToPathBuilder;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;


/**
 * TODO: unify with RuntimeModelProvider to a RuntimeModel- or TreeAcceptor- Builder.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RuntimeModelProviderFromAbstractModel extends RuntimeModelProvider {

    @Inject
    private RootRouteBuilder<PathPattern> rootBuilder;
    @Inject
    private ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    @Inject
    Injector injector;
    @Inject
    Services services;

    private MessageBodyWorkers workers;

    @Inject Factory<RoutingContext> ctxFactory;

    private String currentResourcePath = null;
    private RouteToPathBuilder<PathPattern> lastRoutedBuilder;
    private Map<PathPattern, List<Pair<AbstractResourceMethod, Inflector<Request, Response>>>> method2Inflector =
            new HashMap<PathPattern, List<Pair<AbstractResourceMethod, Inflector<Request, Response>>>>();
    private Map<PathPattern, Pair<SubResourceLocator, TreeAcceptor>> locator2Acceptor =
            new HashMap<PathPattern, Pair<SubResourceLocator, TreeAcceptor>>();

    public RuntimeModelProviderFromAbstractModel() {}

    private RuntimeModelProviderFromAbstractModel(Factory<RoutingContext> ctxFactory, MessageBodyWorkers msgBodyWorkers) {
        this.ctxFactory = ctxFactory;
        this.workers = msgBodyWorkers;
    }

    @Override
    public TreeAcceptor getRuntimeModel() {
        if (!method2Inflector.isEmpty()) {
            final TreeSet<PathPattern> pathPatterns = new TreeSet<PathPattern>(PathPattern.COMPARATOR);
            pathPatterns.addAll(method2Inflector.keySet());
            for (PathPattern path : pathPatterns) {
                List<Pair<AbstractResourceMethod, Inflector<Request, Response>>> methodInflectors = method2Inflector.get(path);
                lastRoutedBuilder = ((lastRoutedBuilder == null) ? rootBuilder : lastRoutedBuilder).route(path).to(
                                        new PushResourceAndDelegateTreeAcceptor(injector, ctxFactory, getDeclaringResource(methodInflectors),
                                            new MultipleMethodAcceptor(injector, workers, ctxFactory, methodInflectors)));
            }
            method2Inflector.clear();
        }
        if (!locator2Acceptor.isEmpty()) {
            final TreeSet<PathPattern> pathPatterns = new TreeSet<PathPattern>(PathPattern.COMPARATOR);
            pathPatterns.addAll(locator2Acceptor.keySet());
            for (PathPattern path : pathPatterns) {
                lastRoutedBuilder = (routedBuilder()).route(path).to(
                    new PushResourceAndDelegateTreeAcceptor(injector, ctxFactory, locator2Acceptor.get(path).left().getResource(), locator2Acceptor.get(path).right()));
            }
            locator2Acceptor.clear();
        }
        // TODO! check for null (lastRoutedBulder can be null when you try to build empty
        //       application - Application.builder().build(); NPE shouldn't be thrown!
        return rootBuilder.root(lastRoutedBuilder.build());
    }

    private RouteBuilder<PathPattern> routedBuilder() {
        return lastRoutedBuilder == null ? rootBuilder : lastRoutedBuilder;
    }

    @Override
    public void visitResourceClass(ResourceClass resource) {
        if (resource.isRootResource()) {
            currentResourcePath = resource.getPath().getValue();
        }
    }

    public void setWorkers(MessageBodyWorkers workers) {
        this.workers = workers;
    }

    @Override
    public void visitResourceMethod(final ResourceMethod method) {
        if (method.getDeclaringResource().isRootResource()) {
            addMethodInflector(joinUriPaths(currentResourcePath), method, createInflector(method));
        } else {
            addMethodInflector("/", method, createInflector(method));
        }
    }

    private Inflector<Request, Response> createInflector(final InvocableResourceMethod method) {
        return resourceMethodInvokerBuilder.build(method);
    }

    @Override
    public void visitSubResourceMethod(SubResourceMethod method) {
        if (method.getDeclaringResource().isRootResource()) {
            final String subResMethodPath = joinUriPaths(currentResourcePath, method.getPath().getValue());
            addMethodInflector(subResMethodPath, method, createInflector(method));
        } else {
            final String subResMethodPath = joinUriPaths(method.getPath().getValue());
            addMethodInflector(subResMethodPath, method, createInflector(method));
        }

    }

    private void addMethodInflector(String path, AbstractResourceMethod method, Inflector<Request, Response> inflector) {
        final PathPattern pathPattern = new PathPattern(path, PathPattern.RightHandPath.capturingZeroSegments);
        if (!method2Inflector.containsKey(pathPattern)) {
            method2Inflector.put(pathPattern, new LinkedList<Pair<AbstractResourceMethod, Inflector<Request, Response>>>());
        }
        method2Inflector.get(pathPattern).add(Tuples.of(method, inflector));
    }

    @Override
    public void visitSubResourceLocator(SubResourceLocator locator) {
        locator2Acceptor.put(new PathPattern(joinUriPaths(currentResourcePath, locator.getPath().getValue())),
                                    Tuples.<SubResourceLocator, TreeAcceptor>of(locator,
                                            new SubResourceLocatorAcceptor(injector, services, ctxFactory, workers, locator)));
        }

    @Override
    public void visitResourceConstructor(ResourceConstructor constructor) {
    }

    @Override
    public void visitInflectorResourceMethod(InflectorBasedResourceMethod method) {
        addMethodInflector(currentResourcePath, method, method.inflector);
    }

    private ResourceClass getDeclaringResource(List<Pair<AbstractResourceMethod, Inflector<Request, Response>>> method2InflectorList) {
        for (Pair<AbstractResourceMethod, Inflector<Request, Response>> methodInflector : method2InflectorList) {
            if (methodInflector.left() instanceof InvocableResourceMethod) {
                return methodInflector.left().getDeclaringResource();
            }
        }
        return null;
    }

    private String joinUriPaths(String... path) {
        StringBuilder result = new StringBuilder("");
        for (String p : path) {
            trimTrailingSlashes(result);
            result.append('/');
            result.append(trimStartingSlashes(p));
        }
        return result.toString();
    }

    private void trimTrailingSlashes(StringBuilder result) {
        // TODO: what about encoded slashes?
        while (result.length() > 0 && result.charAt(result.length()-1) == '/') {
            result.deleteCharAt(result.length()-1);
        }
    }

    // TODO: what about encoded slashes? what about empty strings?
    private String trimStartingSlashes(String s) {
        if (s.isEmpty()) {
            return s;
        }

        if(s.equals("/")) {
            return "";
        }

        int i = 0; while(s.charAt(i) == '/') i++;
        return s.substring(i);
    }
}
