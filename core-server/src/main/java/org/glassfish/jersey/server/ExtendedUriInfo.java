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
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.uri.UriTemplate;

/**
 * Extensions to {@link UriInfo}.
 *
 * @author Paul Sandoz
 */
public interface ExtendedUriInfo extends UriInfo {

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
     * Return all matched {@link RuntimeResource runtime resources} including runtime resources
     * based on child resources. The list is ordered so that the {@link RuntimeResource runtime resource}
     * currently being processed is the first element in the list.
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
     * <td>RuntimeResource["/foo"]</td>
     * </tr>
     * <tr>
     * <td>GET /foo/bar</td>
     * <td>FooResource.getBarResource</td>
     * <td>RuntimeResource["foo/bar"], Resource["foo"]</td>
     * </tr>
     * <tr>
     * <td>GET /foo/bar</td>
     * <td>BarResource.getBar</td>
     * <td>RuntimeResource[no path; based on BarResource.class], RuntimeResource["foo/bar"], RuntimeResource["foo"]</td>
     * </tr>
     * </table>

     * @return List of resources and child resources that were processed during request matching.
     */
    public List<RuntimeResource> getMatchedRuntimeResources();

    /**
     * Get matched {@link ResourceMethod resource method} that is invoked.
     * <p/>
     * Note that sub resource locator is not not considered as a matched resource method and calling the method from
     * sub resource locator will therefore return null.
     *
     * @return The matched resource method that was invoked or null if no resource method was invoked.
     */
    public ResourceMethod getMatchedResourceMethod();

    /**
     * Get matched {@link Resource model resource} from which {@link #getMatchedResourceMethod() the matched} resource method
     * was invoked. The resource can also be a child if the matched method is a sub resource method.
     * <p/>
     * Note that method return only resource containing finally matched {@link ResourceMethod resource method}
     * and not intermediary processed resources (parent resources or resources containing sub resource locators).
     *
     * @return The matched model resource or null if no resource was matched.
     */
    public Resource getMatchedModelResource();

    /**
     * Get resource locators matched since beginning of a matching. The list contains resource
     * locators sorted in the order that the first element of the list is the last locator executed (LIFO ordering).
     * The method can be invoked from request scoped code. When method is invoked from the resource locator
     * itself such a locator will be already in the returned list as a first element.
     * <p>
     * The resource locator is
     * a {@link ResourceMethod resource method} which is annotated by the {@link javax.ws.rs.Path @Path}
     * and returns a sub resource.
     * <p/>
     * @return List of matched resource locators.
     */
    public List<ResourceMethod> getMatchedResourceLocators();

    /**
     * Get the list of sub resources returned from resource locators during matching.
     * The sub resources are sorted in the
     * order, so that the first element of the list is a sub resource that was lastly returned from the
     * resource locator (LIFO ordering).
     * <p/>
     * Sub resource is a resource that is returned from invoked resource locator method and that will be
     * used for further resource matching.
     *
     * @return Locator sub resource.
     */
    public List<Resource> getLocatorSubResources();
}
