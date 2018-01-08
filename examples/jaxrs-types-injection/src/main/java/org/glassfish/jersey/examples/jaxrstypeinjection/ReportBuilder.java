/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.jaxrstypeinjection;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * Provides functionality for appending values of JAX-RS types to a string-based
 * report.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class ReportBuilder {

    private ReportBuilder() {
    }

    public static StringBuilder append(StringBuilder sb, UriInfo uriInfo, HttpHeaders httpHeaders) {
        sb.append("\n UriInfo:");
        sb.append("\n   Absolute path : ").append(uriInfo.getAbsolutePath());
        sb.append("\n   Base URI : ").append(uriInfo.getBaseUri());
        sb.append("\n   Mathced resources : ").append(uriInfo.getMatchedResources().toString());
        sb.append("\n   Matched URIs : ").append(uriInfo.getMatchedURIs().toString());
        sb.append("\n   Path : ").append(uriInfo.getPath());
        sb.append("\n   Path parameters:\n");
        dumpMultivaluedMap(sb, uriInfo.getPathParameters());
        sb.append("   Path segments : ").append(uriInfo.getPathSegments().toString());
        sb.append("\n   Query parameters:\n");
        dumpMultivaluedMap(sb, uriInfo.getQueryParameters());
        sb.append("   Request URI : ").append(uriInfo.getRequestUri());
        sb.append("\n\n HttpHeaders:\n");
        dumpMultivaluedMap(sb, httpHeaders.getRequestHeaders());
        return sb;
    }

    public static void dumpMultivaluedMap(StringBuilder sb, MultivaluedMap<String, String> map) {
        if (map == null) {
            sb.append("     [ null ]\n");
            return;
        }
        for (Map.Entry<String, List<String>> headerEntry : map.entrySet()) {
            sb.append("     ").append(headerEntry.getKey()).append(" : ");
            final Iterator<String> valueIterator = headerEntry.getValue().iterator();
            sb.append(valueIterator.next());
            while (valueIterator.hasNext()) {
                sb.append(", ").append(valueIterator.next());
            }
            sb.append('\n');
        }
    }
}
