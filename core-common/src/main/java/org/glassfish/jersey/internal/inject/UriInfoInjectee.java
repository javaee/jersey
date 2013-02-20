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
package org.glassfish.jersey.internal.inject;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Proxiable wrapper for request scoped {@link UriInfo} instance.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class UriInfoInjectee implements UriInfo {

    private UriInfo wrapped;

    /**
     * Set wrapped instance. Should be invoked on each incoming request,
     * when a new injectee instance is created by HK2.
     *
     * @param uriInfo actual uri info.
     */
    public void set(final UriInfo uriInfo) {
        if (wrapped != null) {
            throw new IllegalStateException(LocalizationMessages.URI_INFO_WAS_ALREADY_SET());
        }
        this.wrapped = uriInfo;
    }

    @Override
    public URI getRequestUri() {
        checkStatus();
        return wrapped.getRequestUri();
    }

    @Override
    public String getPath() {
        checkStatus();
        return wrapped.getPath();
    }

    @Override
    public String getPath(boolean decode) {
        checkStatus();
        return wrapped.getPath(decode);
    }

    @Override
    public List<PathSegment> getPathSegments() {
        checkStatus();
        return wrapped.getPathSegments();
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        checkStatus();
        return wrapped.getPathSegments(decode);
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        checkStatus();
        return wrapped.getRequestUriBuilder();
    }

    @Override
    public URI getAbsolutePath() {
        checkStatus();
        return wrapped.getAbsolutePath();
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        checkStatus();
        return wrapped.getAbsolutePathBuilder();
    }

    @Override
    public URI getBaseUri() {
        checkStatus();
        return wrapped.getBaseUri();
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        checkStatus();
        return wrapped.getBaseUriBuilder();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        checkStatus();
        return wrapped.getPathParameters();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        checkStatus();
        return wrapped.getPathParameters(decode);
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        checkStatus();
        return wrapped.getQueryParameters();
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        checkStatus();
        return wrapped.getQueryParameters(decode);
    }

    @Override
    public List<String> getMatchedURIs() {
        checkStatus();
        return wrapped.getMatchedURIs();
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        checkStatus();
        return wrapped.getMatchedURIs(decode);
    }

    @Override
    public List<Object> getMatchedResources() {
        checkStatus();
        return wrapped.getMatchedResources();
    }

    @Override
    public URI resolve(URI uri) {
        checkStatus();
        return wrapped.resolve(uri);
    }

    @Override
    public URI relativize(URI uri) {
        checkStatus();
        return wrapped.relativize(uri);
    }

    private void checkStatus() throws IllegalStateException {
        if (wrapped == null) {
            throw new IllegalStateException(LocalizationMessages.URI_INFO_WAS_NOT_SET());
        }
    }
}
