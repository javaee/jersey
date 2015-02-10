/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response.Status;

import javax.annotation.Priority;

/**
 * Simple server-side request filter that implements CSRF protection as per the
 * <a href="http://www.nsa.gov/ia/_files/support/guidelines_implementation_rest.pdf">Guidelines for Implementation of REST</a>
 * by NSA (section IV.F) and
 * section 4.3 of <a href="http://seclab.stanford.edu/websec/csrf/csrf.pdf">this paper</a>.
 * If you add it to the request filters of your application, it will check for X-Requested-By header in each
 * request except for those that don't change state (GET, OPTIONS, HEAD). If the header is not found,
 * it returns {@link Status#BAD_REQUEST} response back to the client.
 *
 * @see org.glassfish.jersey.client.filter.CsrfProtectionFilter
 *
 * @author Martin Matula
 */
@Priority(Priorities.AUTHENTICATION) // should be one of the first post-matching filters to get executed
public class CsrfProtectionFilter implements ContainerRequestFilter {

    /**
     * Name of the header this filter will attach to the request.
     */
    public static final String HEADER_NAME = "X-Requested-By";

    private static final Set<String> METHODS_TO_IGNORE;
    static {
        HashSet<String> mti = new HashSet<>();
        mti.add("GET");
        mti.add("OPTIONS");
        mti.add("HEAD");
        METHODS_TO_IGNORE = Collections.unmodifiableSet(mti);
    }

    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        if (!METHODS_TO_IGNORE.contains(rc.getMethod()) && !rc.getHeaders().containsKey(HEADER_NAME)) {
            throw new BadRequestException();
        }
    }
}
