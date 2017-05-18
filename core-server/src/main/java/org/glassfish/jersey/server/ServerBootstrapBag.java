/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server;

import java.util.Collection;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

/**
 * {@inheritDoc}
 * <p>
 * This bootstrap bag is specialized for server part of Jersey.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class ServerBootstrapBag extends BootstrapBag {

    private Application application;
    private ApplicationHandler applicationHandler;
    private Collection<ValueParamProvider> valueParamProviders;
    private MultivaluedParameterExtractorProvider multivaluedParameterExtractorProvider;
    private ProcessingProviders processingProviders;
    private JerseyResourceContext resourceContext;
    private LazyValue<Collection<ComponentProvider>> componentProviders;
    private ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    private ResourceBag resourceBag;
    private ResourceModel resourceModel;
    private Collection<ModelProcessor> modelProcessors;

    public Collection<ModelProcessor> getModelProcessors() {
        return modelProcessors;
    }

    public void setModelProcessors(Collection<ModelProcessor> modelProcessors) {
        this.modelProcessors = modelProcessors;
    }

    public ResourceBag getResourceBag() {
        requireNonNull(resourceBag, ResourceBag.class);
        return resourceBag;
    }

    public void setResourceBag(ResourceBag resourceBag) {
        this.resourceBag = resourceBag;
    }

    public ResourceConfig getRuntimeConfig() {
        return (ResourceConfig) getConfiguration();
    }

    public Application getApplication() {
        requireNonNull(application, Application.class);
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public ApplicationHandler getApplicationHandler() {
        requireNonNull(applicationHandler, ApplicationHandler.class);
        return applicationHandler;
    }

    public void setApplicationHandler(ApplicationHandler applicationHandler) {
        this.applicationHandler = applicationHandler;
    }

    public ProcessingProviders getProcessingProviders() {
        requireNonNull(processingProviders, ProcessingProviders.class);
        return processingProviders;
    }

    public void setProcessingProviders(ProcessingProviders processingProviders) {
        this.processingProviders = processingProviders;
    }

    public MultivaluedParameterExtractorProvider getMultivaluedParameterExtractorProvider() {
        requireNonNull(multivaluedParameterExtractorProvider, MultivaluedParameterExtractorProvider.class);
        return multivaluedParameterExtractorProvider;
    }

    public void setMultivaluedParameterExtractorProvider(MultivaluedParameterExtractorProvider provider) {
        this.multivaluedParameterExtractorProvider = provider;
    }

    public Collection<ValueParamProvider> getValueParamProviders() {
        requireNonNull(valueParamProviders, new GenericType<Collection<ValueParamProvider>>() {}.getType());
        return valueParamProviders;
    }

    public void setValueParamProviders(Collection<ValueParamProvider> valueParamProviders) {
        this.valueParamProviders = valueParamProviders;
    }

    public JerseyResourceContext getResourceContext() {
        requireNonNull(resourceContext, JerseyResourceContext.class);
        return resourceContext;
    }

    public void setResourceContext(JerseyResourceContext resourceContext) {
        this.resourceContext = resourceContext;
    }

    public LazyValue<Collection<ComponentProvider>> getComponentProviders() {
        requireNonNull(componentProviders, new GenericType<LazyValue<Collection<ComponentProvider>>>() {}.getType());
        return componentProviders;
    }

    public void setComponentProviders(LazyValue<Collection<ComponentProvider>> componentProviders) {
        this.componentProviders = componentProviders;
    }

    public ResourceMethodInvoker.Builder getResourceMethodInvokerBuilder() {
        requireNonNull(resourceMethodInvokerBuilder, ResourceMethodInvoker.Builder.class);
        return resourceMethodInvokerBuilder;
    }

    public void setResourceMethodInvokerBuilder(ResourceMethodInvoker.Builder resourceMethodInvokerBuilder) {
        this.resourceMethodInvokerBuilder = resourceMethodInvokerBuilder;
    }

    public ResourceModel getResourceModel() {
        requireNonNull(resourceModel, ResourceModel.class);
        return resourceModel;
    }

    public void setResourceModel(ResourceModel resourceModel) {
        this.resourceModel = resourceModel;
    }
}
