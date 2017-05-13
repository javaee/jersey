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

package org.glassfish.jersey.tests.jaxrs.inject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

/**
 * Provider with {@link Context} injection points.
 */
@Provider
public class StringBeanEntityProviderWithInjectables extends StringBeanEntityProvider {

    @Context
    Application application;
    @Context
    UriInfo info;
    @Context
    Request request;
    @Context
    HttpHeaders headers;
    @Context
    SecurityContext security;
    @Context
    Providers providers;
    @Context
    ResourceContext resources;
    @Context
    Configuration configuration;

    /**
     * Chosen decimal as a representation to be more human readable
     */
    public static String computeMask(Application application, UriInfo info,
            Request request, HttpHeaders headers, SecurityContext security,
            Providers providers, ResourceContext resources,
            Configuration configuration) {
        int mask = 1;
        mask = 10 * mask + (application == null ? 0 : 1);
        mask = 10 * mask + (info == null ? 0 : 1);
        mask = 10 * mask + (request == null ? 0 : 1);
        mask = 10 * mask + (headers == null ? 0 : 1);
        mask = 10 * mask + (security == null ? 0 : 1);
        mask = 10 * mask + (providers == null ? 0 : 1);
        mask = 10 * mask + (resources == null ? 0 : 1);
        mask = 10 * mask + (configuration == null ? 0 : 1);
        return String.valueOf(mask);
    }

    /**
     * Here, the bitwise operation with mask variable would be more efficient,
     * but less human readable when sending over the link as a binary number.
     * Hence, sMask is supposed to be decimal number created by writeTo method.
     * <p>
     * If something has not been injected, and thus not written by writeTo, this
     * static method parses what it was not injected.
     */
    public static String notInjected(String sMask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i != sMask.length(); i++) {
            sb.append(notInjected(sMask, i));
        }
        return sb.toString();
    }

    /**
     * Here, the bitwise operation with mask variable would be more efficient,
     * but less human readable when sending over the link as a binary number.
     * Hence, sMask is supposed to be decimal number created by writeTo method.
     * <p>
     * If something has not been injected, and thus not written by writeTo, this
     * static method parses what it was not injected.
     */
    public static final String notInjected(String sMask, int index) {
        String[] labels = {
                "Application,", "UriInfo,", "Request,",
                "HttpHeaders,", "SecurityContext,", "Providers,",
                "ResourceContext", "Configuration"
        };
        String label = "";
        if (sMask.charAt(index) == '0') {
            label = labels[index - 1];
        }
        return label;
    }

    @Override
    public long getSize(StringBean t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return 9;
    }

    @Override
    public void writeTo(StringBean t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        entityStream.write(computeMask(application, info, request, headers,
                security, providers, resources, configuration).getBytes());
    }

    @Override
    public StringBean readFrom(Class<StringBean> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return new StringBean(computeMask(application, info, request, headers,
                security, providers, resources, configuration));
    }

}
