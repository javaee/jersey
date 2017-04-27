/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.function.Function;

import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.internal.ChainableStage;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.RuntimeResourceModel;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

/**
 * Jersey routing entry point.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class Routing {
    private Routing() {
        throw new AssertionError("No instances allowed.");
    }

    /**
     * Create a new request pre-processing stage that extracts a matched endpoint from a routing context,
     * where it was previously stored by the request routing stage and
     * (if available) returns the endpoint wrapped in a next terminal stage.
     *
     * This request pre-processing stage should be a final stage in the request processing chain.
     *
     * @return new matched endpoint extractor request pre-processing stage.
     */
    public static Stage<RequestProcessingContext> matchedEndpointExtractor() {
        return new MatchedEndpointExtractorStage();
    }

    /**
     * Create a routing stage builder for a given runtime resource model.
     *
     * @param resourceModel runtime resource model
     * @return new routing stage builder.
     */
    public static Builder forModel(RuntimeResourceModel resourceModel) {
        return new Builder(resourceModel);

    }

    /**
     * Resource routing builder.
     */
    public static final class Builder {

        private final RuntimeResourceModel resourceModel;

        private JerseyResourceContext resourceContext;
        private Configuration config;
        private MessageBodyWorkers entityProviders;
        private Collection<ValueParamProvider> valueSuppliers;
        private Iterable<ModelProcessor> modelProcessors;
        private Function<Class<?>, ?> createServiceFunction;
        private ProcessingProviders processingProviders;
        private ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;

        private Builder(RuntimeResourceModel resourceModel) {
            if (resourceModel == null) {
                // No L10N - internally used class
                throw new NullPointerException("Resource model must not be null.");
            }
            this.resourceModel = resourceModel;
        }

        /**
         * Set resource context.
         *
         * @param resourceContext resource context.
         * @return updated routing builder.
         */
        public Builder resourceContext(JerseyResourceContext resourceContext) {
            this.resourceContext = resourceContext;
            return this;
        }

        /**
         * Set runtime configuration.
         *
         * @param config runtime configuration.
         * @return updated routing builder.
         */
        public Builder configuration(Configuration config) {
            this.config = config;
            return this;
        }

        /**
         * Set entity providers.
         *
         * @param workers entity providers.
         * @return updated routing builder.
         */
        public Builder entityProviders(MessageBodyWorkers workers) {
            this.entityProviders = workers;
            return this;
        }

        /**
         * Set value suppliers.
         *
         * @param valueSuppliers all registered value suppliers.
         * @return updated routing builder.
         */
        public Builder valueSupplierProviders(Collection<ValueParamProvider> valueSuppliers) {
            this.valueSuppliers = valueSuppliers;
            return this;
        }

        /**
         * Set request/response processing providers.
         *
         * @param processingProviders request/response processing providers.
         * @return updated routing builder.
         */
        public Builder processingProviders(ProcessingProviders processingProviders) {
            this.processingProviders = processingProviders;
            return this;
        }

        /**
         * Set model processors.
         *
         * @param modelProcessors all registered model processors.
         * @return updated routing builder.
         */
        public Builder modelProcessors(Iterable<ModelProcessor> modelProcessors) {
            this.modelProcessors = modelProcessors;
            return this;
        }

        /**
         * Set model processors.
         *
         * @param createServiceFunction all registered model processors.
         * @return updated routing builder.
         */
        public Builder createService(Function<Class<?>, ?> createServiceFunction) {
            this.createServiceFunction = createServiceFunction;
            return this;
        }

        /**
         * Set builder of ResourceMethodInvoker.
         *
         * @param resourceMethodInvokerBuilder resource method invoker builder.
         * @return updated routing builder.
         */
        public Builder resourceMethodInvokerBuilder(ResourceMethodInvoker.Builder resourceMethodInvokerBuilder) {
            this.resourceMethodInvokerBuilder = resourceMethodInvokerBuilder;
            return this;
        }

        /**
         * Build routing stage.
         *
         * @return routing stage for the runtime resource model.
         */
        public ChainableStage<RequestProcessingContext> buildStage() {
            // No L10N - internally used class
            if (resourceContext == null) {
                throw new NullPointerException("Resource context is not set.");
            }
            if (config == null) {
                throw new NullPointerException("Runtime configuration is not set.");
            }
            if (entityProviders == null) {
                throw new NullPointerException("Entity providers are not set.");
            }
            if (valueSuppliers == null) {
                throw new NullPointerException("Value supplier providers are not set.");
            }
            if (modelProcessors == null) {
                throw new NullPointerException("Model processors are not set.");
            }
            if (createServiceFunction == null) {
                throw new NullPointerException("Create function is not set.");
            }
            if (processingProviders == null) {
                throw new NullPointerException("Processing providers are not set.");
            }
            if (resourceMethodInvokerBuilder == null) {
                throw new NullPointerException("ResourceMethodInvokerBuilder is not set.");
            }

            final RuntimeModelBuilder runtimeModelBuilder = new RuntimeModelBuilder(
                    resourceContext,
                    config,
                    entityProviders,
                    valueSuppliers,
                    processingProviders,
                    resourceMethodInvokerBuilder,
                    modelProcessors,
                    createServiceFunction);

            return new RoutingStage(runtimeModelBuilder.buildModel(resourceModel, false));
        }
    }
}
