/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import java.util.List;
import java.util.regex.MatchResult;

import javax.ws.rs.container.ResourceInfo;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.process.RequestProcessingContext;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.RuntimeResource;
import org.glassfish.jersey.uri.UriTemplate;

/**
 * Jersey request matching and routing context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public interface RoutingContext extends ResourceInfo {

    /**
     * Push the result of the successful request URI routing pattern match.
     *
     * @param matchResult successful request URI routing pattern
     *                    {@link java.util.regex.MatchResult match result}.
     */
    public void pushMatchResult(MatchResult matchResult);

    /**
     * Push the resource that matched the request URI.
     *
     * @param resource instance of the resource that matched the request URI.
     */
    public void pushMatchedResource(Object resource);

    /**
     * Peek the last resource object that successfully matched the request URI.
     *
     * @return last resource matched as previously set by {@link #pushMatchedResource}
     */
    public Object peekMatchedResource();

    /**
     * Peek at the last successful request URI routing pattern
     * {@link java.util.regex.MatchResult match result}.
     *
     * @return last successful request URI routing pattern match result.
     */
    // TODO: consider removing unused method
    public MatchResult peekMatchResult();

    /**
     * Push matched request URI routing pattern {@link org.glassfish.jersey.uri.UriTemplate templates}
     * for a single matched method.
     * <p>
     * In case only a single path matching has been performed on the resource (in case of resource methods,
     * only the resource path is matched), the method template should be passed as {@code null}.
     * In case a path matching has been performed on both a resource and method paths
     * (in case of sub-resource methods and locators), both templates (resource and method) must be specified.
     * </p>
     *
     * @param resourceTemplate resource URI template that should be pushed.
     * @param methodTemplate   (sub-resource) method or locator URI template that should be pushed.
     */
    public void pushTemplates(UriTemplate resourceTemplate, UriTemplate methodTemplate);

    /**
     * Get the final matching group of the last successful request URI routing
     * pattern {@link java.util.regex.MatchResult match result}. Also known as right-hand path.
     * <p>
     * May be empty but is never {@code null}.
     * </p>
     *
     * @return final matching group of the last successful request URI routing
     *         pattern match result.
     */
    public String getFinalMatchingGroup();

    /**
     * Get a read-only list of {@link java.util.regex.MatchResult match results} for matched
     * request URI routing patterns. Entries are ordered in reverse request URI
     * matching order, with the root request URI routing pattern match result
     * last.
     *
     * @return a read-only reverse list of request URI routing pattern match
     *         results.
     */
    // TODO: consider removing unused method
    public List<MatchResult> getMatchedResults();

    /**
     * Add currently matched left-hand side part of request path to the list of
     * matched paths returned by {@link javax.ws.rs.core.UriInfo#getMatchedURIs()}.
     * <p/>
     * Left-hand side request path is the request path excluding the suffix
     * part of the path matched by the {@link #getFinalMatchingGroup() final
     * matching group} of the last successful request URI routing pattern.
     */
    public void pushLeftHandPath();

    /**
     * Set the matched request to response inflector.
     * <p/>
     * This method can be used in a non-terminal stage to set the inflector that
     * can be retrieved and processed by a subsequent stage.
     *
     * @param inflector matched request to response inflector.
     */
    public void setInflector(Inflector<RequestProcessingContext, ContainerResponse> inflector);

    /**
     * Get the matched request to response data inflector if present, or {@code null}
     * otherwise.
     *
     * @return matched request to response inflector, or {@code null} if not available.
     */
    public Inflector<RequestProcessingContext, ContainerResponse> getInflector();

    /**
     * Set the matched {@link ResourceMethod resource method}. This method needs to be called only if the method was
     * matched. This method should be called only for setting the final resource method and not for setting sub resource
     * locators invoked during matching.
     *
     * @param resourceMethod Resource method that was matched.
     */
    public void setMatchedResourceMethod(ResourceMethod resourceMethod);

    /**
     * Push the matched {@link ResourceMethod sub resource locator method}.
     *
     * @param resourceLocator Sub resource locator method.
     */
    public void pushMatchedLocator(ResourceMethod resourceLocator);

    /**
     * Push a matched {@link RuntimeResource runtime resource} that was visited during matching phase. This method must
     * be called for any matched runtime resource.
     *
     * @param runtimeResource Runtime resource that was matched during matching.
     */
    public void pushMatchedRuntimeResource(RuntimeResource runtimeResource);

    /**
     * Push {@link Resource sub resource} returned from a sub resource locator method. The pushed
     * {@code subResourceFromLocator} is the final model of a sub resource which is already enhanced by
     * {@link org.glassfish.jersey.server.model.ModelProcessor model processors} and
     * validated.
     *
     * @param subResourceFromLocator Resource constructed from result of sub resource locator method.
     */
    public void pushLocatorSubResource(Resource subResourceFromLocator);
}
