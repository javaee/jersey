/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.process.RespondingContext;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

/**
 * A single router responsible for selecting a single method from all the methods
 * bound to the same routed request path.
 *
 * The method selection algorithm selects the handling method based on the HTTP request
 * method name, requested media type as well as defined resource method media type
 * capabilities.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
final class MethodSelectingRouter implements Router {

    private static final Logger LOGGER = Logger.getLogger(MethodSelectingRouter.class.getName());

    private final Provider<RespondingContext> respondingContextFactory;
    private final MessageBodyWorkers workers;

    private final Map<String, List<ConsumesProducesAcceptor>> consumesProducesAcceptors;
    private final Router router;

    /**
     * Injectable builder of a {@link MethodSelectingRouter} instance.
     */
    static class Builder {
        @Inject
        private Provider<RespondingContext> respondingContextFactory;

        /**
         * Create a new {@link MethodSelectingRouter} for all the methods on the same path.
         *
         * The router selects the method that best matches the request based on
         * produce/consume information from the resource method models.
         *
         * @param workers             message body workers.
         * @param methodAcceptorPairs [method model, method methodAcceptorPair] pairs.
         * @return new {@link MethodSelectingRouter}
         */
        public MethodSelectingRouter build(
                final MessageBodyWorkers workers, final List<MethodAcceptorPair> methodAcceptorPairs) {

            return new MethodSelectingRouter(respondingContextFactory,
                    workers,
                    methodAcceptorPairs);
        }
    }

    private MethodSelectingRouter(
            Provider<RespondingContext> respondingContextFactory,
            MessageBodyWorkers msgWorkers,
            List<MethodAcceptorPair> methodAcceptorPairs) {
        this.respondingContextFactory = respondingContextFactory;
        this.workers = msgWorkers;

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

        if (!consumesProducesAcceptors.containsKey(HttpMethod.HEAD)) {
            this.router = createHeadEnrichedRouter();
        } else {
            this.router = createInternalRouter();
        }
    }

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

        private CombinedClientServerMediaType.EffectiveMediaType consumes;
        private CombinedClientServerMediaType.EffectiveMediaType produces;
        private MethodAcceptorPair methodAcceptorPair;

        private ConsumesProducesAcceptor(
                CombinedClientServerMediaType.EffectiveMediaType consumes,
                CombinedClientServerMediaType.EffectiveMediaType produces,
                MethodAcceptorPair methodAcceptorPair) {
            this.methodAcceptorPair = methodAcceptorPair;
            this.consumes = consumes;
            this.produces = produces;
        }

        /**
         * Returns the {@link CombinedClientServerMediaType.EffectiveMediaType extended media type} which can be
         * consumed by {@link ResourceMethod resource method} of this {@link ConsumesProducesAcceptor router}.
         *
         * @return Consumed type.
         */
        public CombinedClientServerMediaType.EffectiveMediaType getConsumes() {
            return consumes;
        }

        /**
         * Returns the {@link CombinedClientServerMediaType.EffectiveMediaType extended media type} which can be
         * produced by {@link ResourceMethod resource method} of this {@link ConsumesProducesAcceptor router}.
         *
         * @return Produced type.
         */
        public CombinedClientServerMediaType.EffectiveMediaType getProduces() {
            return produces;
        }


        /**
         * Determines whether this {@link ConsumesProducesAcceptor router} can process the {@code request}.
         *
         * @param requestContext The request to be tested.
         * @return True if the {@code request} can be processed by this router, false otherwise.
         */
        boolean isConsumable(ContainerRequest requestContext) {
            MediaType contentType = requestContext.getMediaType();
            return contentType == null || consumes.getMediaType().isCompatible(contentType);
        }

        @Override
        public String toString() {
            return String.format("%s->%s:%s", consumes.getMediaType(), produces.getMediaType(), methodAcceptorPair);
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

        RequestSpecificConsumesProducesAcceptor(CombinedClientServerMediaType consumes, CombinedClientServerMediaType produces,
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
            return (consumedComparison != 0) ? consumedComparison : CombinedClientServerMediaType.COMPARATOR.compare(produces,
                    other.produces);
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


    private Router createInternalRouter() {
        return new Router() {

            @Override
            public Continuation apply(ContainerRequest requestContext) {
                return Continuation.of(requestContext, getMethodRouter(requestContext));
            }
        };
    }

    @Override
    public Continuation apply(ContainerRequest requestContext) {
        return router.apply(requestContext);
    }

    private void addAllConsumesProducesCombinations(List<ConsumesProducesAcceptor> list,
                                                    MethodAcceptorPair methodAcceptorPair) {
        final List<MediaType> effectiveInputTypes = new LinkedList<MediaType>();
        ResourceMethod resourceMethod = methodAcceptorPair.model;

        boolean consumesFromWorkers = fillMediaTypes(effectiveInputTypes, resourceMethod, resourceMethod.getConsumedTypes(),
                true);
        final List<MediaType> effectiveOutputTypes = new LinkedList<MediaType>();
        boolean producesFromWorkers = fillMediaTypes(effectiveOutputTypes, resourceMethod, resourceMethod.getProducedTypes(),
                false);

        for (MediaType consumes : effectiveInputTypes) {
            for (MediaType produces : effectiveOutputTypes) {
                list.add(new ConsumesProducesAcceptor(new CombinedClientServerMediaType.EffectiveMediaType(consumes,
                        consumesFromWorkers),
                        new CombinedClientServerMediaType.EffectiveMediaType(produces, producesFromWorkers), methodAcceptorPair));
            }
        }
    }

    private boolean fillMediaTypes(List<MediaType> effectiveTypes, ResourceMethod resourceMethod, List<MediaType> methodTypes,
                                   boolean inputTypes) {
        boolean consumesFromWorkers = false;
        effectiveTypes.addAll(methodTypes);
        if (effectiveTypes.isEmpty()) {
            if (workers != null) {
                final Invocable invocableMethod = resourceMethod.getInvocable();
                if (inputTypes) {
                    fillInputTypesFromWorkers(effectiveTypes, invocableMethod);
                } else {
                    fillOutputParameters(effectiveTypes, invocableMethod);
                }
                consumesFromWorkers = !effectiveTypes.isEmpty();
            }
        }
        if (effectiveTypes.isEmpty()) {
            effectiveTypes.add(MediaType.valueOf("*/*"));
        }
        return consumesFromWorkers;
    }

    private void fillOutputParameters(List<MediaType> effectiveOutputTypes, Invocable invocableMethod) {
        final List<MediaType> messageBodyWriterMediaTypes = workers.getMessageBodyWriterMediaTypes(
                invocableMethod.getRawResponseType(),
                invocableMethod.getResponseType(),
                invocableMethod.getHandlingMethod().getDeclaredAnnotations());
        effectiveOutputTypes.addAll(messageBodyWriterMediaTypes);
    }

    private void fillInputTypesFromWorkers(List<MediaType> effectiveInputTypes, Invocable invocableMethod) {
        for (Parameter p : invocableMethod.getParameters()) {
            if (p.getSource() == Parameter.Source.ENTITY) {
                final List<MediaType> messageBodyReaderMediaTypes = workers.getMessageBodyReaderMediaTypes(
                        p.getRawType(), p.getType(), p.getDeclaredAnnotations());
                effectiveInputTypes.addAll(messageBodyReaderMediaTypes);
            }
        }
    }

    private List<Router> getMethodRouter(final ContainerRequest requestContext) {
        List<ConsumesProducesAcceptor> acceptors = consumesProducesAcceptors.get(requestContext.getMethod());
        if (acceptors == null) {
            throw new WebApplicationException(
                    Response.status(Status.METHOD_NOT_ALLOWED).allow(consumesProducesAcceptors.keySet()).build());
        }
        List<ConsumesProducesAcceptor> satisfyingAcceptors = new LinkedList<ConsumesProducesAcceptor>();
        for (ConsumesProducesAcceptor cpi : acceptors) {
            if (cpi.isConsumable(requestContext)) {
                satisfyingAcceptors.add(cpi);
            }
        }
        if (satisfyingAcceptors.isEmpty()) {
            throw new WebApplicationException(Status.UNSUPPORTED_MEDIA_TYPE);
        }

        final List<MediaType> acceptableMediaTypes = requestContext.getAcceptableMediaTypes();
        final MethodSelector methodSelector = new MethodSelector(null);

        for (MediaType acceptableMediaType : acceptableMediaTypes) {
            for (final ConsumesProducesAcceptor satisfiable : satisfyingAcceptors) {
                if (satisfiable.produces.getMediaType().isCompatible(acceptableMediaType)) {

                    final MediaType requestContentType = requestContext.getMediaType();
                    final MediaType effectiveContentType = requestContentType == null ? MediaType.WILDCARD_TYPE :
                            requestContentType;

                    final RequestSpecificConsumesProducesAcceptor candidate = new RequestSpecificConsumesProducesAcceptor(
                            CombinedClientServerMediaType.create(effectiveContentType, satisfiable.getConsumes()),
                            CombinedClientServerMediaType.create(acceptableMediaType, satisfiable.getProduces()),
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

            respondingContextFactory.get().push(
                    new Function<ContainerResponse, ContainerResponse>() {
                        @Override
                        public ContainerResponse apply(final ContainerResponse responseContext) {
                            // we only need to compute and set the effective media type if it hasn't been set already
                            // and either there is an entity, or we are responding to a HEAD request
                            if (responseContext.getMediaType() == null &&
                                    (responseContext.hasEntity() ||
                                            HttpMethod.HEAD.equals(responseContext.getRequestContext().getMethod()))) {
                                MediaType effectiveResponseType = selected.produces.getCombinedMediaType();
                                if (isWildcard(effectiveResponseType)) {
                                    if (effectiveResponseType.isWildcardType() || effectiveResponseType.getType()
                                            .equalsIgnoreCase("application")) {
                                        effectiveResponseType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
                                    } else {
                                        throw new WebApplicationException(Response.status(Status.NOT_ACCEPTABLE).build());
                                    }
                                }
                                responseContext.setMediaType(effectiveResponseType);
                            }
                            return responseContext;
                        }
                    });
            return selected.methodAcceptorPair.router;
        }

        throw new WebApplicationException(Response.status(Status.NOT_ACCEPTABLE).build());
    }

    private boolean isWildcard(final MediaType effectiveResponseType) {
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

    private Router createHeadEnrichedRouter() {
        return new Router() {

            @Override
            public Continuation apply(final ContainerRequest requestContext) {
                if (HttpMethod.HEAD.equals(requestContext.getMethod())) {
                    requestContext.setMethodWithoutException(HttpMethod.GET);
                    respondingContextFactory.get().push(
                            new Function<ContainerResponse, ContainerResponse>() {
                                @Override
                                public ContainerResponse apply(ContainerResponse responseContext) {
                                    responseContext.getRequestContext().setMethodWithoutException(HttpMethod.HEAD);
                                    return responseContext;
                                }
                            }
                    );
                }
                return Continuation.of(requestContext, getMethodRouter(requestContext));
            }
        };
    }
}
