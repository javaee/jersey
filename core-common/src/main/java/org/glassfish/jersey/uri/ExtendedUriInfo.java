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
package org.glassfish.jersey.uri;

import java.util.List;
import java.util.regex.MatchResult;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

/**
 * Extensions to {@link UriInfo}.
 *
 * @author Paul Sandoz
 */
public interface ExtendedUriInfo extends UriInfo {

    /**
     * Get get matched resource method that was invoked.
     *
     * @return the matched resource method, otherwise null if no resource
     *         method was invoked.
     */
    // todo: add support
    // ResourceMethod getMatchedMethod();

    /**
     * Get the throwable that was mapped to a response.
     * <p>
     * A response filter or a message body writer may utilize this method to
     * determine if a resource method was invoked but did not return a
     * response because an exception was thrown from the resource method, or
     * the resource method returned but a response filter threw an exception.
     *
     * @return the throwable that was mapped to a response, otherwise null
     *         if no throwable was mapped to a response.
     */
    Throwable getMappedThrowable();

    /**
     * Get a read-only list of {@link MatchResult} for matched resources.
     * Entries are ordered in reverse request URI matching order, with the
     * root resource match result last.
     *
     * @return a read-only list of match results for matched resources.
     */
    List<MatchResult> getMatchedResults();

    /**
     * Get a read-only list of {@link UriTemplate} for matched resources.
     * Each entry is a URI template that is the value of the
     * {@link javax.ws.rs.Path} that is a partial path that matched a resource
     * class, a sub-resource method or a sub-resource locator.
     * Entries are ordered in reverse request URI matching order, with the
     * root resource URI template last.
     *
     * @return a read-only list of URI templates for matched resources.
     */
    List<UriTemplate> getMatchedTemplates();

    /**
     * Get the path segments that contain a template variable.
     * All sequences of escaped octets are decoded,
     * equivalent to <code>getPathSegments(true)</code>.
     *
     * @param name the template variable name
     * @return the path segments, the list will be empty the matching path does
     *         not contain the template
     */
    List<PathSegment> getPathSegments(String name);

    /**
     * Get the path segments that contain a template variable.
     *
     * @param name the template variable name
     * @param decode controls whether sequences of escaped octets are decoded
     * (true) or not (false).
     * @return the path segments, the list will be empty the matching path does
     *         not contain the template
     */
    List<PathSegment> getPathSegments(String name, boolean decode);
}
