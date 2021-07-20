/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.httptrace;

import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.server.ContainerRequest;

/**
 * Request stringifier.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class Stringifier {

    private Stringifier() {
    }

    public static String stringify(ContainerRequest request) {
        StringBuilder buffer = new StringBuilder();

        printRequestLine(buffer, request);
        printPrefixedHeaders(buffer, request.getHeaders());

        if (request.hasEntity()) {
            buffer.append(request.readEntity(String.class)).append("\n");
        }

        return buffer.toString();
    }

    private static void printRequestLine(StringBuilder buffer, ContainerRequest request) {
        buffer.append(request.getMethod()).append(" ").append(request.getUriInfo().getRequestUri().toASCIIString()).append("\n");
    }

    private static void printPrefixedHeaders(StringBuilder buffer, Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            List<String> val = e.getValue();
            String header = e.getKey();

            if (val.size() == 1) {
                buffer.append(header).append(": ").append(val.get(0)).append("\n");
            } else {
                StringBuilder sb = new StringBuilder();
                boolean add = false;
                for (String s : val) {
                    if (add) {
                        sb.append(',');
                    }
                    add = true;
                    sb.append(s);
                }
                buffer.append(header).append(": ").append(sb.toString()).append("\n");
            }
        }
    }
}
