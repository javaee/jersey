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
package org.glassfish.jersey.server.internal.routing;

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
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.server.model.AbstractResourceMethod;
import org.glassfish.jersey.server.model.InvocableResourceMethod;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.RuntimeModelProvider;
import org.glassfish.jersey.server.model.SubResourceLocator;
import org.glassfish.jersey.server.model.SubResourceMethod;

import org.jvnet.hk2.annotations.Inject;

/**
 * This is a common base for root resource and sub-resource runtime model provider.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class RuntimeModelProviderBase extends RuntimeModelProvider {

    @Inject
    RootRouteBuilder<PathPattern> rootBuilder;
    @Inject
    ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    @Inject
    Injector injector;
    @Inject
    Services services;

    MessageBodyWorkers workers;
    private RouteToPathBuilder<PathPattern> lastRoutedBuilder;

    abstract class SubResourceEntry {
        abstract String getSubResourcePathPattern();
    }

    class SubResourceMethodEntry extends SubResourceEntry {
        String supportedHttpMethod;
        SubResourceMethod srm;
        Inflector<Request, Response> inflector;

        SubResourceMethodEntry(String supportedHttpMethod, SubResourceMethod srm, Inflector<Request, Response> inflector) {
            this.supportedHttpMethod = supportedHttpMethod;
            this.srm = srm;
            this.inflector = inflector;
        }

        @Override
        String getSubResourcePathPattern() {
            return srm.getPath().getValue();
        }
    }

    class SubResourceLocatorEntry extends SubResourceEntry {
        SubResourceLocator srl;
        TreeAcceptor aceptor;

        SubResourceLocatorEntry(SubResourceLocator srl, TreeAcceptor aceptor) {
            this.srl = srl;
            this.aceptor = aceptor;
        }

        @Override
        String getSubResourcePathPattern() {
            return srl.getPath().getValue();
        }
    }

    private Map<PathPattern, List<Pair<AbstractResourceMethod, Inflector<Request, Response>>>> method2Inflector =
            new HashMap<PathPattern, List<Pair<AbstractResourceMethod, Inflector<Request, Response>>>>();

    private Map<PathPattern, Map<PathPattern, List<SubResourceEntry>>> locators =
            new HashMap<PathPattern, Map<PathPattern, List<SubResourceEntry>>>();

    public RuntimeModelProviderBase() {
    }

    public RuntimeModelProviderBase(MessageBodyWorkers msgBodyWorkers) {
        this.workers = msgBodyWorkers;
    }

    TreeAcceptor adaptSubResourceMethodAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return acceptor;
    }

    TreeAcceptor adaptResourceMethodAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return acceptor;
    }

    TreeAcceptor adaptSubResourceLocatorAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return acceptor;
    }

    abstract TreeAcceptor createFinalTreeAcceptor(RootRouteBuilder<PathPattern> rootRouteBuilder,
                                                    RouteToPathBuilder<PathPattern> lastRoutedBuilder);

    @Override
    public TreeAcceptor getRuntimeModel() {
        if (!method2Inflector.isEmpty()) {
            final TreeSet<PathPattern> pathPatterns = new TreeSet<PathPattern>(PathPattern.COMPARATOR);
            pathPatterns.addAll(method2Inflector.keySet());
            for (PathPattern path : pathPatterns) {
                List<Pair<AbstractResourceMethod, Inflector<Request, Response>>> methodInflectors = method2Inflector.get(path);
                lastRoutedBuilder = routedBuilder().route(path).to(adaptResourceMethodAcceptor(
                                getDeclaringResource(methodInflectors),
                                new MultipleMethodAcceptor(injector, workers, methodInflectors)));
            }
            method2Inflector.clear();
        }
        if (!locators.isEmpty()) {
            final TreeSet<PathPattern> pathPatterns = new TreeSet<PathPattern>(PathPattern.COMPARATOR);
            pathPatterns.addAll(locators.keySet());
            for (PathPattern path : pathPatterns) {
                RouteToPathBuilder<PathPattern> srRoutedBuilder = null;
                final TreeSet<PathPattern> srPathPatterns = new TreeSet<PathPattern>(PathPattern.COMPARATOR);
                srPathPatterns.addAll(locators.get(path).keySet());
                for (PathPattern srPath : srPathPatterns) {
                    List<Pair<AbstractResourceMethod, Inflector<Request, Response>>> methodInflectors =
                            new LinkedList<Pair<AbstractResourceMethod, Inflector<Request, Response>>>();
                    SubResourceLocatorEntry srl = null;
                    for (SubResourceEntry entry : locators.get(path).get(srPath)) {
                        if (entry instanceof SubResourceMethodEntry) {
                            final SubResourceMethodEntry srm = (SubResourceMethodEntry) entry;
                            methodInflectors.add(
                                    Tuples.<AbstractResourceMethod, Inflector<Request, Response>>of(srm.srm, srm.inflector));
                        } else {
                            srl = (SubResourceLocatorEntry) entry;
                        }
                    }
                    if (!methodInflectors.isEmpty()) {
                        final PathPattern srPathEmptyRHP = new PathPattern(srPath.getTemplate().getTemplate(), PathPattern.RightHandPath.capturingZeroSegments);
                        srRoutedBuilder = ((srRoutedBuilder == null) ? rootBuilder : srRoutedBuilder).route(srPathEmptyRHP).to(
                                    adaptSubResourceMethodAcceptor(getDeclaringResource(methodInflectors), new MultipleMethodAcceptor(injector, workers, methodInflectors)));
                    }
                    if (srl != null) {
                        srRoutedBuilder = ((srRoutedBuilder == null) ? rootBuilder : srRoutedBuilder).route(srPath).to(
                            adaptSubResourceLocatorAcceptor(srl.srl.getResource(), srl.aceptor));
                    }
                }
                lastRoutedBuilder = routedBuilder().route(path).to(srRoutedBuilder.build());
            }
            locators.clear();
        }
        // TODO! check for null (lastRoutedBulder can be null when you try to build empty
        //       application - Application.builder().build(); NPE shouldn't be thrown!
        return createFinalTreeAcceptor(rootBuilder, lastRoutedBuilder);//rootBuilder.root(lastRoutedBuilder.build());
    }

    RouteBuilder<PathPattern> routedBuilder() {
        return lastRoutedBuilder == null ? rootBuilder : lastRoutedBuilder;
    }

    public void setWorkers(MessageBodyWorkers workers) {
        this.workers = workers;
    }

    Inflector<Request, Response> createInflector(final InvocableResourceMethod method) {
        return resourceMethodInvokerBuilder.build(method);
    }

    void addMethodInflector(PathPattern pathPattern, AbstractResourceMethod method, Inflector<Request, Response> inflector) {
        if (!method2Inflector.containsKey(pathPattern)) {
            method2Inflector.put(pathPattern, new LinkedList<Pair<AbstractResourceMethod, Inflector<Request, Response>>>());
        }
        method2Inflector.get(pathPattern).add(Tuples.of(method, inflector));
    }

    void addSubResourceLocatorEntry(String path, SubResourceEntry entry) {
        final PathPattern pathPattern = new PathPattern(path);
        if (!locators.containsKey(pathPattern)) {
            locators.put(pathPattern, new HashMap<PathPattern, List<SubResourceEntry>>());
        }
        final PathPattern subResourcePathPattern = new PathPattern(entry.getSubResourcePathPattern());
        final Map<PathPattern, List<SubResourceEntry>> singleResourceMap = locators.get(pathPattern);
        if (!singleResourceMap.containsKey(subResourcePathPattern)) {
            singleResourceMap.put(subResourcePathPattern, new LinkedList<SubResourceEntry>());
        }
        singleResourceMap.get(subResourcePathPattern).add(entry);
    }

    ResourceClass getDeclaringResource(List<Pair<AbstractResourceMethod, Inflector<Request, Response>>> method2InflectorList) {
        for (Pair<AbstractResourceMethod, Inflector<Request, Response>> methodInflector : method2InflectorList) {
            if (methodInflector.left() instanceof InvocableResourceMethod) {
                return methodInflector.left().getDeclaringResource();
            }
        }
        return null;
    }
}
