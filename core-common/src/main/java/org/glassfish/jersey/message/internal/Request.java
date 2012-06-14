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
package org.glassfish.jersey.message.internal;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.Cookie;

import org.glassfish.jersey._remove.RequestBuilder;
import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 *
 * @author Paul Sandoz
 */
interface Request extends Entity, Headers {

    interface Builder extends Request, Entity.Builder<Request.Builder>, Headers.Builder<Request.Builder> {
        // Common message methods

        public Builder properties(Map<String, Object> properties);

        public Builder property(String name, Object value);

        public Builder clearProperties();

        // Request-specific methods
        public Builder uri(String uri);

        public Builder uri(URI uri);

        public Builder uris(String applicationRootUri, String requestUri);

        public Builder uris(URI applicationRootUri, URI requestUri);

        public Builder method(String method);

        public Builder workers(MessageBodyWorkers workers);

        public Builder cookie(Cookie cookie);

        @Override
        public Builder clone();

        public Request build();

        public RequestBuilder toJaxrsRequestBuilder();
    }

    // Common message methods
    public Map<String, Object> properties();

    public void close();

    // Request-specific methods
    public Request clone();

    public URI baseUri();

    /**
     * Get the path of the current request relative to the application root (base)
     * URI as a string.
     *
     * @param decode controls whether sequences of escaped octets are decoded
     *     ({@code true}) or not ({@code false}).
     * @return relative request path.
     */
    public String relativePath(boolean decode);

    public URI uri();

    public String method();

    public MessageBodyWorkers workers();

    public javax.ws.rs.core.Request toJaxrsRequest();

    public JaxrsRequestHeadersView getJaxrsHeaders();

    public Builder toBuilder();
}
