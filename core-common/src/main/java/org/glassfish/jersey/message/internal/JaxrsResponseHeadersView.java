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

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

/**
 * Former adapter for {@link Headers Jersey Headers} to former JAX-RS {@code ResponseHeaders}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
class JaxrsResponseHeadersView {

    private Headers wrapped;

    public JaxrsResponseHeadersView(Headers wrapped) {
        this.wrapped = wrapped;
    }

    public Map<String, NewCookie> getCookies() {
        return HttpHelper.getNewCookies(wrapped);
    }

    public EntityTag getEntityTag() {
        return HttpHelper.getEntityTag(wrapped);
    }

    public Date getLastModified() {
        return HttpHelper.getLastModified(wrapped);
    }

    public URI getLocation() {
        return HttpHelper.getLocation(wrapped);
    }

    public Set<String> getAllowedMethods() {
        return HttpHelper.getAllowedMethods(wrapped);
    }

    public Date getDate() {
        return HttpHelper.getDate(wrapped);
    }

    public String getHeader(String name) {
        return wrapped.header(name);
    }

    public MultivaluedMap<String, String> asMap() {
        return wrapped.headers();
    }

    public List<String> getHeaderValues(String name) {
        return wrapped.headerValues(name);
    }

    public Locale getLanguage() {
        return HttpHelper.getContentLanguageAsLocale(wrapped);
    }

    public int getLength() {
        return HttpHelper.getContentLength(wrapped);
    }

    public MediaType getMediaType() {
        return HttpHelper.getContentType(wrapped);
    }

    public Set<Link> getLinks() {
        return HttpHelper.getLinks(wrapped);
    }

    public Link getLink(String relation) {
        for (Link l : HttpHelper.getLinks(wrapped)) {
            List<String> rels = l.getRel();
            if (rels != null && rels.contains(relation)) {
                return l;
            }
        }
        return null;
    }

    public boolean hasLink(String relation) {
        for (Link l : HttpHelper.getLinks(wrapped)) {
            List<String> rels = l.getRel();
            if (rels != null && rels.contains(relation)) {
                return true;
            }
        }
        return false;
    }

    public Link.Builder getLinkBuilder(String relation) {
        Link link = getLink(relation);
        if (link == null) {
            return null;
        }

        return Link.fromLink(link);
    }
}
