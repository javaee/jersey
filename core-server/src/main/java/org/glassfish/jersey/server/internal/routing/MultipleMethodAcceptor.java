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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.internal.util.collection.Tuples;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.model.AbstractResourceMethod;
import org.glassfish.jersey.server.model.InvocableResourceMethod;
import org.glassfish.jersey.server.model.Parameter;

import com.google.common.collect.Iterators;

/**
 * A single acceptor to be responsible to respond to all HTTP methods defined on a given resource.
 * Method selection algorithm is implemented here, which takes into account requested media type
 * and defined resource method media type capabilities.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
final class MultipleMethodAcceptor implements TreeAcceptor {

    /* package */class ConsumesProducesInflector {

        MediaType consumes;
        MediaType produces;
        Inflector<Request, Response> inflector;

        ConsumesProducesInflector(MediaType consumes, MediaType produces, Inflector<Request, Response> inflector) {
            this.inflector = inflector;
            this.consumes = consumes;
            this.produces = produces;
        }

        boolean isConsumable(Request request) {
            MediaType contentType = request.getHeaders().getMediaType();
            return contentType == null || consumes.isCompatible(contentType);
        }

        @Override
        public String toString() {
            return "ConsumesProducesInflector{" + "consumes=" + consumes + ", produces=" + produces + '}';
        }
    }
    final Map<String, List<ConsumesProducesInflector>> method2InflectorMap;
    final MessageBodyWorkers workers;
    final Injector injector;

    /* package */ MultipleMethodAcceptor(Injector injector, MessageBodyWorkers msgWorkers, List<Pair<AbstractResourceMethod, Inflector<Request, Response>>> method2InflectorList) {
        this.injector = injector;
        this.workers = msgWorkers;
        this.method2InflectorMap = new HashMap<String, List<ConsumesProducesInflector>>();
        for (final Pair<AbstractResourceMethod, Inflector<Request, Response>> methodInflector : method2InflectorList) {
            String httpMethod = methodInflector.left().getHttpMethod();
            if (!method2InflectorMap.containsKey(httpMethod)) {
                method2InflectorMap.put(httpMethod, new LinkedList<ConsumesProducesInflector>());
            }
            addAllCombinations(method2InflectorMap.get(httpMethod), methodInflector);
        }
    }

    void addAllCombinations(List<ConsumesProducesInflector> list, Pair<AbstractResourceMethod, Inflector<Request, Response>> methodInflector) {
        final List<MediaType> effectiveInputTypes = new LinkedList<MediaType>();
        final List<MediaType> effectiveOutputTypes = new LinkedList<MediaType>();
        AbstractResourceMethod resourceMethod = methodInflector.left();
        effectiveInputTypes.addAll(resourceMethod.getSupportedInputTypes());
        if (effectiveInputTypes.isEmpty()) {
            if (workers != null && resourceMethod instanceof InvocableResourceMethod) {
                final InvocableResourceMethod invocableMethod = (InvocableResourceMethod) resourceMethod;
                for (Parameter p : invocableMethod.getParameters()) {
                    if (p.getSource() == Parameter.Source.ENTITY) {
                        final Type paramType = p.getParameterType();
                        final Class paramClass = p.getParameterClass();
                        effectiveInputTypes.addAll(workers.getMessageBodyReaderMediaTypes(paramClass, paramType, p.getDeclaredAnnotations()));
                    }
                }
            }
        }
        if (effectiveInputTypes.isEmpty()) {
            effectiveInputTypes.add(MediaType.valueOf("*/*"));
        }
        effectiveOutputTypes.addAll(resourceMethod.getSupportedOutputTypes());
        if (effectiveOutputTypes.isEmpty()) {
            if (workers != null && resourceMethod instanceof InvocableResourceMethod) {
                final InvocableResourceMethod invocableMethod = (InvocableResourceMethod) resourceMethod;
                final Type returnType = invocableMethod.getGenericReturnType();
                final Class returnClass = invocableMethod.getReturnType();
                effectiveOutputTypes.addAll(workers.getMessageBodyWriterMediaTypes(returnClass, returnType, invocableMethod.getMethod().getDeclaredAnnotations()));
            }
        }
        if (effectiveOutputTypes.isEmpty()) {
            effectiveOutputTypes.add(MediaType.valueOf("*/*"));
        }
        for (MediaType consumes : effectiveInputTypes) {
            for (MediaType produces : effectiveOutputTypes) {
                list.add(new ConsumesProducesInflector(consumes, produces, methodInflector.right()));
            }
        }
        // TODO: take server quality factor into account
        // TODO: take server quality factor into account
        Collections.sort(list, new Comparator<ConsumesProducesInflector>() {

            @Override
            public int compare(ConsumesProducesInflector i1, ConsumesProducesInflector i2) {
                int consumesComparsion = MediaTypes.MEDIA_TYPE_COMPARATOR.compare(i1.consumes, i2.consumes);
                return (consumesComparsion != 0) ? consumesComparsion : MediaTypes.MEDIA_TYPE_COMPARATOR.compare(i1.produces, i2.produces);
            }
        });
    }

    @Override
    public Pair<Request, Iterator<TreeAcceptor>> apply(Request request) {
        List<ConsumesProducesInflector> inflectors = method2InflectorMap.get(request.getMethod());
        if (inflectors == null) {
            throw new WebApplicationException(Response.Status.METHOD_NOT_ALLOWED);
        }
        List<ConsumesProducesInflector> satisfyingInflectors = new LinkedList<ConsumesProducesInflector>();
        for (ConsumesProducesInflector cpi : inflectors) {
            if (cpi.isConsumable(request)) {
                satisfyingInflectors.add(cpi);
            }
        }
        if (satisfyingInflectors.isEmpty()) {
            throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
        }
        // TODO: remove try/catch clauses based on JERSEY-913 resolution
        // TODO: remove try/catch clauses based on JERSEY-913 resolution
        List<MediaType> acceptableMediaTypes;
        try {
            acceptableMediaTypes = request.getHeaders().getAcceptableMediaTypes();
        } catch (RuntimeException re) {
            throw new WebApplicationException(Response.status(400).entity(re.getMessage()).build());
        }
        for (MediaType acceptableMediaType : acceptableMediaTypes) {
            for (final ConsumesProducesInflector satisfiable : satisfyingInflectors) {
                if (satisfiable.produces.isCompatible(acceptableMediaType)) {
                    final MediaType effectiveResponseType = typeNotSpecific(satisfiable.produces) ? MediaTypes.mostSpecific(acceptableMediaType, satisfiable.produces) : satisfiable.produces;
                    injector.inject(RoutingContext.class).setEffectiveAcceptableType(effectiveResponseType);
                    final Inflector<Request, Response> inflector = satisfiable.inflector;
                    return Tuples.<Request, Iterator<TreeAcceptor>>of(request, Iterators.singletonIterator(Stages.asTreeAcceptor(new Inflector<Request, Response>() {

                        @Override
                        public Response apply(Request request) {
                            injector.inject(inflector);
                            return typeNotSpecific(effectiveResponseType) ? inflector.apply(request) : responseWithContentTypeHeader(effectiveResponseType, inflector.apply(request));
                        }
                    })));
                }
            }
        }
        throw new WebApplicationException(Response.status(Status.NOT_ACCEPTABLE).build());
    }

    private Response responseWithContentTypeHeader(final MediaType mt, final Response response) {
        return Responses.toBuilder(response).header(HttpHeaders.CONTENT_TYPE, mt).build();
    }

    private boolean typeNotSpecific(final MediaType effectiveResponseType) {
        return effectiveResponseType.isWildcardType() || effectiveResponseType.isWildcardSubtype();
    }

}
