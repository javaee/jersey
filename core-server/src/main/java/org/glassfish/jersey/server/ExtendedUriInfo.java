/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.util.List;
import java.util.regex.MatchResult;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.uri.UriTemplate;

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
     * Get a read-only list of {@link org.glassfish.jersey.uri.UriTemplate} for matched resources.
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

    /**
     * Return all matched {@link Resource model resources} including child resources. The list is ordered so that the
     * {@link Resource resource} currently being processed is the first element in the list.
     *
     * <p/>
     * The following example
     * <pre>&#064;Path("foo")
     * public class FooResource {
     *  &#064;GET
     *  public String getFoo() {...}
     *
     *  &#064;Path("bar")
     *  public BarResource getBarResource() {...}
     * }
     *
     * public class BarResource {
     *  &#064;GET
     *  public String getBar() {...}
     * }
     * </pre>
     *
     * <p>The values returned by this method based on request uri and where
     * the method is called from are:</p>
     *
     * <table border="1">
     * <tr>
     * <th>Request</th>
     * <th>Called from</th>
     * <th>Value(s)</th>
     * </tr>
     * <tr>
     * <td>GET /foo</td>
     * <td>FooResource.getFoo</td>
     * <td>Resource["foo"]</td>
     * </tr>
     * <tr>
     * <td>GET /foo/bar</td>
     * <td>FooResource.getBarResource</td>
     * <td>Child-Resource["foo/bar"], Resource["foo"]</td>
     * </tr>
     * <tr>
     * <td>GET /foo/bar</td>
     * <td>BarResource.getBar</td>
     * <td>Resource[no path; based on BarResource.class], Child-Resource["foo/bar"], Resource["foo"]</td>
     * </tr>
     * </table>

     * @return List of resources and child resources that were processed during request matching.
     */
    public List<Resource> getMatchedAllModelResources();

    /**
     * Return all matched {@link Resource model resources} except child resources. The result list is the same as result from
     * method {@link #getMatchedAllModelResources()} except it does not contain child resources.
     * @return List of resources that were processed during request matching.
     */
    public List<Resource> getMatchedModelResources();


    /**
     * Return a last matched {@link Resource model resource}. Returned resource is the resource which contains the resource
     * method or resource locator from which {@link #getMatchedModelResource()} is executed. Returned resource is not a
     * child resource; if the method is executed from child resource then the resource containing this child resource is returned.
     *
     * @return Last matched model resource.
     */
    public Resource getMatchedModelResource();

    /**
     * Return a last matched {@link Resource model child resource}. Returned child resource resource is the child resource which
     * contains the resource method or resource locator from which {@link #getMatchedChildModelResource()} is executed.
     * If the method is not executed from child resource null is returned.
     *
     * @return Last matched model child resource if the method is executed from child resource; false otherwise.
     */
    public Resource getMatchedChildModelResource();
}
