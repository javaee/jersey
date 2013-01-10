/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.BindingPriority;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ModelProcessorServerTest {
    public static class ModelProcessorFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(SimpleModelProcessor.class);
            return true;
        }

        @BindingPriority(5000)
        private static class SimpleModelProcessor implements ModelProcessor {

            @Override
            public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
                ResourceModel.Builder modelBuilder = new ResourceModel.Builder(resourceModel.getRootResources());
                final Resource modelResource = Resource.from(ModelResource.class);
                modelBuilder.addResource(modelResource);

                for (final Resource resource : resourceModel.getRootResources()) {
                    Resource newResource = enhanceResource(resource);
                    modelBuilder.addResource(newResource);
                }

                return modelBuilder.build();
            }

            private Resource enhanceResource(final Resource resource) {
                boolean optionsFound = false;
                Resource newResource;
                for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
                    if (resourceMethod.getHttpMethod().equals("OPTIONS")) {
                        optionsFound = true;
                    }
                }
                if (!optionsFound) {
                    final Resource.Builder resBuilder = Resource.builder(resource);
                    resBuilder.addMethod("OPTIONS").handledBy(new Inflector<ContainerRequestContext, String>() {
                        @Override
                        public String apply(ContainerRequestContext containerRequestContext) {
                            return resource.getPath();
                        }
                    });
                    newResource = resBuilder.build();
                } else {
                    newResource = resource;
                }
                return newResource;
            }

            @Override
            public Resource processSubResource(Resource subResource, Configuration configuration) {
                final Resource resource = enhanceResource(subResource);
                return resource;
            }
        }


        @Path("model")
        public static class ModelResource {
            @Context
            ExtendedResourceContext resourceContext;

            @GET
            public String get() {
                final ResourceModel resourceModel = resourceContext.getResourceModel();
                StringBuilder sb = new StringBuilder();
                for (Resource resource : resourceModel.getRootResources()) {
                    sb.append(resource.getPath()).append("\n");
                }

                return sb.toString();
            }
        }
    }

    @Path("a")
    public static class ResourceA {
        @GET
        public String get() {
            return "a-get";
        }

        @POST
        public String post(String entity) {
            return "post";
        }

        @GET
        @Path("child")
        public String getChild() {
            return "child-get";
        }

        @Path("locator")
        public SubResource locator() {
            return new SubResource();
        }
    }

    public static class SubResource {
        @GET
        public String get() {
            return "sub-get";
        }
    }

    @Path("b")
    public static class ResourceB {
        @GET
        public String get() {
            return "b-get";
        }

        @OPTIONS
        public String options() {
            return "b-options";
        }

        @Path("locator")
        public SubResource locator() {
            return new SubResource();
        }
    }


}
