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

package org.glassfish.jersey.server.wadl.processor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.inject.Inject;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.server.model.internal.ModelProcessorUtil;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;
import org.glassfish.jersey.server.wadl.internal.WadlResource;

import com.google.common.collect.Lists;

import com.sun.research.ws.wadl.Application;

/**
 * WADL {@link ModelProcessor model processor} which enhance resource model by WADL related resources (like "/application.wadl").
 * The provider should be registered using {@link WadlModelProcessorFeature}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
@Priority(10000)
public class WadlModelProcessor implements ModelProcessor {


    private final List<ModelProcessorUtil.Method> methodList;

    public WadlModelProcessor() {
        methodList = Lists.newArrayList();
        methodList.add(new ModelProcessorUtil.Method(HttpMethod.OPTIONS, MediaType.WILDCARD_TYPE, MediaTypes.WADL,
                OptionsHandler.class));
    }


    @Override
    public ResourceModel processResourceModel(final ResourceModel resourceModel, final Configuration configuration) {
        final boolean disabled = PropertiesHelper.isProperty(configuration.getProperty
                (ServerProperties.FEATURE_DISABLE_WADL));
        if (disabled) {
            return resourceModel;
        }

        final Resource wadlResource = Resource.builder(WadlResource.class).build();

        final ResourceModel.Builder builder = ModelProcessorUtil.enhanceResourceModel(resourceModel, false, methodList);
        builder.addResource(wadlResource);
        return builder.build();

    }

    public static class OptionsHandler implements Inflector<ContainerRequestContext, Response> {
        private final String lastModified =
                new SimpleDateFormat(WadlResource.HTTPDATEFORMAT).format(new Date());

        @Inject
        private ExtendedUriInfo extendedUriInfo;

        @Context
        private WadlApplicationContext wadlApplicationContext;


        @Override
        public Response apply(ContainerRequestContext containerRequestContext) {

            final RuntimeResource resource = extendedUriInfo.getMatchedRuntimeResources().get(0);
            // TODO: support multiple resources, see ignored tests in WadlResourceTest.Wadl8Test
            final Application wadlApplication = wadlApplicationContext.getApplication(
                    containerRequestContext.getUriInfo(),
                    resource.getResources().get(0));
            Response response = Response.ok()
                    .type(MediaTypes.WADL)
                    .allow(ModelProcessorUtil.getAllowedMethods(resource))
                    .header("Last-modified", lastModified)
                    .entity(wadlApplication)
                    .build();

            return response;
        }
    }

    @Override
    public ResourceModel processSubResource(ResourceModel resourceModel, Configuration configuration) {
        final boolean disabled = PropertiesHelper.isProperty(configuration.getProperty(ServerProperties.FEATURE_DISABLE_WADL));
        if (disabled) {
            return resourceModel;
        }
        return ModelProcessorUtil.enhanceResourceModel(resourceModel, true, methodList).build();
    }
}
