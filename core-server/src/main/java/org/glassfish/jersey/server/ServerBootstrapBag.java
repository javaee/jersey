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
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.model.internal.ManagedObjectsFinalizer;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.server.internal.JerseyResourceContext;
import org.glassfish.jersey.server.internal.ProcessingProviders;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.internal.ValueSupplierProvider;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

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
    private Collection<ValueSupplierProvider> valueSupplierProviders;
    private MultivaluedParameterExtractorProvider multivaluedParameterExtractorProvider;
    private ProcessingProviders processingProviders;
    private JerseyResourceContext resourceContext;
    private LazyValue<Collection<ComponentProvider>> componentProviders;
    private ResourceMethodInvoker.Builder resourceMethodInvokerBuilder;
    private ResourceBag resourceBag;
    private ResourceModel resourceModel;

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

    public Collection<ValueSupplierProvider> getValueSupplierProviders() {
        requireNonNull(valueSupplierProviders, new GenericType<Collection<ValueSupplierProvider>>() {}.getType());
        return valueSupplierProviders;
    }

    public void setValueSupplierProviders(Collection<ValueSupplierProvider> valueSupplierProviders) {
        this.valueSupplierProviders = valueSupplierProviders;
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
        return resourceModel;
    }

    public void setResourceModel(ResourceModel resourceModel) {
        this.resourceModel = resourceModel;
    }

    /**
     * Creates an immutable version of bootstrap bag.
     *
     * @return immutable bootstrap bag.
     */
    public ServerBootstrapBag toImmutable() {
        return new ImmutableServerBootstrapBag(this);
    }

    /**
     * Immutable version of {@link BootstrapBag}.
     */
    static class ImmutableServerBootstrapBag extends ServerBootstrapBag {

        private final ServerBootstrapBag delegate;

        private ImmutableServerBootstrapBag(ServerBootstrapBag delegate) {
            this.delegate = delegate;
        }

        public ResourceModel getResourceModel() {
            return delegate.getResourceModel();
        }

        public void setResourceModel(ResourceModel resourceModel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceBag getResourceBag() {
            return delegate.getResourceBag();
        }

        @Override
        public void setResourceBag(ResourceBag resourceBag) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceConfig getRuntimeConfig() {
            return delegate.getRuntimeConfig();
        }

        @Override
        public Application getApplication() {
            return delegate.getApplication();
        }

        @Override
        public void setApplication(Application application) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApplicationHandler getApplicationHandler() {
            return delegate.getApplicationHandler();
        }

        @Override
        public void setApplicationHandler(ApplicationHandler applicationHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProcessingProviders getProcessingProviders() {
            return delegate.getProcessingProviders();
        }

        @Override
        public void setProcessingProviders(ProcessingProviders processingProviders) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MultivaluedParameterExtractorProvider getMultivaluedParameterExtractorProvider() {
            return delegate.getMultivaluedParameterExtractorProvider();
        }

        @Override
        public void setMultivaluedParameterExtractorProvider(MultivaluedParameterExtractorProvider provider) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<ValueSupplierProvider> getValueSupplierProviders() {
            return delegate.getValueSupplierProviders();
        }

        @Override
        public void setValueSupplierProviders(Collection<ValueSupplierProvider> valueSupplierProviders) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JerseyResourceContext getResourceContext() {
            return delegate.getResourceContext();
        }

        @Override
        public void setResourceContext(JerseyResourceContext resourceContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LazyValue<Collection<ComponentProvider>> getComponentProviders() {
            return delegate.getComponentProviders();
        }

        @Override
        public void setComponentProviders(LazyValue<Collection<ComponentProvider>> componentProviders) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceMethodInvoker.Builder getResourceMethodInvokerBuilder() {
            return delegate.getResourceMethodInvokerBuilder();
        }

        @Override
        public void setResourceMethodInvokerBuilder(ResourceMethodInvoker.Builder resourceMethodInvokerBuilder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestScope getRequestScope() {
            return delegate.getRequestScope();
        }

        @Override
        public void setRequestScope(RequestScope requestScope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MessageBodyWorkers getMessageBodyWorkers() {
            return delegate.getMessageBodyWorkers();
        }

        @Override
        public void setMessageBodyWorkers(MessageBodyWorkers messageBodyWorkers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Configuration getConfiguration() {
            return delegate.getConfiguration();
        }

        @Override
        public void setConfiguration(Configuration configuration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ExceptionMappers getExceptionMappers() {
            return delegate.getExceptionMappers();
        }

        @Override
        public void setExceptionMappers(ExceptionMappers exceptionMappers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContextResolvers getContextResolvers() {
            return delegate.getContextResolvers();
        }

        @Override
        public void setContextResolvers(ContextResolvers contextResolvers) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManagedObjectsFinalizer getManagedObjectsFinalizer() {
            return delegate.getManagedObjectsFinalizer();
        }

        @Override
        public void setManagedObjectsFinalizer(ManagedObjectsFinalizer managedObjectsFinalizer) {
            throw new UnsupportedOperationException();
        }
    }
}
