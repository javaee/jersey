/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import javax.xml.namespace.QName;

import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Parameterized;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.generators.WadlGeneratorJAXBGrammarGenerator;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Doc;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.ParamStyle;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * This class implements the algorithm how the wadl is built for one or more
 * {@link org.glassfish.jersey.server.model.Resource} classes. Wadl artifacts are created by a
 * {@link org.glassfish.jersey.server.wadl.WadlGenerator}.
 * Created on: Jun 18, 2008<br>
 *
 * @author Marc Hadley
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public class WadlBuilder {

    private WadlGenerator _wadlGenerator;

    public WadlBuilder() {
        this(new WadlGeneratorJAXBGrammarGenerator());
    }

    public WadlBuilder(WadlGenerator wadlGenerator) {
        _wadlGenerator = wadlGenerator;
    }

    /**
     * Generate WADL for a set of resources.
     * @param resources the set of resources
     * @return the JAXB WADL application bean
     */
    public ApplicationDescription generate(List<org.glassfish.jersey.server.model.Resource> resources) {
        Application wadlApplication = _wadlGenerator.createApplication();
        Resources wadlResources = _wadlGenerator.createResources();

        // for each resource
        for (org.glassfish.jersey.server.model.Resource r : resources) {
            Resource wadlResource = generateResource(r, null);
            wadlResources.getResource().add(wadlResource);
        }
        wadlApplication.getResources().add(wadlResources);

        addVersion(wadlApplication);
        
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
     * @param resource the resource
     * @param description the overall application description so we can
     * @return the JAXB WADL application bean
     */
    public Application generate(
            ApplicationDescription description,
            org.glassfish.jersey.server.model.Resource resource) {
        Application wadlApplication = _wadlGenerator.createApplication();
        Resources wadlResources = _wadlGenerator.createResources();
        Resource wadlResource = generateResource(resource, null);
        wadlResources.getResource().add(wadlResource);
        wadlApplication.getResources().add(wadlResources);

        addVersion(wadlApplication);
        
        // Attach the data to the parts of the model
        
        _wadlGenerator.attachTypes(description);
        
        // Return the WADL
        
        return wadlApplication;
    }

    /**
     * Generate WADL for a virtual subresource resulting from sub resource
     * methods.
     * @param description the overall application description so we can
     * @param resource the parent resource
     * @param path the value of the methods path annotations
     * @return the JAXB WADL application bean
     */
    public Application generate(
            ApplicationDescription description,
            org.glassfish.jersey.server.model.Resource resource, String path) {
        Application wadlApplication = _wadlGenerator.createApplication();
        Resources wadlResources = _wadlGenerator.createResources();
        Resource wadlResource = generateSubResource(resource, path);
        wadlResources.getResource().add(wadlResource);
        wadlApplication.getResources().add(wadlResources);

        addVersion(wadlApplication);
        
        // Attach the data to the parts of the model
        _wadlGenerator.attachTypes(description);
        
        // Return the WADL
        return wadlApplication;
    }

    private void addVersion(Application wadlApplication) {
        // Include Jersey version as doc element with generatedBy attribute
        Doc d = new Doc();
        d.getOtherAttributes().put(new QName("http://jersey.java.net/", "generatedBy", "jersey"),
                Version.getBuildId());
        wadlApplication.getDoc().add(0, d);
    }

    private com.sun.research.ws.wadl.Method generateMethod(org.glassfish.jersey.server.model.Resource r,
                                                           final Map<String, Param> wadlResourceParams,
                                                           final org.glassfish.jersey.server.model.ResourceMethod m) {
        com.sun.research.ws.wadl.Method wadlMethod = _wadlGenerator.createMethod(r, m);
        // generate the request part
        Request wadlRequest = generateRequest(r, m, wadlResourceParams);
        if (wadlRequest != null) {
            wadlMethod.setRequest(wadlRequest);
        }
        // generate the response part
        final List<Response> responses = generateResponses(r, m);
        if(responses != null) {
            wadlMethod.getResponse().addAll(responses);
        }
        return wadlMethod;
    }

    private Request generateRequest(org.glassfish.jersey.server.model.Resource r,
                                    final org.glassfish.jersey.server.model.ResourceMethod m,
                                    Map<String, Param> wadlResourceParams) {
        if (m.getInvocable().getParameters().isEmpty()) {
            return null;
        }

        Request wadlRequest = _wadlGenerator.createRequest(r, m);

        for (Parameter p : m.getInvocable().getParameters()) {
            if (p.getSource() == Parameter.Source.ENTITY) {
                for (MediaType mediaType : m.getConsumedTypes()) {
                    setRepresentationForMediaType(r, m, mediaType, wadlRequest);
                }
            } else if (p.getAnnotation().annotationType() == FormParam.class) {
                // Use application/x-www-form-urlencoded if no @Consumes
                List<MediaType> supportedInputTypes = m.getConsumedTypes();
                if (supportedInputTypes.isEmpty()
                        || (supportedInputTypes.size() == 1 && supportedInputTypes.get(0).isWildcardType())) {
                    supportedInputTypes = Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
                }

                for (MediaType mediaType : supportedInputTypes) {
                    final Representation wadlRepresentation = setRepresentationForMediaType(r, m, mediaType, wadlRequest);
                    if (getParamByName(wadlRepresentation.getParam(), p.getSourceName()) == null) {
                        final Param wadlParam = generateParam(r, m, p);
                        if (wadlParam != null) {
                            wadlRepresentation.getParam().add(wadlParam);
                        }
                    }
                }
            } else if (p.getAnnotation().annotationType().getName().equals("org.glassfish.jersey.media.multipart.FormDataParam")) { // jersey-multipart support
                // Use multipart/form-data if no @Consumes
                List<MediaType> supportedInputTypes = m.getConsumedTypes();
                if (supportedInputTypes.isEmpty()
                        || (supportedInputTypes.size() == 1 && supportedInputTypes.get(0).isWildcardType())) {
                    supportedInputTypes = Collections.singletonList(MediaType.MULTIPART_FORM_DATA_TYPE);
                }

                for (MediaType mediaType : supportedInputTypes) {
                    final Representation wadlRepresentation = setRepresentationForMediaType(r, m, mediaType, wadlRequest);
                    if (getParamByName(wadlRepresentation.getParam(), p.getSourceName()) == null) {
                        final Param wadlParam = generateParam(r, m, p);
                        if (wadlParam != null) {
                            wadlRepresentation.getParam().add(wadlParam);
                        }
                    }
                }
            } else {
                Param wadlParam = generateParam(r, m, p);
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
        if (wadlRequest.getRepresentation().size() + wadlRequest.getParam().size() == 0) {
            return null;
        } else {
            return wadlRequest;
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
     * Create the wadl {@link Representation} for the specified {@link MediaType} if not yet
     * existing for the wadl {@link Request} and return it.
     * @param r the resource
     * @param m the resource method
     * @param mediaType an accepted media type of the resource method
     * @param wadlRequest the wadl request the wadl representation is to be created for (if not yet existing).
     * @author Martin Grotzke
     * @return the wadl request representation for the specified {@link MediaType}.
     */
    private Representation setRepresentationForMediaType(org.glassfish.jersey.server.model.Resource r,
                                                             final org.glassfish.jersey.server.model.ResourceMethod m,
                                                             MediaType mediaType,
                                                             Request wadlRequest) {
        Representation wadlRepresentation = getRepresentationByMediaType(wadlRequest.getRepresentation(), mediaType);
        if (wadlRepresentation == null) {
            wadlRepresentation = _wadlGenerator.createRequestRepresentation(r, m, mediaType);
            wadlRequest.getRepresentation().add(wadlRepresentation);
        }
        return wadlRepresentation;
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

    private Param generateParam(org.glassfish.jersey.server.model.Resource r, org.glassfish.jersey.server.model.ResourceMethod m, final Parameter p) {
        if (p.getSource() == Parameter.Source.ENTITY || p.getSource() == Parameter.Source.CONTEXT) {
            return null;
        }
        return _wadlGenerator.createParam(r, m, p);
    }

    private Resource generateResource(org.glassfish.jersey.server.model.Resource r, String path) {
        return generateResource(r, path, Collections.<org.glassfish.jersey.server.model.Resource>emptySet());
    }

    private Resource generateResource(org.glassfish.jersey.server.model.Resource r, String path, Set<org.glassfish.jersey.server.model.Resource> visitedResources) {
        Resource wadlResource = _wadlGenerator.createResource(r, path);

        // prevent infinite recursion
        if (visitedResources.contains(r)) {
            return wadlResource;
        } else {
            visitedResources = new HashSet<org.glassfish.jersey.server.model.Resource>(visitedResources);
            visitedResources.add(r);
        }

        Map<String, Param> wadlResourceParams = new HashMap<String, Param>();

        // add resource field/setter parameters that are associated with the resource PATH template

        List<Parameterized> fieldsOrSetters = new LinkedList<Parameterized>();

//        if (r.getFields() != null) {
//            fieldsOrSetters.addAll(r.getFields());
//        }
//        if (r.getSetterMethods() != null) {
//            fieldsOrSetters.addAll(r.getSetterMethods());
//        }

        for (Parameterized f : fieldsOrSetters) {
            for (Parameter fp : f.getParameters()) {
                Param wadlParam = generateParam(r, null, fp);
                if (wadlParam != null) {
                    wadlResource.getParam().add(wadlParam);
                }
            }
        }
        // for each resource method
        for (org.glassfish.jersey.server.model.ResourceMethod m : r.getResourceMethods()) {
            com.sun.research.ws.wadl.Method wadlMethod = generateMethod(r, wadlResourceParams, m);
            wadlResource.getMethodOrResource().add(wadlMethod);
        }
        // add method parameters that are associated with the resource PATH template
        for (Param wadlParam : wadlResourceParams.values()) {
            wadlResource.getParam().add(wadlParam);
        }

        // for each sub-resource method
        Map<String, Resource> wadlSubResources = new HashMap<String, Resource>();
        Map<String, Map<String, Param>> wadlSubResourcesParams =
                new HashMap<String, Map<String, Param>>();
        for (ResourceMethod subResourceMethod : r.getSubResourceMethods()) {
            // find or create sub resource for uri template
            String template = subResourceMethod.getPath();
            Resource wadlSubResource = wadlSubResources.get(template);
            Map<String, Param> wadlSubResourceParams = wadlSubResourcesParams.get(template);
            if (wadlSubResource == null) {
                wadlSubResource = new Resource();
                wadlSubResource.setPath(template);
                wadlSubResources.put(template, wadlSubResource);
                wadlSubResourceParams = new HashMap<String, Param>();
                wadlSubResourcesParams.put(template, wadlSubResourceParams);
                wadlResource.getMethodOrResource().add(wadlSubResource);
            }
            com.sun.research.ws.wadl.Method wadlMethod = generateMethod(r, wadlSubResourceParams, subResourceMethod);
            wadlSubResource.getMethodOrResource().add(wadlMethod);
        }
        // add parameters that are associated with each sub-resource method PATH template
        for (Map.Entry<String, Resource> e : wadlSubResources.entrySet()) {
            String template = e.getKey();
            Resource wadlSubResource = e.getValue();
            Map<String, Param> wadlSubResourceParams = wadlSubResourcesParams.get(template);
            for (Param wadlParam : wadlSubResourceParams.values()) {
                wadlSubResource.getParam().add(wadlParam);
            }
        }

        // for each sub resource locator
        for (ResourceMethod l : r.getSubResourceLocators()) {

            org.glassfish.jersey.server.model.Resource subResource =
                    org.glassfish.jersey.server.model.Resource.builder(l.getInvocable().getRawResponseType(), null).build();
            Resource wadlSubResource = generateResource(subResource,
                    l.getPath(), visitedResources);
            wadlResource.getMethodOrResource().add(wadlSubResource);

            for (Parameter p : l.getInvocable().getParameters()) {
                Param wadlParam = generateParam(r, l, p);
                if (wadlParam != null && wadlParam.getStyle() == ParamStyle.TEMPLATE) {
                    wadlSubResource.getParam().add(wadlParam);
                }
            }
        }
        return wadlResource;
    }

    private Resource generateSubResource(org.glassfish.jersey.server.model.Resource r, String path) {
        Resource wadlResource = new Resource();
        if (r.isRootResource()) {
            StringBuilder b = new StringBuilder(r.getPath());
            if (!(r.getPath().endsWith("/") || path.startsWith("/"))) {
                b.append("/");
            }
            b.append(path);
            wadlResource.setPath(b.toString());
        }
        // for each sub-resource method
        Map<String, Param> wadlSubResourceParams = new HashMap<String, Param>();
        for (ResourceMethod m : r.getSubResourceMethods()) {
            // find or create sub resource for uri template
            String template = m.getPath();
            if (!template.equals(path)) {
                continue;
            }
            com.sun.research.ws.wadl.Method wadlMethod = generateMethod(r, wadlSubResourceParams, m);
            wadlResource.getMethodOrResource().add(wadlMethod);
        }
        // add parameters that are associated with each sub-resource method PATH template
        for (Param wadlParam : wadlSubResourceParams.values()) {
            wadlResource.getParam().add(wadlParam);
        }

        return wadlResource;
    }

    private List<Response> generateResponses(org.glassfish.jersey.server.model.Resource r, final ResourceMethod m) {
        if (m.getInvocable().getRawResponseType() == void.class) {
            return null;
        }
        return _wadlGenerator.createResponses(r, m);
    }
}
