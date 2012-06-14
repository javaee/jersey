/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey._remove;

import java.io.IOException;

/**
 * <p>Interface implemented by filters invoked at the <i>PreMatch</i>
 * extension point. Use a filter of this type to update the input to the
 * JAX-RS matching algorithm, e.g., the HTTP method, Accept header, etc.
 * Otherwise, the use of a filter invoked at the <i>Pre</i> extension
 * point (after resource matching) is recommended.</p>
 *
 * <p>Filters implementing
 * this interface MUST be annotated with {@link javax.ws.rs.ext.Provider}.
 * This type of filters is supported only as part of the Server API.</p>
 *
 * @author Santiago Pericas-Geertsen
 * @since 2.0
 * @see RequestFilter
 */
public interface PreMatchRequestFilter {

    /**
     * <p>Filter method called at the <i>PreMatch</i> extension point.
     * I.e., before resource matching as part of the Server API. This type
     * of filters are not supported in the Client API.</p>
     *
     * <p>Filters in a chain are ordered according to their binding
     * priority (see {@link javax.ws.rs.BindingPriority}). If a pre-match
     * request filter produces a response by calling
     * {@link FilterContext#setResponse}, the execution of the pre-match request
     * chain is stopped and the response is returned without matching a resource
     * method. For example, a caching filter may produce a response in this way.
     * Note that responses produced in this manner are still processed by
     * the response filter chain.</p>
     *
     * @param context invocation context.
     * @throws IOException if an I/O exception occurs.
     */
    void preMatchFilter(FilterContext context) throws IOException;
}
