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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import javax.xml.namespace.QName;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.ParamStyle;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * This WadlGenerator creates the basic wadl artifacts.<br>
 * Created on: Jun 16, 2008<br>
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 * @author Miroslav Fuksa
 */
public class WadlGeneratorImpl implements WadlGenerator {

    @Override
    public String getRequiredJaxbContextPath() {
        final String name = Application.class.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    @Override
    public void init() {
    }

    @Override
    public void setWadlGeneratorDelegate(WadlGenerator delegate) {
        throw new UnsupportedOperationException("No delegate supported.");
    }

    @Override
    public Resources createResources() {
        return new Resources();
    }

    @Override
    public Application createApplication() {
        return new Application();
    }

    @Override
    public com.sun.research.ws.wadl.Method createMethod(
            org.glassfish.jersey.server.model.Resource r, final ResourceMethod m) {
        com.sun.research.ws.wadl.Method wadlMethod =
                new com.sun.research.ws.wadl.Method();
        wadlMethod.setName(m.getHttpMethod());
        wadlMethod.setId(m.getInvocable().getDefinitionMethod().getName());
        if (m.isExtended()) {
            wadlMethod.getAny().add(WadlApplicationContextImpl.EXTENDED_ELEMENT);
        }
        return wadlMethod;
    }

    @Override
    public Representation createRequestRepresentation(org.glassfish.jersey.server.model.Resource r,
                                                      ResourceMethod m, MediaType mediaType) {
        Representation wadlRepresentation = new Representation();
        wadlRepresentation.setMediaType(mediaType.toString());
        return wadlRepresentation;
    }

    @Override
    public Request createRequest(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
        return new Request();
    }

    @Override
    public Param createParam(org.glassfish.jersey.server.model.Resource r, ResourceMethod m, final Parameter p) {

        if (p.getSource() == Parameter.Source.UNKNOWN) {
            return null;
        }

        Param wadlParam = new Param();
        wadlParam.setName(p.getSourceName());

        switch (p.getSource()) {
            case FORM:
                wadlParam.setStyle(ParamStyle.QUERY);
                break;
            case QUERY:
                wadlParam.setStyle(ParamStyle.QUERY);
                break;
            case MATRIX:
                wadlParam.setStyle(ParamStyle.MATRIX);
                break;
            case PATH:
                wadlParam.setStyle(ParamStyle.TEMPLATE);
                break;
            case HEADER:
                wadlParam.setStyle(ParamStyle.HEADER);
                break;
            case COOKIE:
                // Generates name="Cookie" path="<name>"
                wadlParam.setStyle(ParamStyle.HEADER);
                wadlParam.setName("Cookie");
                wadlParam.setPath(p.getSourceName());
                break;
            default:
                break;
        }

        if (p.hasDefaultValue()) {
            wadlParam.setDefault(p.getDefaultValue());
        }
        Class<?> pClass = p.getRawType();
        if (pClass.isArray()) {
            wadlParam.setRepeating(true);
            pClass = pClass.getComponentType();
        }
        if (pClass.equals(int.class) || pClass.equals(Integer.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "int", "xs"));
        } else if (pClass.equals(boolean.class) || pClass.equals(Boolean.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "boolean", "xs"));
        } else if (pClass.equals(long.class) || pClass.equals(Long.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "long", "xs"));
        } else if (pClass.equals(short.class) || pClass.equals(Short.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "short", "xs"));
        } else if (pClass.equals(byte.class) || pClass.equals(Byte.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "byte", "xs"));
        } else if (pClass.equals(float.class) || pClass.equals(Float.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "float", "xs"));
        } else if (pClass.equals(double.class) || pClass.equals(Double.class)) {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "double", "xs"));
        } else {
            wadlParam.setType(new QName("http://www.w3.org/2001/XMLSchema", "string", "xs"));
        }
        return wadlParam;
    }

    @Override
    public Resource createResource(org.glassfish.jersey.server.model.Resource resource, String path) {
        Resource wadlResource = new Resource();
        if (path != null) {
            wadlResource.setPath(path);
        } else if (resource.getPath() != null) {
            wadlResource.setPath(resource.getPath());
        }

        if (resource.isExtended()) {

            wadlResource.getAny().add(WadlApplicationContextImpl.EXTENDED_ELEMENT);
        }

        return wadlResource;
    }

    @Override
    public List<Response> createResponses(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
        final Response response = new Response();

        // add mediaType="*/*" in case that no mediaType was specified
        if (hasEmptyProducibleMediaTypeSet(m)) {
            Representation wadlRepresentation = createResponseRepresentation(r, m, MediaType.WILDCARD_TYPE);
            response.getRepresentation().add(wadlRepresentation);
        } else {
            for (MediaType mediaType : m.getProducedTypes()) {
                Representation wadlRepresentation = createResponseRepresentation(r, m, mediaType);
                response.getRepresentation().add(wadlRepresentation);
            }
        }

        List<Response> responses = new ArrayList<Response>();
        responses.add(response);
        return responses;
    }

    private boolean hasEmptyProducibleMediaTypeSet(final ResourceMethod method) {
        return method.getProducedTypes().isEmpty();
    }

    public Representation createResponseRepresentation(org.glassfish.jersey.server.model.Resource r, ResourceMethod m,
                                                       MediaType mediaType) {
        Representation wadlRepresentation = new Representation();
        wadlRepresentation.setMediaType(mediaType.toString());
        return wadlRepresentation;
    }

    // ================ methods for post build actions =======================

    @Override
    public ExternalGrammarDefinition createExternalGrammar() {
        // Return an empty list to add to
        return new ExternalGrammarDefinition();
    }

    @Override
    public void attachTypes(ApplicationDescription egd) {
    }
}
