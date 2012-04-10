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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.hk2.inject.Injector;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.InflectorBasedResourceMethod;
import org.glassfish.jersey.server.model.PathValue;
import org.glassfish.jersey.server.model.ResourceBuilder;
import org.glassfish.jersey.server.model.ResourceClass;


import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jvnet.hk2.annotations.Inject;

/**
 * Implementation of the {@link JerseyApplication.Builder Jersey application builder}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
/*package*/ class DefaultResourceBuilder implements ResourceBuilder {

    private static final Logger LOGGER = Logger.getLogger(DefaultResourceBuilder.class.getName());

    private class BoundBuilder implements BoundResourceBuilder {

        final ResourceClass resource;
        final Set<String> methods = new HashSet<String>();
        private final List<MediaType> resourceConsumes = new ArrayList<MediaType>();
        private final List<MediaType> resourceProduces = new ArrayList<MediaType>();
        private final ResourceMethodBuilder resourceMethodBuilder = new ResourceMethodBuilder() {

            private final BoundBuilder applicationBuilder = BoundBuilder.this;
            private final List<MediaType> methodConsumes = new ArrayList<MediaType>();
            private final List<MediaType> methodProduces = new ArrayList<MediaType>();

            @Override
            public BoundResourceBuilder to(Inflector<Request, Response> transformation) {
                for (String method : methods) {
                    List<MediaType> effectiveInputTypes = methodConsumes.isEmpty() ? resourceConsumes : methodConsumes;
                    List<MediaType> effectiveOutputTypes = methodProduces.isEmpty() ? resourceProduces : methodProduces;
                    resource.getResourceMethods().add(new InflectorBasedResourceMethod(resource, method, effectiveInputTypes, effectiveOutputTypes, transformation));
                }
                methods.clear();
                return applicationBuilder;
            }

            @Override
            public BoundResourceBuilder to(final Class<? extends Inflector<Request, Response>> transformationClass) {
                return to(new Inflector<Request, Response>() {
                    // gets injected in multiple method acceptor
                    private @Inject Injector injector;

                    @Override
                    public Response apply(Request data) {
                        final Inflector<Request, Response> transformation = injector.inject(transformationClass);
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

        public BoundBuilder(String path) {
            resource = getResourceClass(path);
        }

        public BoundBuilder(String path, String method, Inflector<Request, Response> transformation) {
            Preconditions.checkNotNull(path, "Path must not be null.");
            Preconditions.checkNotNull(method, "HTTP method must not be null.");
            Preconditions.checkArgument(!method.isEmpty(), "HTTP method must not be empty string.");
            Preconditions.checkNotNull(transformation, "Transformation must not be null.");

            resource = getResourceClass(path);
            resource.getResourceMethods().add(new InflectorBasedResourceMethod(resource, method, null, null, transformation));
        }

        private ResourceClass getResourceClass(String path) {
            ResourceClass res = pathToResource.get(path);

            if (res == null) {
                res = new ResourceClass(null, new PathValue(path));
                resources.add(res);
                pathToResource.put(path, res);
            }

            return res;
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
        public BoundResourceBuilder produces(MediaType... mediaTypes) {
            resourceProduces.addAll(Arrays.asList(mediaTypes));
            return this;
        }

        @Override
        public BoundResourceBuilder consumes(MediaType... mediaTypes) {
            resourceConsumes.addAll(Arrays.asList(mediaTypes));
            return this;
        }
//        @Override
//        public BoundResourceBuilder subPath(String subPath) {
//            throw new UnsupportedOperationException();
//        }
    }
    //
    ApplicationHandler application;
    ResourceConfig resourceConfig;
    private Set<ResourceClass> resources = Sets.newHashSet();
    private Map<String, ResourceClass> pathToResource = Maps.newHashMap();

    /*package*/ DefaultResourceBuilder() {
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
    public BoundResourceBuilder path(String path) {
        return new BoundBuilder(path);
    }

    /**
     * Create new application based on the defined resource bindings.
     *
     * @return newly created application.
     */
    @Override
    public Set<ResourceClass> build() {
        return Sets.newHashSet(resources);
    }
}
