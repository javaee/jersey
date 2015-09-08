/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.wadl.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.xml.namespace.QName;

import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Doc;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.ParamStyle;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * This class implements the algorithm how the wadl is built for one or more {@link org.glassfish.jersey.server.model.Resource}
 * classes. Wadl artifacts are created by a {@link org.glassfish.jersey.server.wadl.WadlGenerator}. Created on: Jun 18, 2008<br>
 *
 * @author Marc Hadley
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 * @author Miroslav Fuksa
 */
public class WadlBuilder {

    private final WadlGenerator _wadlGenerator;
    private final UriInfo uriInfo;

    private final boolean detailedWadl;

    public WadlBuilder(WadlGenerator wadlGenerator, boolean detailedWadl, UriInfo uriInfo) {
        this.detailedWadl = detailedWadl;
        _wadlGenerator = wadlGenerator;
        this.uriInfo = uriInfo;
    }

    /**
     * Generate WADL for a set of resources.
     *
     * @param resources the set of resources.
     * @return the JAXB WADL application bean.
     */
    public ApplicationDescription generate(List<org.glassfish.jersey.server.model.Resource> resources) {
        Application wadlApplication = _wadlGenerator.createApplication();
        Resources wadlResources = _wadlGenerator.createResources();

        // for each resource
        for (org.glassfish.jersey.server.model.Resource r : resources) {
            Resource wadlResource = generateResource(r, r.getPath());
            if (wadlResource == null) {
                continue;
            }
            wadlResources.getResource().add(wadlResource);
        }
        wadlApplication.getResources().add(wadlResources);

        addVersion(wadlApplication);
        addHint(wadlApplication);

        // Build any external grammars

        WadlGenerator.ExternalGrammarDefinition external =
                _wadlGenerator.createExternalGrammar();
        //

        ApplicationDescription description = new ApplicationDescription(wadlApplication, external);

        // Attach the data to the parts of the model

        _wadlGenerator.attachTypes(description);

        // Return the description of the application

        return description;
    }

