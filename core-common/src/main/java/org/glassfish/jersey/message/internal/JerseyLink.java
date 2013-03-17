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
package org.glassfish.jersey.message.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.uri.UriTemplate;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

/**
 * Jersey implementation of {@link javax.ws.rs.core.Link JAX-RS Link} contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class JerseyLink extends javax.ws.rs.core.Link {
    /**
     * Underlying builder for link's URI.
     */
    private final URI uri;
    /**
     * A map for all the link parameters such as "rel", "type", etc.
     */
    private final Map<String, String> params;

    /**
     * Jersey implementation of {@link javax.ws.rs.core.Link.Builder JAX-RS Link.Builder} contract.
     */
    public static class Builder implements javax.ws.rs.core.Link.Builder {
        /**
         * Underlying builder for link's URI.
         */
        private UriBuilder uriBuilder = new JerseyUriBuilder();
        /**
         * Base URI for resolution of a link URI (if relative).
         */
        private URI baseUri = null;
        /**
         * A map for all the link parameters such as "rel", "type", etc.
         */
        private Map<String, String> params = new HashMap<String, String>();

        @Override
        public Builder link(javax.ws.rs.core.Link link) {
            uriBuilder.uri(link.getUri());
            params.clear();
            params.putAll(link.getParams());
            return this;
        }

        @Override
        public Builder link(String link) {
            LinkProvider.initBuilder(this, link);
            return this;
        }

        @Override
        public Builder uri(URI uri) {
            this.uriBuilder = UriBuilder.fromUri(uri);
            return this;
        }

        @Override
        public Builder uri(String uri) {
            this.uriBuilder = UriBuilder.fromUri(uri);
            return this;
        }

        @Override
        public Builder uriBuilder(UriBuilder uriBuilder) {
            this.uriBuilder = UriBuilder.fromUri(uriBuilder.toTemplate());
            return this;
        }

        @Override
        public Link.Builder baseUri(URI uri) {
            this.baseUri = uri;
            return this;
        }

        @Override
        public Link.Builder baseUri(String uri) {
            this.baseUri = URI.create(uri);
            return this;
        }

        @Override
        public Builder rel(String rel) {
            final String rels = params.get(REL);
            param(REL, rels == null ? rel : rels + " " + rel);
            return this;
        }

        @Override
        public Builder title(String title) {
            param(TITLE, title);
            return this;
        }

        @Override
        public Builder type(String type) {
            param(TYPE, type);
            return this;
        }

        @Override
        public Builder param(String name, String value) {
            if (name == null || value == null) {
                throw new IllegalArgumentException("Link parameter name or value is null");
            }
            params.put(name, value);
            return this;
        }

        @Override
        public JerseyLink build(Object... values) {
            final URI linkUri = resolveLinkUri(values);
            return new JerseyLink(linkUri, Collections.unmodifiableMap(new HashMap<String, String>(params)));
        }

        @Override
        public Link buildRelativized(URI uri, Object... values) {
            final URI linkUri = UriTemplate.relativize(uri, resolveLinkUri(values));
            return new JerseyLink(linkUri, Collections.unmodifiableMap(new HashMap<String, String>(params)));
        }

        private URI resolveLinkUri(Object[] values) {
            final URI linkUri = uriBuilder.build(values);
            if (baseUri == null || linkUri.isAbsolute()) {
                return UriTemplate.normalize(linkUri);
            }
            return UriTemplate.resolve(baseUri, linkUri);
        }
    }

    private JerseyLink(URI uri, Map<String, String> params) {
        this.uri = uri;
        this.params = params;
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public UriBuilder getUriBuilder() {
        return new JerseyUriBuilder().uri(uri);
    }

    @Override
    public String getRel() {
        return params.get(REL);
    }

    @Override
    public List<String> getRels() {
        final String rels = params.get(REL);
        return rels == null ? Collections.<String>emptyList() : Arrays.asList(rels.split(" +"));
    }

    @Override
    public String getTitle() {
        return params.get(TITLE);
    }

    @Override
    public String getType() {
        return params.get(TYPE);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return LinkProvider.stringfy(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Link) {
            final Link otherLink = (Link) other;
            return uri.equals(otherLink.getUri()) && params.equals(otherLink.getParams());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = 89 * hash + (this.params != null ? this.params.hashCode() : 0);
        return hash;
    }
}
