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
package org.glassfish.jersey.server.internal.routing;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.ReaderModel;
import org.glassfish.jersey.message.WriterModel;
import org.glassfish.jersey.message.internal.AcceptableMediaType;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Sets;
import jersey.repackaged.com.google.common.primitives.Primitives;

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

    private static final Comparator<ConsumesProducesAcceptor> CONSUMES_PRODUCES_ACCEPTOR_COMPARATOR =
            new Comparator<ConsumesProducesAcceptor>() {

                @Override
                public int compare(final ConsumesProducesAcceptor o1, final ConsumesProducesAcceptor o2) {
                    // Make sure that annotated (@Consumes, @Produces) goes first.
                    final ResourceMethod model1 = o1.methodRouting.method;
                    final ResourceMethod model2 = o2.methodRouting.method;

                    // @Consumes on method.
                    int compared = compare(model1.getConsumedTypes(), model2.getConsumedTypes());
                    if (compared != 0) {
                        return compared;
                    }

                    compared = compare(model1.getProducedTypes(), model2.getProducedTypes());
                    if (compared != 0) {
                        return compared;
                    }

                    compared = MediaTypes.PARTIAL_ORDER_COMPARATOR.compare(o1.consumes.getMediaType(),
                            o2.consumes.getMediaType());
                    if (compared != 0) {
                        return compared;
                    }

                    return MediaTypes.PARTIAL_ORDER_COMPARATOR.compare(o1.produces.getMediaType(),
                            o2.produces.getMediaType());
                }

                private int compare(List<MediaType> mediaTypeList1, List<MediaType> mediaTypeList2) {
                    mediaTypeList1 = mediaTypeList1.isEmpty() ? MediaTypes.WILDCARD_TYPE_SINGLETON_LIST : mediaTypeList1;
                    mediaTypeList2 = mediaTypeList2.isEmpty() ? MediaTypes.WILDCARD_TYPE_SINGLETON_LIST : mediaTypeList2;

                    return MediaTypes.MEDIA_TYPE_LIST_COMPARATOR.compare(mediaTypeList1, mediaTypeList2);
                }
            };

    private final MessageBodyWorkers workers;

    private final Map<String, List<ConsumesProducesAcceptor>> consumesProducesAcceptors;
    private final Router router;

    /**
     * Create a new {@code MethodSelectingRouter} for all the methods on the same path.
     *
     * The router selects the method that best matches the request based on
     * produce/consume information from the resource method models.
     *
     * @param workers        message body workers.
     * @param methodRoutings [method model, method methodAcceptorPair] pairs.
     */
    MethodSelectingRouter(MessageBodyWorkers workers, List<MethodRouting> methodRoutings) {
        this.workers = workers;

        this.consumesProducesAcceptors = new HashMap<>();

        final Set<String> httpMethods = Sets.newHashSet();
        for (final MethodRouting methodRouting : methodRoutings) {
            final String httpMethod = methodRouting.method.getHttpMethod();
            httpMethods.add(httpMethod);

            List<ConsumesProducesAcceptor> httpMethodBoundAcceptors = consumesProducesAcceptors.get(httpMethod);
            if (httpMethodBoundAcceptors == null) {
                httpMethodBoundAcceptors = new LinkedList<>();
                consumesProducesAcceptors.put(httpMethod, httpMethodBoundAcceptors);
            }

            addAllConsumesProducesCombinations(httpMethodBoundAcceptors, methodRouting);
        }

        // Sort acceptors for added HTTP methods - primary based on @Consumes, @Produces present on method, secondary on consumes,
        // produces values of the acceptor.
        for (final String httpMethod : httpMethods) {
            Collections.sort(consumesProducesAcceptors.get(httpMethod), CONSUMES_PRODUCES_ACCEPTOR_COMPARATOR);
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

        final CombinedMediaType.EffectiveMediaType consumes;
        final CombinedMediaType.EffectiveMediaType produces;
        final MethodRouting methodRouting;

        private ConsumesProducesAcceptor(
                CombinedMediaType.EffectiveMediaType consumes,
                CombinedMediaType.EffectiveMediaType produces,
                MethodRouting methodRouting) {
            this.methodRouting = methodRouting;
            this.consumes = consumes;
            this.produces = produces;
        }

        /**
         * Determines whether this {@code ConsumesProducesAcceptor} router can process the {@code request}.
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
            return String.format("%s->%s:%s", consumes.getMediaType(), produces.getMediaType(), methodRouting);
        }

        @Override
        @SuppressWarnings("RedundantIfStatement")
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConsumesProducesAcceptor)) {
                return false;
            }

            final ConsumesProducesAcceptor that = (ConsumesProducesAcceptor) o;

            if (consumes != null ? !consumes.equals(that.consumes) : that.consumes != null) {
                return false;
            }
            if (methodRouting != null ? !methodRouting.equals(that.methodRouting) : that.methodRouting != null) {
                return false;
            }
            if (produces != null ? !produces.equals(that.produces) : that.produces != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = consumes != null ? consumes.hashCode() : 0;
            result = 31 * result + (produces != null ? produces.hashCode() : 0);
            result = 31 * result + (methodRouting != null ? methodRouting.hashCode() : 0);
            return result;
        }
    }

    /**
     * The same as above ConsumesProducesAcceptor,
     * only concrete request content-type and accept header info is included in addition.
     *
     * @see CombinedMediaType
     */
    @SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
    private static final class RequestSpecificConsumesProducesAcceptor implements Comparable {

        final CombinedMediaType consumes;
        final CombinedMediaType produces;
        final MethodRouting methodRouting;

        final boolean producesFromProviders;

        RequestSpecificConsumesProducesAcceptor(final CombinedMediaType consumes,
                                                final CombinedMediaType produces,
                                                final boolean producesFromProviders,
                                                final MethodRouting methodRouting) {

            this.methodRouting = methodRouting;
            this.consumes = consumes;
            this.produces = produces;

            this.producesFromProviders = producesFromProviders;
        }

        @Override
        public String toString() {
            return String.format("%s->%s:%s", consumes, produces, methodRouting);
        }

        @Override
        public int compareTo(Object o) {
            if (o == null) {
                return -1;
            }
            if (!(o instanceof RequestSpecificConsumesProducesAcceptor)) {
                return -1;
            }
            RequestSpecificConsumesProducesAcceptor other = (RequestSpecificConsumesProducesAcceptor) o;
            final int consumedComparison = CombinedMediaType.COMPARATOR.compare(consumes, other.consumes);
            return (consumedComparison != 0)
                    ? consumedComparison : CombinedMediaType.COMPARATOR.compare(produces, other.produces);
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
            final int theLessTheBetter = i.compareTo(selected);
            if (theLessTheBetter < 0) {
                selected = i;
                sameFitnessAcceptors = null;
            } else {
                if (theLessTheBetter == 0 && (selected.methodRouting != i.methodRouting)) {
                    getSameFitnessList().add(i);
                }
            }
        }

        List<RequestSpecificConsumesProducesAcceptor> getSameFitnessList() {
            if (sameFitnessAcceptors == null) {
                sameFitnessAcceptors = new LinkedList<>();
            }
            return sameFitnessAcceptors;
        }
    }

    private Router createInternalRouter() {
        return new Router() {

            @Override
            public Continuation apply(RequestProcessingContext requestContext) {
                return Continuation.of(requestContext, getMethodRouter(requestContext));
            }
        };
    }

    @Override
    public Continuation apply(RequestProcessingContext requestContext) {
        return router.apply(requestContext);
    }

    private void addAllConsumesProducesCombinations(final List<ConsumesProducesAcceptor> acceptors,
                                                    final MethodRouting methodRouting) {
        final ResourceMethod resourceMethod = methodRouting.method;

        final Set<MediaType> effectiveInputTypes = new LinkedHashSet<>();
        boolean consumesFromWorkers = fillMediaTypes(effectiveInputTypes, resourceMethod,
                resourceMethod.getConsumedTypes(), true);

        final Set<MediaType> effectiveOutputTypes = new LinkedHashSet<>();
        boolean producesFromWorkers = fillMediaTypes(effectiveOutputTypes, resourceMethod,
                resourceMethod.getProducedTypes(), false);

        final Set<ConsumesProducesAcceptor> acceptorSet = Sets.newHashSet();
        for (MediaType consumes : effectiveInputTypes) {
            for (MediaType produces : effectiveOutputTypes) {

                acceptorSet.add(new ConsumesProducesAcceptor(
                        new CombinedMediaType.EffectiveMediaType(consumes, consumesFromWorkers),
                        new CombinedMediaType.EffectiveMediaType(produces, producesFromWorkers),
                        methodRouting));
            }
        }
        acceptors.addAll(acceptorSet);
    }

    private boolean fillMediaTypes(final Set<MediaType> effectiveTypes,
                                   final ResourceMethod resourceMethod,
                                   final List<MediaType> methodTypes,
                                   final boolean inputTypes) {

        // Add method types to the resulting list iff there is more than just */*
        if (methodTypes.size() > 1 || !methodTypes.contains(MediaType.WILDCARD_TYPE)) {
            effectiveTypes.addAll(methodTypes);
        }

        boolean mediaTypesFromWorkers = effectiveTypes.isEmpty();
        if (mediaTypesFromWorkers) {
            final Invocable invocableMethod = resourceMethod.getInvocable();

            // If not predefined from method - get it from workers.
            if (inputTypes) {
                fillInputTypesFromWorkers(effectiveTypes, invocableMethod);
            } else {
                fillOutputTypesFromWorkers(effectiveTypes, invocableMethod.getRawResponseType());
            }
            mediaTypesFromWorkers = !effectiveTypes.isEmpty();

            // If still empty - get all available.
            if (!mediaTypesFromWorkers) {
                if (inputTypes) {
                    effectiveTypes.addAll(workers.getMessageBodyReaderMediaTypesByType(Object.class));
                } else {
                    effectiveTypes.addAll(workers.getMessageBodyWriterMediaTypesByType(Object.class));
                }
                mediaTypesFromWorkers = true;
            }
        }

        return mediaTypesFromWorkers;
    }

    private void fillOutputTypesFromWorkers(final Set<MediaType> effectiveOutputTypes, final Class<?> returnEntityType) {
        effectiveOutputTypes.addAll(workers.getMessageBodyWriterMediaTypesByType(returnEntityType));
    }

    private void fillInputTypesFromWorkers(final Set<MediaType> effectiveInputTypes, final Invocable invocableMethod) {
        for (Parameter p : invocableMethod.getParameters()) {
            if (p.getSource() == Parameter.Source.ENTITY) {
                effectiveInputTypes.addAll(workers.getMessageBodyReaderMediaTypesByType(p.getRawType()));

                // there's at most one entity parameter
                break;
            }
        }
    }

    private Parameter getEntityParam(final Invocable invocable) {
        for (final Parameter parameter : invocable.getParameters()) {
            if (parameter.getSource() == Parameter.Source.ENTITY
                    && !ContainerRequestContext.class.isAssignableFrom(parameter.getRawType())) {
                // there's at most one entity parameter
                return parameter;
            }
        }
        return null;
    }

    private List<Router> getMethodRouter(final RequestProcessingContext context) {
        final ContainerRequest request = context.request();
        final List<ConsumesProducesAcceptor> acceptors = consumesProducesAcceptors.get(request.getMethod());
        if (acceptors == null) {
            throw new NotAllowedException(
                    Response.status(Status.METHOD_NOT_ALLOWED).allow(consumesProducesAcceptors.keySet()).build());
        }

        final List<ConsumesProducesAcceptor> satisfyingAcceptors = new LinkedList<>();
        final Set<ResourceMethod> differentInvokableMethods = Sets.newIdentityHashSet();
        for (ConsumesProducesAcceptor cpi : acceptors) {
            if (cpi.isConsumable(request)) {
                satisfyingAcceptors.add(cpi);
                differentInvokableMethods.add(cpi.methodRouting.method);
            }
        }
        if (satisfyingAcceptors.isEmpty()) {
            throw new NotSupportedException();
        }

        final List<AcceptableMediaType> acceptableMediaTypes = request.getQualifiedAcceptableMediaTypes();

        final MediaType requestContentType = request.getMediaType();
        final MediaType effectiveContentType = requestContentType == null ? MediaType.WILDCARD_TYPE : requestContentType;

        final MethodSelector methodSelector = selectMethod(acceptableMediaTypes, satisfyingAcceptors, effectiveContentType,
                differentInvokableMethods.size() == 1);

        if (methodSelector.selected != null) {
            final RequestSpecificConsumesProducesAcceptor selected = methodSelector.selected;

            if (methodSelector.sameFitnessAcceptors != null) {
                reportMethodSelectionAmbiguity(acceptableMediaTypes, methodSelector.selected,
                        methodSelector.sameFitnessAcceptors);
            }

            context.push(new Function<ContainerResponse, ContainerResponse>() {
                @Override
                public ContainerResponse apply(final ContainerResponse responseContext) {
                    // we only need to compute and set the effective media type if:
                    // - it hasn't been set already, and
                    // - either there is an entity, or we are responding to a HEAD request
                    if (responseContext.getMediaType() == null
                            && ((responseContext.hasEntity() || HttpMethod.HEAD.equals(request.getMethod())))) {

                        MediaType effectiveResponseType = determineResponseMediaType(
                                responseContext.getEntityClass(),
                                responseContext.getEntityType(),
                                methodSelector.selected,
                                acceptableMediaTypes);

                        if (MediaTypes.isWildcard(effectiveResponseType)) {
                            if (effectiveResponseType.isWildcardType()
                                    || "application".equalsIgnoreCase(effectiveResponseType.getType())) {
                                effectiveResponseType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
                            } else {
                                throw new NotAcceptableException();
                            }
                        }
                        responseContext.setMediaType(effectiveResponseType);
                    }

                    return responseContext;
                }
            });
            return selected.methodRouting.routers;
        }

        throw new NotAcceptableException();
    }

    /**
     * Determine the {@link MediaType} of the {@link Response} based on writers suitable for the given entity class,
     * pre-selected method and acceptable media types.
     *
     * @param entityClass          entity class to determine the media type for.
     * @param entityType           entity type for writers.
     * @param selectedMethod       pre-selected (invoked) method.
     * @param acceptableMediaTypes acceptable media types from request.
     * @return media type of the response.
     */
    private MediaType determineResponseMediaType(
            final Class<?> entityClass,
            final Type entityType,
            final RequestSpecificConsumesProducesAcceptor selectedMethod,
            final List<AcceptableMediaType> acceptableMediaTypes) {

        // Return pre-selected MediaType.
        if (usePreSelectedMediaType(selectedMethod, acceptableMediaTypes)) {
            return selectedMethod.produces.combinedType;
        }

        final ResourceMethod resourceMethod = selectedMethod.methodRouting.method;
        final Invocable invocable = resourceMethod.getInvocable();

        // Entity class can be null when considering HEAD method || empty entity.
        final Class<?> responseEntityClass = entityClass == null ? invocable.getRawRoutingResponseType() : entityClass;
        final Method handlingMethod = invocable.getHandlingMethod();

        // Media types producible by method.
        final List<MediaType> methodProducesTypes = !resourceMethod.getProducedTypes().isEmpty()
                ? resourceMethod.getProducedTypes() : Lists.newArrayList(MediaType.WILDCARD_TYPE);
        // Applicable entity providers
        final List<WriterModel> writersForEntityType = workers.getWritersModelsForType(responseEntityClass);

        CombinedMediaType selected = null;
        for (final MediaType acceptableMediaType : acceptableMediaTypes) {
            for (final MediaType methodProducesType : methodProducesTypes) {
                if (!acceptableMediaType.isCompatible(methodProducesType)) {
                    // no need to go deeper if acceptable and method produces type are incompatible
                    continue;
                }

                // Use writers suitable for entity class to determine the media type.
                for (final WriterModel model : writersForEntityType) {
                    for (final MediaType writerProduces : model.declaredTypes()) {
                        if (!writerProduces.isCompatible(acceptableMediaType)
                                || !methodProducesType.isCompatible(writerProduces)) {
                            continue;
                        }

                        final CombinedMediaType.EffectiveMediaType effectiveProduces =
                                new CombinedMediaType.EffectiveMediaType(
                                        MediaTypes.mostSpecific(methodProducesType, writerProduces),
                                        false);

                        final CombinedMediaType candidate =
                                CombinedMediaType.create(acceptableMediaType, effectiveProduces);

                        if (candidate != CombinedMediaType.NO_MATCH) {
                            // Look for a better compatible worker.
                            if (selected == null || CombinedMediaType.COMPARATOR.compare(candidate, selected) < 0) {
                                if (model.isWriteable(
                                        responseEntityClass,
                                        entityType,
                                        handlingMethod.getDeclaredAnnotations(),
                                        candidate.combinedType)) {
                                    selected = candidate;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Found media type for current writer.
        if (selected != null) {
            return selected.combinedType;
        }

        // If the media type couldn't be determined, choose pre-selected one and wait whether interceptors change the mediaType
        // so it can be written.
        return selectedMethod.produces.combinedType;
    }

    private static boolean usePreSelectedMediaType(final RequestSpecificConsumesProducesAcceptor selectedMethod,
                                                   final List<AcceptableMediaType> acceptableMediaTypes) {
        // Resource method is annotated with @Produces and this annotation contains only one MediaType.
        if (!selectedMethod.producesFromProviders
                && selectedMethod.methodRouting.method.getProducedTypes().size() == 1) {
            return true;
        }

        // There is only one (non-wildcard) acceptable media type - at this point the pre-selected method has to be chosen so
        // there are compatible writers (not necessarily writeable ones).
        return acceptableMediaTypes.size() == 1 && !MediaTypes.isWildcard(acceptableMediaTypes.get(0));
    }

    private boolean isWriteable(final RequestSpecificConsumesProducesAcceptor candidate) {
        final Invocable invocable = candidate.methodRouting.method.getInvocable();
        final Class<?> responseType = Primitives.wrap(invocable.getRawRoutingResponseType());

        if (Response.class.isAssignableFrom(responseType)
                || Void.class.isAssignableFrom(responseType)) {
            return true;
        }

        final Type genericType = invocable.getRoutingResponseType();

        final Type genericReturnType = genericType instanceof GenericType
                ? ((GenericType) genericType).getType() : genericType;

        for (final WriterModel model : workers.getWritersModelsForType(responseType)) {
            if (model.isWriteable(
                    responseType,
                    genericReturnType,
                    invocable.getHandlingMethod().getDeclaredAnnotations(),
                    candidate.produces.combinedType)) {
                return true;
            }
        }

        return false;
    }

    private boolean isReadable(final RequestSpecificConsumesProducesAcceptor candidate) {
        final Invocable invocable = candidate.methodRouting.method.getInvocable();
        final Method handlingMethod = invocable.getHandlingMethod();
        final Parameter entityParam = getEntityParam(invocable);

        if (entityParam == null) {
            return true;
        } else {
            final Class<?> entityType = entityParam.getRawType();

            for (final ReaderModel model : workers.getReaderModelsForType(entityType)) {
                if (model.isReadable(
                        entityType,
                        entityParam.getType(),
                        handlingMethod.getDeclaredAnnotations(),
                        candidate.consumes.combinedType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Select method to be invoked. Method is chosen among the given set of acceptors (if they are compatible with acceptable
     * media types).
     *
     * @param acceptableMediaTypes  media types acceptable by the client.
     * @param satisfyingAcceptors   pre-computed acceptors.
     * @param effectiveContentType  media type of incoming entity.
     * @param singleInvokableMethod flag determining whether only one method to be invoked has been found among satisfying
     *                              acceptors.
     * @return method to be invoked.
     */
    private MethodSelector selectMethod(final List<AcceptableMediaType> acceptableMediaTypes,
                                        final List<ConsumesProducesAcceptor> satisfyingAcceptors,
                                        final MediaType effectiveContentType,
                                        final boolean singleInvokableMethod) {

        // Selected method we have a reader and writer for.
        final MethodSelector method = new MethodSelector(null);
        // If we cannot find a writer at this point use the best alternative.
        final MethodSelector alternative = new MethodSelector(null);

        for (final MediaType acceptableMediaType : acceptableMediaTypes) {
            for (final ConsumesProducesAcceptor satisfiable : satisfyingAcceptors) {

                final CombinedMediaType produces =
                        CombinedMediaType.create(acceptableMediaType, satisfiable.produces);

                if (produces != CombinedMediaType.NO_MATCH) {
                    final CombinedMediaType consumes =
                            CombinedMediaType.create(effectiveContentType, satisfiable.consumes);
                    final RequestSpecificConsumesProducesAcceptor candidate = new RequestSpecificConsumesProducesAcceptor(
                            consumes,
                            produces,
                            satisfiable.produces.isDerived(),
                            satisfiable.methodRouting);

                    if (singleInvokableMethod) {
                        // Only one possible method and it's compatible.
                        return new MethodSelector(candidate);
                    } else if (candidate.compareTo(method.selected) < 0) {
                        // Candidate is better than the previous one.
                        if (method.selected == null
                                || candidate.methodRouting.method != method.selected.methodRouting.method) {
                            // No candidate so far or better candidate.
                            if (isReadable(candidate) && isWriteable(candidate)) {
                                method.consider(candidate);
                            } else {
                                alternative.consider(candidate);
                            }
                        } else {
                            // Same resource method - better candidate, no need to compare anything else.
                            method.consider(candidate);
                        }
                    }
                }
            }
        }

        return method.selected != null ? method : alternative;
    }

    private void reportMethodSelectionAmbiguity(List<AcceptableMediaType> acceptableTypes,
                                                RequestSpecificConsumesProducesAcceptor selected,
                                                List<RequestSpecificConsumesProducesAcceptor> sameFitnessAcceptors) {
        if (LOGGER.isLoggable(Level.WARNING)) {
            StringBuilder msgBuilder =
                    new StringBuilder(LocalizationMessages.AMBIGUOUS_RESOURCE_METHOD(acceptableTypes)).append('\n');
            msgBuilder.append('\t').append(selected.methodRouting.method).append('\n');
            final Set<ResourceMethod> reportedMethods = Sets.newHashSet();
            reportedMethods.add(selected.methodRouting.method);
            for (RequestSpecificConsumesProducesAcceptor i : sameFitnessAcceptors) {
                if (!reportedMethods.contains(i.methodRouting.method)) {
                    msgBuilder.append('\t').append(i.methodRouting.method).append('\n');
                }
                reportedMethods.add(i.methodRouting.method);
            }
            LOGGER.log(Level.WARNING, msgBuilder.toString());
        }
    }

    private Router createHeadEnrichedRouter() {
        return new Router() {

            @Override
            public Continuation apply(final RequestProcessingContext context) {
                final ContainerRequest request = context.request();
                if (HttpMethod.HEAD.equals(request.getMethod())) {
                    request.setMethodWithoutException(HttpMethod.GET);
                    context.push(
                            new Function<ContainerResponse, ContainerResponse>() {
                                @Override
                                public ContainerResponse apply(final ContainerResponse responseContext) {
                                    responseContext.getRequestContext().setMethodWithoutException(HttpMethod.HEAD);
                                    return responseContext;
                                }
                            }
                    );
                }
                return Continuation.of(context, getMethodRouter(context));
            }
        };
    }
}
