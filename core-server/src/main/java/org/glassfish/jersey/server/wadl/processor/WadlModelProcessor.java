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

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.xml.bind.Marshaller;

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
import org.glassfish.jersey.server.wadl.internal.WadlUtils;

import com.sun.research.ws.wadl.Application;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * WADL {@link ModelProcessor model processor} which enhance resource model by WADL related resources (like "/application.wadl").
 * The provider should be registered using
 * {@link org.glassfish.jersey.server.wadl.internal.WadlAutoDiscoverable} or by
 * {@link org.glassfish.jersey.server.wadl.WadlFeature} if auto-discovery is disabled.
 *
 * @author Miroslav Fuksa
 *
 */
@Priority(10000)
public class WadlModelProcessor implements ModelProcessor {


    private final List<ModelProcessorUtil.Method> methodList;

    /**
     * Create new WADL model processor instance.
     */
    public WadlModelProcessor() {
        methodList = Lists.newArrayList();
        methodList.add(new ModelProcessorUtil.Method(HttpMethod.OPTIONS, MediaType.WILDCARD_TYPE, MediaTypes.WADL_TYPE,
                OptionsHandler.class));
    }


    @Override
    public ResourceModel processResourceModel(final ResourceModel resourceModel, final Configuration configuration) {
        final boolean disabled = PropertiesHelper.isProperty(configuration.getProperty(ServerProperties.WADL_FEATURE_DISABLE));
        if (disabled) {
            return resourceModel;
        }

        final ResourceModel.Builder builder = ModelProcessorUtil.enhanceResourceModel(resourceModel, false, methodList, true);

        // Do not add WadlResource if already present in the classes (i.e. added during scanning).
        if (!configuration.getClasses().contains(WadlResource.class)) {
            final Resource wadlResource = Resource.builder(WadlResource.class).build();
            builder.addResource(wadlResource);
        }

        return builder.build();

    }

    /**
     * OPTIONS resource method handler that serves resource WADL.
     */
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
            final UriInfo uriInfo = containerRequestContext.getUriInfo();

            final Application wadlApplication = wadlApplicationContext.getApplication(uriInfo,
                    resource.getResources().get(0), WadlUtils.isDetailedWadlRequested(uriInfo));

            if (wadlApplication == null) {
                // wadlApplication can be null if limited WADL is requested and all content
                // of wadlApplication is invisible in limited WADL
                return Response.status(Response.Status.NOT_FOUND).build();

            }

            byte[] bytes;
            try {
                final Marshaller marshaller = wadlApplicationContext.getJAXBContext().createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                marshaller.marshal(wadlApplication, os);
                bytes = os.toByteArray();
                os.close();
            } catch (Exception e) {
                throw new ProcessingException("Could not marshal the wadl Application.", e);
            }

            return Response.ok()
                    .type(MediaTypes.WADL_TYPE)
                    .allow(ModelProcessorUtil.getAllowedMethods(resource))
                    .header("Last-modified", lastModified)
                    .entity(bytes)
                    .build();
        }
    }

    @Override
    public ResourceModel processSubResource(ResourceModel resourceModel, Configuration configuration) {
        final boolean disabled = PropertiesHelper.isProperty(configuration.getProperty(ServerProperties.WADL_FEATURE_DISABLE));
        if (disabled) {
            return resourceModel;
        }
        return ModelProcessorUtil.enhanceResourceModel(resourceModel, true, methodList, true).build();
    }
}