    /**
     * Generate WADL for a resource.
     *
     * @param resource    the resource
     * @param description the overall application description so we can
     * @return the JAXB WADL application bean
     */
    public Application generate(
            ApplicationDescription description,
            org.glassfish.jersey.server.model.Resource resource) {
        try {
            Application wadlApplication = _wadlGenerator.createApplication();
            Resources wadlResources = _wadlGenerator.createResources();
            Resource wadlResource = generateResource(resource, null);
            if (wadlResource == null) {
                return null;
            }
            wadlResources.getResource().add(wadlResource);
            wadlApplication.getResources().add(wadlResources);

            addVersion(wadlApplication);

            // Attach the data to the parts of the model

            _wadlGenerator.attachTypes(description);

            // Return the WADL

            return wadlApplication;
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_RESOURCE(resource), e);
        }
    }

    private void addVersion(Application wadlApplication) {
        // Include Jersey version as doc element with generatedBy attribute
        Doc d = new Doc();
        d.getOtherAttributes().put(new QName(WadlApplicationContextImpl.WADL_JERSEY_NAMESPACE, "generatedBy", "jersey"),
                Version.getBuildId());

        wadlApplication.getDoc().add(d);
    }

    private void addHint(Application wadlApplication) {
        // TODO: this not-null check is here only because of unit tests
        if (uriInfo != null) {
            Doc d = new Doc();

            String message;

            if (detailedWadl) {
                final String uriWithoutQueryParam = UriBuilder.fromUri(uriInfo.getRequestUri()).replaceQuery("").build()
                        .toString();
                message = LocalizationMessages.WADL_DOC_EXTENDED_WADL(WadlUtils.DETAILED_WADL_QUERY_PARAM, uriWithoutQueryParam);
            } else {
                final String uriWithQueryParam = UriBuilder.fromUri(uriInfo.getRequestUri())
                        .queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true").build().toString();

                message = LocalizationMessages.WADL_DOC_SIMPLE_WADL(WadlUtils.DETAILED_WADL_QUERY_PARAM, uriWithQueryParam);
            }

            d.getOtherAttributes().put(new QName(WadlApplicationContextImpl.WADL_JERSEY_NAMESPACE, "hint", "jersey"), message);
            wadlApplication.getDoc().add(d);

        }
    }

    private com.sun.research.ws.wadl.Method generateMethod(
            final org.glassfish.jersey.server.model.Resource parentResource,
            final Map<String, Param> wadlResourceParams,
            final org.glassfish.jersey.server.model.ResourceMethod resourceMethod) {

        try {
            if (!detailedWadl && resourceMethod.isExtended()) {
                return null;
            }
            com.sun.research.ws.wadl.Method wadlMethod = _wadlGenerator.createMethod(parentResource, resourceMethod);

            // generate the request part
            Request wadlRequest = generateRequest(parentResource, resourceMethod, wadlResourceParams);
            if (wadlRequest != null) {
                wadlMethod.setRequest(wadlRequest);
            }
            // generate the response part
            final List<Response> responses = generateResponses(parentResource, resourceMethod);
            if (responses != null) {
                wadlMethod.getResponse().addAll(responses);
            }
            return wadlMethod;
        } catch (Exception e) {
            throw new ProcessingException(
                    LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_METHOD(resourceMethod, parentResource), e);
        }
    }

    private Request generateRequest(org.glassfish.jersey.server.model.Resource parentResource,
                                    final org.glassfish.jersey.server.model.ResourceMethod resourceMethod,
                                    Map<String, Param> wadlResourceParams) {
        try {
            final List<Parameter> requestParams = Lists.newLinkedList(resourceMethod.getInvocable().getParameters());
            // Adding handler instance parameters to the list of potential request parameters.
            requestParams.addAll(resourceMethod.getInvocable().getHandler().getParameters());

            if (requestParams.isEmpty()) {
                return null;
            }

            Request wadlRequest = _wadlGenerator.createRequest(parentResource, resourceMethod);

            processRequestParameters(parentResource, resourceMethod, wadlResourceParams, requestParams, wadlRequest);

            if (wadlRequest.getRepresentation().size() + wadlRequest.getParam().size() == 0) {
                return null;
            } else {
                return wadlRequest;
            }
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_REQUEST(
                    resourceMethod, parentResource), e);
        }
    }

    /**
     * Recursively processes provided request parameters and adds the resulting WADL information into the WADL request.
     */
    private void processRequestParameters(final org.glassfish.jersey.server.model.Resource parentResource,
                                          final ResourceMethod resourceMethod,
                                          final Map<String, Param> wadlResourceParams,
                                          final Collection<Parameter> requestParameters,
                                          final Request wadlRequest) {
        for (Parameter parameter : requestParameters) {
            if (parameter.getSource() == Parameter.Source.ENTITY || parameter.getSource() == Parameter.Source.UNKNOWN) {
                for (MediaType mediaType : resourceMethod.getConsumedTypes()) {
                    setRepresentationForMediaType(parentResource, resourceMethod, mediaType, wadlRequest);
                }
            } else if (parameter.getSourceAnnotation().annotationType() == FormParam.class) {
                // Use application/x-www-form-urlencoded if no @Consumes
                List<MediaType> supportedInputTypes = resourceMethod.getConsumedTypes();
                if (supportedInputTypes.isEmpty()
                        || (supportedInputTypes.size() == 1 && supportedInputTypes.get(0).isWildcardType())) {
                    supportedInputTypes = Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
                }

                for (MediaType mediaType : supportedInputTypes) {
                    final Representation wadlRepresentation =
                            setRepresentationForMediaType(parentResource, resourceMethod, mediaType, wadlRequest);
                    if (getParamByName(wadlRepresentation.getParam(), parameter.getSourceName()) == null) {
                        final Param wadlParam = generateParam(parentResource, resourceMethod, parameter);
                        if (wadlParam != null) {
                            wadlRepresentation.getParam().add(wadlParam);
                        }
                    }
                }
            } else if ("org.glassfish.jersey.media.multipart.FormDataParam".equals(
                    parameter.getSourceAnnotation().annotationType().getName())) { // jersey-multipart support
                // Use multipart/form-data if no @Consumes
                List<MediaType> supportedInputTypes = resourceMethod.getConsumedTypes();
                if (supportedInputTypes.isEmpty()
                        || (supportedInputTypes.size() == 1 && supportedInputTypes.get(0).isWildcardType())) {
                    supportedInputTypes = Collections.singletonList(MediaType.MULTIPART_FORM_DATA_TYPE);
                }

                for (MediaType mediaType : supportedInputTypes) {
                    final Representation wadlRepresentation =
                            setRepresentationForMediaType(parentResource, resourceMethod, mediaType, wadlRequest);
                    if (getParamByName(wadlRepresentation.getParam(), parameter.getSourceName()) == null) {
                        final Param wadlParam = generateParam(parentResource, resourceMethod, parameter);
                        if (wadlParam != null) {
                            wadlRepresentation.getParam().add(wadlParam);
                        }
                    }
                }
            } else if (parameter instanceof Parameter.BeanParameter) {
                processRequestParameters(parentResource, resourceMethod, wadlResourceParams,
                        ((Parameter.BeanParameter) parameter).getParameters(), wadlRequest);
            } else {
                Param wadlParam = generateParam(parentResource, resourceMethod, parameter);
                if (wadlParam == null) {
                    continue;
                }
                if (wadlParam.getStyle() == ParamStyle.TEMPLATE || wadlParam.getStyle() == ParamStyle.MATRIX) {
                    wadlResourceParams.put(wadlParam.getName(), wadlParam);
                } else {
                    wadlRequest.getParam().add(wadlParam);
                }
            }
        }
    }

    private Param getParamByName(final List<Param> params, final String name) {
        for (Param param : params) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    /**
     * Create the wadl {@link Representation} for the specified {@link MediaType} if not yet existing for the wadl {@link Request}
     * and return it.
     *
     * @param r           the resource
     * @param m           the resource method
     * @param mediaType   an accepted media type of the resource method
     * @param wadlRequest the wadl request the wadl representation is to be created for (if not yet existing).
     * @return the wadl request representation for the specified {@link MediaType}.
     */
    private Representation setRepresentationForMediaType(org.glassfish.jersey.server.model.Resource r,
                                                         final org.glassfish.jersey.server.model.ResourceMethod m,
                                                         MediaType mediaType,
                                                         Request wadlRequest) {
        try {
            Representation wadlRepresentation = getRepresentationByMediaType(wadlRequest.getRepresentation(), mediaType);
            if (wadlRepresentation == null) {
                wadlRepresentation = _wadlGenerator.createRequestRepresentation(r, m, mediaType);
                wadlRequest.getRepresentation().add(wadlRepresentation);
            }
            return wadlRepresentation;
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_REQUEST_MEDIA_TYPE(mediaType,
                    m, r), e);
        }
    }

    private Representation getRepresentationByMediaType(
            final List<Representation> representations, MediaType mediaType) {
        for (Representation representation : representations) {
            if (mediaType.toString().equals(representation.getMediaType())) {
                return representation;
            }
        }
        return null;
    }

    private Param generateParam(final org.glassfish.jersey.server.model.Resource resource,
                                final org.glassfish.jersey.server.model.ResourceMethod method,
                                final Parameter param) {
        try {
            if (param.getSource() == Parameter.Source.ENTITY || param.getSource() == Parameter.Source.CONTEXT) {
                return null;
            }
            return _wadlGenerator.createParam(resource, method, param);
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_PARAM(param, resource, method), e);
        }
    }

    private Resource generateResource(org.glassfish.jersey.server.model.Resource r, String path) {
        return generateResource(r, path, Collections.<org.glassfish.jersey.server.model.Resource>emptySet());
    }

    private Resource generateResource(final org.glassfish.jersey.server.model.Resource resource, String path,
                                      Set<org.glassfish.jersey.server.model.Resource> visitedResources) {
        try {

            if (!detailedWadl && resource.isExtended()) {
                return null;
            }
            Resource wadlResource = _wadlGenerator.createResource(resource, path);

            // prevent infinite recursion
            if (visitedResources.contains(resource)) {
                return wadlResource;
            } else {
                visitedResources = new HashSet<>(visitedResources);
                visitedResources.add(resource);
            }

            // if the resource contains subresource locator create new resource for this locator and return it instead
            // of this resource
            final ResourceMethod locator = resource.getResourceLocator();
            if (locator != null) {
                try {
                    org.glassfish.jersey.server.model.Resource.Builder builder = org.glassfish.jersey.server.model.Resource
                            .builder(locator.getInvocable().getRawResponseType());
                    if (builder == null) {
                        // for example in the case the return type of the sub resource locator is Object
                        builder = org.glassfish.jersey.server.model.Resource.builder().path(resource.getPath());
                    }
                    org.glassfish.jersey.server.model.Resource subResource = builder.build();

                    Resource wadlSubResource = generateResource(subResource, resource.getPath(), visitedResources);
                    if (wadlSubResource == null) {
                        return null;
                    }
                    if (locator.isExtended()) {
                        wadlSubResource.getAny().add(WadlApplicationContextImpl.EXTENDED_ELEMENT);
                    }

                    for (Parameter param : locator.getInvocable().getParameters()) {
                        Param wadlParam = generateParam(resource, locator, param);

                        if (wadlParam != null && wadlParam.getStyle() == ParamStyle.TEMPLATE) {
                            wadlSubResource.getParam().add(wadlParam);
                        }
                    }
                    return wadlSubResource;
                } catch (RuntimeException e) {
                    throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_RESOURCE_LOCATOR(locator,
                            resource), e);
                }
            }

            Map<String, Param> wadlResourceParams = new HashMap<>();
            // for each resource method
            for (org.glassfish.jersey.server.model.ResourceMethod method : resource.getResourceMethods()) {
                if (!detailedWadl && method.isExtended()) {
                    continue;
                }
                com.sun.research.ws.wadl.Method wadlMethod = generateMethod(resource, wadlResourceParams, method);
                wadlResource.getMethodOrResource().add(wadlMethod);

            }
            // add method parameters that are associated with the resource PATH template
            for (Param wadlParam : wadlResourceParams.values()) {
                wadlResource.getParam().add(wadlParam);
            }

            // for each sub-resource method
            Map<String, Resource> wadlSubResources = new HashMap<>();
            Map<String, Map<String, Param>> wadlSubResourcesParams = new HashMap<>();

            for (org.glassfish.jersey.server.model.Resource childResource : resource.getChildResources()) {
                Resource childWadlResource = generateResource(childResource, childResource.getPath(),
                        visitedResources);
                if (childWadlResource == null) {
                    continue;
                }
                wadlResource.getMethodOrResource().add(childWadlResource);
            }

            return wadlResource;
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_RESOURCE_PATH(resource, path), e);
        }
    }

    private List<Response> generateResponses(org.glassfish.jersey.server.model.Resource r, final ResourceMethod m) {
        try {
            if (m.getInvocable().getRawResponseType() == void.class) {
                return null;
            }
            return _wadlGenerator.createResponses(r, m);
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_BUILDER_GENERATION_RESPONSE(m, r), e);
        }
    }
}
