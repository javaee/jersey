/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.oauth1.signature;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Interface to be implemented as a wrapper around an HTTP request, so that
 * digital signature can be generated and/or verified.
 *
 * @author Paul C. Bryan <pbryan@sun.com>
 */
public interface OAuth1Request {

    /**
     * Returns the name of the HTTP method with which this request was made,
     * for example, GET, POST, or PUT.
     *
     * @return the name of the method with which this request was made.
     */
    public String getRequestMethod();

    /**
     * Returns the URL of the request, including protocol, server name,
     * optional port number, and server path.
     *
     * @return the request URL.
     */
    public URL getRequestURL();

    /**
     * Returns an {@link java.util.Set} of {@link String} objects containing the
     * names of the parameters contained in the request.
     *
     * @return the names of the parameters.
     */
    public Set<String> getParameterNames();

    /**
     * Returns an {@link java.util.List} of {@link String} objects containing the
     * values of the specified request parameter, or null if the parameter does
     * not exist. For HTTP requests, parameters are contained in the query
     * string and/or posted form data.
     *
     * @param name the name of the parameter.
     * @return the values of the parameter.
     */
    public List<String> getParameterValues(String name);

    /**
     * Returns the value(s) of the specified request header. If the request did
     * not include a header of the specified name, this method returns null.
     *
     * @param name the header name.
     * @return the value(s) of the requested header, or null if none exist.
     */
    public List<String> getHeaderValues(String name);

    /**
     * Adds a header with the given name and value.
     *
     * @param name the name of the header.
     * @param value the header value.
     * @throws IllegalStateException if this method cannot be implemented.
     */
    public void addHeaderValue(String name, String value) throws IllegalStateException;
}
