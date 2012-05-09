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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import javax.annotation.Nullable;

import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.ResponseProcessor;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.routing.RouterModule.RoutingContext;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

/**
 * A single acceptor responsible for selecting a single method from all the methods
 * bound to the same routed request path.
 *
 * The method selection algorithm selects the handling method based on the HTTP request
 * method name, requested media type as well as defined resource method media type
 * capabilities.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class MethodSelectingAcceptor implements TreeAcceptor {

    private static final Logger LOGGER = Logger.getLogger(MethodSelectingAcceptor.class.getName());

    /**
     * Represents a 1-1-1 relation between input and output media type and an methodAcceptorPair.
     * <p>E.g. for a single resource method
     * <pre>
     *   &#064;Consumes("&#042;/&#042;")
     *   &#064;Produces("text/plain","text/html")
     *   &#064;GET
     *   public String myGetMethod() {
     *     return "S";
     *   }
     * </pre>
     * the following two relations would be generated:
     * <table>
     * <tr>
     * <th>consumes</th>
     * <th>produces</th>
     * <th>method</th>
     * </tr>
     * <tr>
     * <td>&#042;/&#042;</td>
     * <td>text/plain</td>
     * <td>myGetMethod</td>
     * </td>
     * </tr>
     * <tr>
     * <td>&#042;/&#042;</td>
     * <td>text/html</td>
     * <td>myGetMethod</td>
     * </td>
     * </tr>
     * </table>
     */
    private static class ConsumesProducesAcceptor {

        MediaType consumes;
        MediaType produces;
        MethodAcceptorPair methodAcceptorPair;

        ConsumesProducesAcceptor(
                MediaType consumes,
                MediaType produces,
                MethodAcceptorPair methodAcceptorPair) {

            this.methodAcceptorPair = methodAcceptorPair;
            this.consumes = consumes;
            this.produces = produces;
        }

        boolean isConsumable(Request request) {
            MediaType contentType = request.getHeaders().getMediaType();
            return contentType == null || consumes.isCompatible(contentType);
        }

        @Override
        public String toString() {
            return String.format("%s->%s:%s", consumes, produces, methodAcceptorPair);
        }
    }

    /**
     * The same as above ConsumesProducesAcceptor,
     * only concrete request content-type and accept header info is included in addition.
     *
     * @see org.glassfish.jersey.server.internal.routing.CombinedClientServerMediaType
     */
    private static class RequestSpecificConsumesProducesAcceptor implements Comparable {

        CombinedClientServerMediaType consumes;
        CombinedClientServerMediaType produces;
        MethodAcceptorPair methodAcceptorPair;

        RequestSpecificConsumesProducesAcceptor(
                CombinedClientServerMediaType consumes,
                CombinedClientServerMediaType produces,
                MethodAcceptorPair methodAcceptorPair) {

            this.methodAcceptorPair = methodAcceptorPair;
            this.consumes = consumes;
            this.produces = produces;
        }

        @Override
        public String toString() {
            return String.format("%s->%s:%s", consumes, produces, methodAcceptorPair);
        }

        @Override
        public int compareTo(Object o) {
            if (o == null) {
                return 1;
            }
            if (!(o instanceof RequestSpecificConsumesProducesAcceptor)) {
                return 1;
            }
            RequestSpecificConsumesProducesAcceptor other = (RequestSpecificConsumesProducesAcceptor) o;
            final int consumedComparison = CombinedClientServerMediaType.COMPARATOR.compare(consumes, other.consumes);
            return (consumedComparison != 0) ? consumedComparison : CombinedClientServerMediaType.COMPARATOR.compare(produces, other.produces);
        }
    }

    /**
     * Helper class to select matching resource method to be invoked.
     */
    private static class MethodSelector {

        RequestSpecificConsumesProducesAcceptor selected;
        List<RequestSpecificConsumesProducesAcceptor> sameFitnessAcceptors;

        MethodSelector(RequestSpecificConsumesProducesAcceptor i) {
            selected = i;
            sameFitnessAcceptors = null;
        }

        void consider(RequestSpecificConsumesProducesAcceptor i) {
            final int theGreaterTheBetter = i.compareTo(selected);
            if (theGreaterTheBetter > 0) {
                selected = i;
                sameFitnessAcceptors = null;
            } else {
                if (theGreaterTheBetter == 0 && (selected.methodAcceptorPair != i.methodAcceptorPair)) {
                    getSameFitnessList().add(i);
                }
            }
        }

        List<RequestSpecificConsumesProducesAcceptor> getSameFitnessList() {
            if (sameFitnessAcceptors == null) {
                sameFitnessAcceptors = new LinkedList<RequestSpecificConsumesProducesAcceptor>();
            }
            return sameFitnessAcceptors;
        }
    }

    /**
     * Injectable builder of a {@link MethodSelectingAcceptor} instance.
     */
    static class Builder {
        @Inject
        private Services services;
        @Inject
        private Injector injector;
        @Inject
        private Factory<ResponseProcessor.RespondingContext> respondingContextFactory;


        /**
         * Create a new method selecting acceptor for all the methods on the same path.
         *
         * The acceptor selects the method that best matches the request based on
         * produce/consume information from the resource method models.
         *
         * @param workers message body workers.
         * @param methodAcceptorPairs [method model, method methodAcceptorPair] pairs.
         */
        public MethodSelectingAcceptor build(
                final MessageBodyWorkers workers, final List<MethodAcceptorPair> methodAcceptorPairs) {
            return new MethodSelectingAcceptor(services, injector, workers, respondingContextFactory, methodAcceptorPairs);
        }

    }

    private final Services services;
    private final Injector injector;
    private final Factory<ResponseProcessor.RespondingContext> respondingContextFactory;
    private final MessageBodyWorkers workers;

    private final Map<String, List<ConsumesProducesAcceptor>> consumesProducesAcceptors;
    private final TreeAcceptor acceptor;

    private MethodSelectingAcceptor(
            Services services,
            Injector injector,
            MessageBodyWorkers msgWorkers,
            Factory<ResponseProcessor.RespondingContext> respondingContextFactory,
            List<MethodAcceptorPair> methodAcceptorPairs) {
        this.injector = injector;
        this.workers = msgWorkers;
        this.services = services;
        this.respondingContextFactory = respondingContextFactory;
        this.consumesProducesAcceptors = new HashMap<String, List<ConsumesProducesAcceptor>>();
        for (final MethodAcceptorPair methodAcceptorPair : methodAcceptorPairs) {
            String httpMethod = methodAcceptorPair.model.getHttpMethod();

            List<ConsumesProducesAcceptor> httpMethodBoundAcceptors = consumesProducesAcceptors.get(httpMethod);
            if (httpMethodBoundAcceptors == null) {
                httpMethodBoundAcceptors = new LinkedList<ConsumesProducesAcceptor>();
                consumesProducesAcceptors.put(httpMethod, httpMethodBoundAcceptors);
            }
            addAllConsumesProducesCombinations(httpMethodBoundAcceptors, methodAcceptorPair);
        }
        // todo HEAD & OPTIONS
        if (!consumesProducesAcceptors.containsKey(HttpMethod.HEAD)) {
            this.acceptor = createHeadEnrichedAcceptor();
        } else {
            this.acceptor = createInternalAcceptor();
        }
        if (!consumesProducesAcceptors.containsKey(HttpMethod.OPTIONS)) {
            addOptionsSupport();
        }
    }

    private TreeAcceptor createInternalAcceptor() {
        return new TreeAcceptor() {

            @Override
            public Pair<Request, Iterator<TreeAcceptor>> apply(Request request) {
                return Stages.singletonTreeContinuation(request, getMethodAcceptor(request));
            }
        };
    }

    @Override
    public Pair<Request, Iterator<TreeAcceptor>> apply(Request request) {
        return acceptor.apply(request);
    }

    private void addAllConsumesProducesCombinations(List<ConsumesProducesAcceptor> list,
                                                    MethodAcceptorPair methodAcceptorPair) {
        final List<MediaType> effectiveInputTypes = new LinkedList<MediaType>();
        final List<MediaType> effectiveOutputTypes = new LinkedList<MediaType>();
        ResourceMethod resourceMethod = methodAcceptorPair.model;
        effectiveInputTypes.addAll(resourceMethod.getConsumedTypes());
        if (effectiveInputTypes.isEmpty()) {
            if (workers != null) {
                final Invocable invocableMethod = resourceMethod.getInvocable();
                for (Parameter p : invocableMethod.getParameters()) {
                    if (p.getSource() == Parameter.Source.ENTITY) {
                        final GenericType<?> paramType = p.getParameterType();
                        effectiveInputTypes.addAll(workers.getMessageBodyReaderMediaTypes(
                                paramType.getRawType(), paramType.getType(), p.getDeclaredAnnotations()));
                    }
                }
            }
        }
        if (effectiveInputTypes.isEmpty()) {
            effectiveInputTypes.add(MediaType.valueOf("*/*"));
        }
        effectiveOutputTypes.addAll(resourceMethod.getProducedTypes());
        if (effectiveOutputTypes.isEmpty()) {
            if (workers != null) {
                final Invocable invocableMethod = resourceMethod.getInvocable();
                final GenericType<?> responseType = invocableMethod.getResponseType();
                effectiveOutputTypes.addAll(workers.getMessageBodyWriterMediaTypes(
                        responseType.getRawType(),
                        responseType.getType(),
                        invocableMethod.getHandlingMethod().getDeclaredAnnotations()));
            }
        }
        if (effectiveOutputTypes.isEmpty()) {
            effectiveOutputTypes.add(MediaType.valueOf("*/*"));
        }
        for (MediaType consumes : effectiveInputTypes) {
            for (MediaType produces : effectiveOutputTypes) {
                list.add(new ConsumesProducesAcceptor(consumes, produces, methodAcceptorPair));
            }
        }
    }

    private TreeAcceptor getMethodAcceptor(final Request request) {
        List<ConsumesProducesAcceptor> acceptors = consumesProducesAcceptors.get(request.getMethod());
        if (acceptors == null) {
            throw new WebApplicationException(
                    Response.status(Status.METHOD_NOT_ALLOWED).allow(consumesProducesAcceptors.keySet()).build());
        }
        List<ConsumesProducesAcceptor> satisfyingAcceptors = new LinkedList<ConsumesProducesAcceptor>();
        for (ConsumesProducesAcceptor cpi : acceptors) {
            if (cpi.isConsumable(request)) {
                satisfyingAcceptors.add(cpi);
            }
        }
        if (satisfyingAcceptors.isEmpty()) {
            throw new WebApplicationException(Status.UNSUPPORTED_MEDIA_TYPE);
        }

        // TODO: remove try/catch clauses based on JERSEY-913 resolution
        List<MediaType> acceptableMediaTypes;
        try {
            acceptableMediaTypes = request.getHeaders().getAcceptableMediaTypes();
        } catch (RuntimeException re) {
            throw new WebApplicationException(Response.status(400).entity(re.getMessage()).build());
        }

        final MethodSelector methodSelector = new MethodSelector(null);

        for (MediaType acceptableMediaType : acceptableMediaTypes) {
            for (final ConsumesProducesAcceptor satisfiable : satisfyingAcceptors) {
                if (satisfiable.produces.isCompatible(acceptableMediaType)) {

                    final MediaType requestContentType = request.getHeaders().getMediaType();
                    final MediaType effectiveContentType = requestContentType == null ? MediaType.WILDCARD_TYPE : requestContentType;

                    final RequestSpecificConsumesProducesAcceptor candidate = new RequestSpecificConsumesProducesAcceptor(
                            CombinedClientServerMediaType.create(effectiveContentType, satisfiable.consumes),
                            CombinedClientServerMediaType.create(acceptableMediaType, satisfiable.produces),
                            satisfiable.methodAcceptorPair);
                    methodSelector.consider(candidate);
                }
            }
        }

        if (methodSelector.selected != null) {
            final RequestSpecificConsumesProducesAcceptor selected = methodSelector.selected;

            if (methodSelector.sameFitnessAcceptors != null) {
                reportMethodSelectionAmbiguity(acceptableMediaTypes, selected, methodSelector.sameFitnessAcceptors);
            }

            final MediaType effectiveResponseType = selected.produces.combinedMediaType;
            injector.inject(RoutingContext.class).setEffectiveAcceptableType(effectiveResponseType);
            services.forContract(ResponseProcessor.RespondingContext.class).get().push(new Function<Response, Response>() {
                @Override
                public Response apply(final Response response) {
                    return typeNotSpecific(effectiveResponseType)
                            ? response : responseWithContentTypeHeader(effectiveResponseType, request, response);
                }
            });
            return selected.methodAcceptorPair.acceptor;
        }

        throw new WebApplicationException(Response.status(Status.NOT_ACCEPTABLE).build());
    }

    private Response responseWithContentTypeHeader(final MediaType mt, final Request request, final Response response) {
        final boolean contentTypeAlreadySet = response.getMetadata().containsKey(HttpHeaders.CONTENT_TYPE);
        return (!request.getMethod().equals(HttpMethod.HEAD) && (!response.hasEntity() || contentTypeAlreadySet))
                ? response : Responses.toBuilder(response).header(HttpHeaders.CONTENT_TYPE, mt).build();
    }

    private boolean typeNotSpecific(final MediaType effectiveResponseType) {
        return effectiveResponseType.isWildcardType() || effectiveResponseType.isWildcardSubtype();
    }

    private void reportMethodSelectionAmbiguity(List<MediaType> acceptableTypes,
                                                RequestSpecificConsumesProducesAcceptor selected,
                                                List<RequestSpecificConsumesProducesAcceptor> sameFitnessAcceptors) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            StringBuilder msgBuilder =
                    new StringBuilder(LocalizationMessages.AMBIGUOUS_RESOURCE_METHOD(acceptableTypes)).append('\n');
            msgBuilder.append('\t').append(selected.methodAcceptorPair.model).append('\n');
            final Set<ResourceMethod> reportedMethods = Sets.newHashSet();
            reportedMethods.add(selected.methodAcceptorPair.model);
            for (RequestSpecificConsumesProducesAcceptor i : sameFitnessAcceptors) {
                if (!reportedMethods.contains(i.methodAcceptorPair.model)) {
                    msgBuilder.append('\t').append(i.methodAcceptorPair.model).append('\n');
                }
                reportedMethods.add(i.methodAcceptorPair.model);
            }
            LOGGER.log(Level.WARNING, msgBuilder.toString());
        }
    }

    private void addOptionsSupport() {
        final Set<String> allowedMethods = new HashSet<String>(consumesProducesAcceptors.keySet());
        allowedMethods.add(HttpMethod.HEAD);
        allowedMethods.add(HttpMethod.OPTIONS);

        List<ConsumesProducesAcceptor> optionsAcceptors = new LinkedList<ConsumesProducesAcceptor>();
        optionsAcceptors.add(createPlainTextOptionsInflector(allowedMethods));
        optionsAcceptors.add(createGenericOptionsInflector(allowedMethods));
        consumesProducesAcceptors.put(HttpMethod.OPTIONS, optionsAcceptors);
    }

    private Response stripEntity(final Response originalResponse) {
        if (originalResponse.hasEntity()) {
            Response.ResponseBuilder result = Response.status(originalResponse.getStatus());
            Responses.fillHeaders(result, originalResponse.getHeaders().asMap());
            return result.build();
        } else {
            return originalResponse;
        }
    }

    private TreeAcceptor createHeadEnrichedAcceptor() {
        return new TreeAcceptor() {

            @Override
            public Pair<Request, Iterator<TreeAcceptor>> apply(final Request request) {
                if (HttpMethod.HEAD.equals(request.getMethod())) {
                    respondingContextFactory.get().push(new Function<Response, Response>() {
                        @Override
                        public Response apply(@Nullable Response response) {
                            return stripEntity(response);
                        }
                    });

                    final Request getRequest = Requests.from(request).method(HttpMethod.GET).build();
                    return Stages.singletonTreeContinuation(getRequest, getMethodAcceptor(getRequest));
                } else {
                    return Stages.singletonTreeContinuation(request, getMethodAcceptor(request));
                }
            }
        };
    }

    private ConsumesProducesAcceptor createPlainTextOptionsInflector(final Set<String> allowedMethods) {

        final String allowedList = allowedMethods.toString();
        final String optionsBody = allowedList.substring(1, allowedList.length() - 1);

        return new ConsumesProducesAcceptor(
                MediaType.WILDCARD_TYPE,
                MediaType.TEXT_PLAIN_TYPE,
                new MethodAcceptorPair(null, Stages.asTreeAcceptor(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        return Response.ok(optionsBody, MediaType.TEXT_PLAIN_TYPE)
                                .allow(allowedMethods)
                                .build();
                    }
                })));
    }

    private ConsumesProducesAcceptor createGenericOptionsInflector(final Set<String> allowedMethods) {

        return new ConsumesProducesAcceptor(
                MediaType.WILDCARD_TYPE,
                MediaType.WILDCARD_TYPE,
                new MethodAcceptorPair(null, Stages.asTreeAcceptor(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        return Response.ok()
                                .allow(allowedMethods)
                                .header(HttpHeaders.CONTENT_LENGTH, "0")
                                .type(data.getHeaders().getAcceptableMediaTypes().get(0))
                                .build();
                    }
                })));
    }
}
