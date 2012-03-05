/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.google.common.base.Preconditions;
import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Module;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.BasicValidator;
import org.glassfish.jersey.server.model.InflectorBasedResourceMethod;
import org.glassfish.jersey.server.model.IntrospectionModeller;
import org.glassfish.jersey.server.model.PathValue;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.model.ResourceModelIssue;
import org.glassfish.jersey.server.model.ResourceModelValidator;
import org.glassfish.jersey.server.internal.routing.RuntimeModelProviderFromRootResource;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of the {@link Application.Builder Jersey application builder}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
/*package*/ class ApplicationBuilder implements Application.Builder {

    private class AppBoundBuilder implements BoundBuilder {

        final ResourceClass resource;
        final Set<String> methods = new HashSet<String>();
        private final List<MediaType> resourceConsumes = new ArrayList<MediaType>();
        private final List<MediaType> resourceProduces = new ArrayList<MediaType>();
        private final ResourceMethodBuilder resourceMethodBuilder = new ResourceMethodBuilder() {

            private final AppBoundBuilder applicationBuilder = AppBoundBuilder.this;
            private final List<MediaType> methodConsumes = new ArrayList<MediaType>();
            private final List<MediaType> methodProduces = new ArrayList<MediaType>();

            @Override
            public BoundBuilder to(Inflector<Request, Response> transformation) {
                for (String method : methods) {
                    List<MediaType> effectiveInputTypes = methodConsumes.isEmpty() ? resourceConsumes : methodConsumes;
                    List<MediaType> effectiveOutputTypes = methodProduces.isEmpty() ? resourceProduces : methodProduces;
                    resource.getResourceMethods().add(new InflectorBasedResourceMethod(resource, method, effectiveInputTypes, effectiveOutputTypes, transformation));
                }
                methods.clear();
                return applicationBuilder;
            }

            @Override
            public BoundBuilder to(final Class<? extends Inflector<Request, Response>> transformationClass) {
                return to(new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        final Inflector<Request, Response> transformation =
                                ApplicationBuilder.this.services.forContract(Injector.class).get().inject(transformationClass);
                        return transformation.apply(data);
                    }
                });
            }

            @Override
            public ResourceMethodBuilder produces(MediaType... mediaTypes) {
                methodProduces.addAll(Arrays.asList(mediaTypes));
                return resourceMethodBuilder;
            }

            @Override
            public ResourceMethodBuilder consumes(MediaType... mediaTypes) {
                methodConsumes.addAll(Arrays.asList(mediaTypes));
                return resourceMethodBuilder;
            }
        };

        public AppBoundBuilder(String path) {
            resource = takeExistingOrCreateVirtualResourceClass(path);
        }

        public AppBoundBuilder(String path, String method, Inflector<Request, Response> transformation) {
            Preconditions.checkNotNull(path, "Path must not be null.");
            Preconditions.checkNotNull(method, "HTTP method must not be null.");
            Preconditions.checkArgument(!method.isEmpty(), "HTTP method must not be empty string.");
            Preconditions.checkNotNull(transformation, "Transformation must not be null.");

            resource = takeExistingOrCreateVirtualResourceClass(path);
            resource.getResourceMethods().add(new InflectorBasedResourceMethod(resource, method, null, null, transformation));
        }

        private ResourceClass takeExistingOrCreateVirtualResourceClass(String path) {
            final ResourceClass existingResourceClass = lookupResourceClass(path);

            if (existingResourceClass != null) {
                return existingResourceClass;
            } else {
                final ResourceClass virtualResourceClass = new ResourceClass(null, new PathValue(path));
                resources.add(virtualResourceClass);
                return virtualResourceClass;
            }
        }

        @Override
        public ResourceMethodBuilder method(String... methods) {
            for (String method : methods) {
                Preconditions.checkNotNull(method, "HTTP method must not be null.");
                Preconditions.checkArgument(!method.isEmpty(), "HTTP method must not be empty string.");

                this.methods.add(method);
            }
            return resourceMethodBuilder;
        }

        @Override
        public BoundBuilder produces(MediaType... mediaTypes) {
            resourceProduces.addAll(Arrays.asList(mediaTypes));
            return this;
        }

        @Override
        public BoundBuilder consumes(MediaType... mediaTypes) {
            resourceConsumes.addAll(Arrays.asList(mediaTypes));
            return this;
        }

        @Override
        public BoundBuilder subPath(String subPath) {
            throw new UnsupportedOperationException();
        }
    }
    //
    private Application application;
    private ResourceConfig resourceConfig;
    private RuntimeModelProviderFromRootResource runtimeModelCreator;
    private Services services;
    private Set<ResourceClass> resources = new HashSet<ResourceClass>();

    /*package*/ ApplicationBuilder(@Nullable ResourceConfig resourceConfig) {
        this.resourceConfig = (resourceConfig != null) ? resourceConfig : ResourceConfig.empty();
        this.application = new Application();

        final Module[] jerseyModules = new Module[]{
            new ServerModule(),
            application.module()
        };

        Module[] modules = new Module[jerseyModules.length + this.resourceConfig.getCustomModules().size()];
        System.arraycopy(jerseyModules, 0, modules, 0, jerseyModules.length);
        System.arraycopy(this.resourceConfig.getCustomModules().toArray(), 0, modules, jerseyModules.length, this.resourceConfig.getCustomModules().size());

        // TODO parent/child services - when HK2 bec ready:
        //  this.jerseyServices = HK2.get().build(null, jerseyModules);
        //  this.services = HK2.get().build(jerseyServices, customModules);

        this.services = HK2.get().create(null, modules);

        this.runtimeModelCreator = services.byType(RuntimeModelProviderFromRootResource.class).get();

        final Class<? extends javax.ws.rs.core.Application> applicationClass = this.resourceConfig.getApplicationClass();

        if (applicationClass != null) {
            this.resourceConfig = new ResourceConfig(this.resourceConfig, services.forContract(applicationClass).get());
        }

        for (Class<?> c : this.resourceConfig.getClasses()) {
            if (IntrospectionModeller.isRootResource(c)) {
                resources.add(IntrospectionModeller.createResource(c));
            }
        }
    }

    /**
     * Bind a resource to a path within the application.
     * <p/>
     * TODO elaborate on javadoc.
     *
     * @param path resource path.
     * @return resource builder bound to the {@code path}.
     */
    @Override
    public BoundBuilder bind(String path) {
        return new AppBoundBuilder(path);
    }

    private ResourceClass lookupResourceClass(String path) {
        for (ResourceClass r : resources) {
            if (r.isRootResource() && r.getPath().getValue().equals(path)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Create new application based on the defined resource bindings.
     *
     * @return newly created application.
     */
    @Override
    public Application build() {

        // FIXME: this will not work if workers change. Need to be replaced with injection
        final ServiceProviders sp = services.forContract(ServiceProviders.Builder.class).get()
                .setProviderClasses(resourceConfig.getClasses())
                .setProviderInstances(resourceConfig.getSingletons())
                .build();
        final MessageBodyFactory messageBodyWorkers = new MessageBodyFactory(sp);
        // END

        runtimeModelCreator.setWorkers(messageBodyWorkers);
        validateResources(messageBodyWorkers);

        for (ResourceClass r : resources) {
            runtimeModelCreator.process(r);
        }

        application.setRootAcceptor(runtimeModelCreator.getRuntimeModel());

        services.forContract(Injector.class).get().inject(application);

        application.setResourceConfig(resourceConfig);

        return application;
    }

    private void validateResources(MessageBodyWorkers workers) {

        ResourceModelValidator validator = new BasicValidator(workers);

        for (ResourceClass r : resources) {
            validator.validate(r);
        }
        processIssues(validator);
    }

    private void processIssues(ResourceModelValidator validator) {

        final List<ResourceModelIssue> issueList = validator.getIssueList();
        if (!issueList.isEmpty()) {
            final Logger logger = Logger.getLogger(ApplicationBuilder.class.getName());
            final String allIssueMessages = allIssueLogMessages(validator.getIssueList());
            if (validator.fatalIssuesFound()) {
                logger.severe(
                        LocalizationMessages.ERRORS_AND_WARNINGS_DETECTED_WITH_RESOURCE_CLASSES(allIssueMessages));
            } else {
                logger.warning(
                        LocalizationMessages.WARNINGS_DETECTED_WITH_RESOURCE_CLASSES(allIssueMessages));
            }
        }

        if (validator.fatalIssuesFound()) {
            throw new ResourceModelValidator.ModelException(issueList);
        }
    }

    private String allIssueLogMessages(final List<ResourceModelIssue> issueList) {
        StringBuilder errors = new StringBuilder("\n");
        StringBuilder warnings = new StringBuilder();

        for (ResourceModelIssue issue: issueList) {
            if (issue.isFatal()) {
                errors.append(LocalizationMessages.ERROR_MSG(issue.getMessage())).append('\n');
            } else {
                warnings.append(LocalizationMessages.WARNING_MSG(issue.getMessage())).append('\n');
            }
        }

        return errors.append(warnings).toString();
    }
}
