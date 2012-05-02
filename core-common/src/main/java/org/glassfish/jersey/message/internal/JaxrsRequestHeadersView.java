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
package org.glassfish.jersey.message.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.RequestHeaders;

/**
 * Adapter for {@link Headers Jersey Headers} to JAX-RS {@link javax.ws.rs.core.RequestHeaders}
 * and {@link javax.ws.rs.core.HttpHeaders}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class JaxrsRequestHeadersView implements RequestHeaders, HttpHeaders {

    private Headers wrapped;

    public JaxrsRequestHeadersView(Headers wrapped) {
        this.wrapped = wrapped;
    }

    static JaxrsRequestHeadersView unwrap(javax.ws.rs.core.RequestHeaders headers) {
        if (headers instanceof JaxrsRequestHeadersView) {
            return (JaxrsRequestHeadersView) headers;
        }

        throw new IllegalArgumentException(String.format("Request headers class type '%s' not supported.", headers.getClass().getName()));
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return new ArrayList<MediaType>(HttpHelper.getAccept(wrapped));
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        List<AcceptableLanguageTag> alts = HttpHelper.getAcceptLanguage(wrapped);
        List<Locale> acceptLanguages = new ArrayList<Locale>(alts.size());
        for (AcceptableLanguageTag alt : alts) {
            acceptLanguages.add(alt.getAsLocale());
        }
        return acceptLanguages;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return HttpHelper.getCookies(wrapped);
    }

    @Override
    public Date getDate() {
        return HttpHelper.getDate(wrapped);
    }

    @Override
    public String getHeader(String name) {
        return wrapped.header(name);
    }

    @Override
    public MultivaluedMap<String, String> asMap() {
        return wrapped.headers();
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return wrapped.headerValues(name);
    }

    @Override
    public Locale getLanguage() {
        return HttpHelper.getContentLanguageAsLocale(wrapped);
    }

    @Override
    public int getLength() {
        return HttpHelper.getContentLength(wrapped);
    }

    @Override
    public MediaType getMediaType() {
        return HttpHelper.getContentType(wrapped);
    }

    @Override
    public List<String> getRequestHeader(String name) {
        return wrapped.headerValues(name);
    }

    @Override
    public MultivaluedMap<String, String> getRequestHeaders() {
        return asMap();
    }

    @Override
    public Set<Link> getLinks() {
        return HttpHelper.getLinks(wrapped);
    }

    @Override
    public Link getLink(String relation) {
        for (Link l : HttpHelper.getLinks(wrapped)) {
            List<String> rels = l.getRel();
            if (rels != null && rels.contains(relation)) {
                return l;
            }
        }
        return null;
    }

    @Override
    public boolean hasLink(String relation) {
        for (Link l : HttpHelper.getLinks(wrapped)) {
            List<String> rels = l.getRel();
            if (rels != null && rels.contains(relation)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        Link link = getLink(relation);
        if (link == null) {
            return null;
        }

        return Link.fromLink(link);
    }
}
