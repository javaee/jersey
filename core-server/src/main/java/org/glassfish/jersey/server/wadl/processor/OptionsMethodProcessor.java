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

package org.glassfish.jersey.server.wadl.processor;

import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;
import javax.inject.Inject;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.internal.ModelProcessorUtil;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * {@link ModelProcessor Model processor} enhancing {@link ResourceModel resource model} and {@link Resource sub resources}
 * by default OPTIONS methods defined by JAX-RS specification.
 *
 * @author Miroslav Fuksa
 */
@Priority(Integer.MAX_VALUE)
public class OptionsMethodProcessor implements ModelProcessor {

    private final List<ModelProcessorUtil.Method> methodList;

    /**
     * Creates new instance.
     */
    public OptionsMethodProcessor() {
        methodList = Lists.newArrayList();
        methodList.add(new ModelProcessorUtil.Method(HttpMethod.OPTIONS, MediaType.WILDCARD_TYPE, MediaType.TEXT_PLAIN_TYPE,
                PlainTextOptionsInflector.class));

        methodList.add(new ModelProcessorUtil.Method(HttpMethod.OPTIONS, MediaType.WILDCARD_TYPE, MediaType.WILDCARD_TYPE,
                GenericOptionsInflector.class));
    }


    private static class PlainTextOptionsInflector implements Inflector<ContainerRequestContext, Response> {

        @Inject
        private ExtendedUriInfo extendedUriInfo;

        @Override
        public Response apply(ContainerRequestContext containerRequestContext) {
            Set<String> allowedMethods = ModelProcessorUtil.getAllowedMethods(extendedUriInfo
                    .getMatchedRuntimeResources().get(0));

            final String allowedList = allowedMethods.toString();
            final String optionsBody = allowedList.substring(1, allowedList.length() - 1);

            return Response.ok(optionsBody, MediaType.TEXT_PLAIN_TYPE)
                    .allow(allowedMethods)
                    .build();
        }
    }

    private static class GenericOptionsInflector implements Inflector<ContainerRequestContext, Response> {
        @Inject
        private ExtendedUriInfo extendedUriInfo;


        @Override
        public Response apply(ContainerRequestContext containerRequestContext) {
            final Set<String> allowedMethods = ModelProcessorUtil.getAllowedMethods(
                    (extendedUriInfo.getMatchedRuntimeResources().get(0)));
            return Response.ok()
                    .allow(allowedMethods)
                    .header(HttpHeaders.CONTENT_LENGTH, "0")
                    .type(containerRequestContext.getAcceptableMediaTypes().get(0))
                    .build();
        }
    }

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
        return ModelProcessorUtil.enhanceResourceModel(resourceModel, false, methodList, true).build();
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        return ModelProcessorUtil.enhanceResourceModel(subResourceModel, true, methodList, true).build();
    }
}
