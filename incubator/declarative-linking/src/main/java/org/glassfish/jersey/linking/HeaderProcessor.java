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

package org.glassfish.jersey.linking;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;

/**
 * Processes @Link and @LinkHeaders annotations on entity classes and
 * adds appropriate HTTP Link headers.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class HeaderProcessor<T> {

    private EntityDescriptor instanceDescriptor;

    HeaderProcessor(Class<T> c) {
        instanceDescriptor = EntityDescriptor.getInstance(c);
    }

    /**
     * Process any {@link InjectLink} annotations on the supplied entity.
     * @param entity the entity object returned by the resource method
     * @param uriInfo the uriInfo for the request
     * @param headers the map into which the headers will be added
     */
    void processLinkHeaders(T entity,
                            UriInfo uriInfo,
                            ResourceMappingContext rmc,
                            MultivaluedMap<String, Object> headers) {
        List<String> headerValues = getLinkHeaderValues(entity, uriInfo, rmc);
        for (String headerValue : headerValues) {
            headers.add("Link", headerValue);
        }
    }

    List<String> getLinkHeaderValues(Object entity, UriInfo uriInfo, ResourceMappingContext rmc) {
        final List<Object> matchedResources = uriInfo.getMatchedResources();

        if (!matchedResources.isEmpty()) {
            final Object resource = matchedResources.get(0);
            final List<String> headerValues = new ArrayList<>();

            for (LinkHeaderDescriptor desc : instanceDescriptor.getLinkHeaders()) {
                if (ELLinkBuilder.evaluateCondition(desc.getCondition(), entity, resource, entity)) {
                    String headerValue = getLinkHeaderValue(desc, entity, resource, uriInfo, rmc);
                    headerValues.add(headerValue);
                }
            }
            return headerValues;
        }

        return Collections.emptyList();
    }

    private static String getLinkHeaderValue(LinkHeaderDescriptor desc, Object entity, Object resource, UriInfo uriInfo,
                                             ResourceMappingContext rmc) {
        URI uri = ELLinkBuilder.buildURI(desc, entity, resource, entity, uriInfo, rmc);
        InjectLink link = desc.getLinkHeader();
        return InjectLink.Util.buildLinkFromUri(uri, link).toString();
    }

}
