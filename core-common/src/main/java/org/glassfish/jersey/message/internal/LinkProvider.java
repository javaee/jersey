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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Singleton;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.LocalizationMessages;

import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.spi.HeaderDelegateProvider;

import static org.glassfish.jersey.message.internal.Utils.throwIllegalArgumentExceptionIfNull;

/**
 * Provider for Link Headers.
 *
 * @author Santiago Pericas-Geertsen
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
public class LinkProvider implements HeaderDelegateProvider<Link> {

    @Override
    public boolean supports(Class<?> type) {
        return Link.class.isAssignableFrom(type);
    }

    @Override
    public Link fromString(String value) throws IllegalArgumentException {

        throwIllegalArgumentExceptionIfNull(value, LocalizationMessages.LINK_IS_NULL());

        Builder lb;
        StringTokenizer st = new StringTokenizer(value.trim(), "<>;=\"", true);
        try {
            checkToken(st, "<");
            lb = Link.fromUri(st.nextToken().trim());
            checkToken(st, ">");
            while (st.hasMoreTokens()) {
                checkToken(st, ";");
                String n = st.nextToken().trim();
                checkToken(st, "=");
                checkToken(st, "\"");
                String v = st.nextToken();
                checkToken(st, "\"");
                lb.param(n, v);
            }
        } catch (Throwable e) {
            lb = null;
        }
        if (lb == null) {
            throw new IllegalArgumentException("Unable to parse link " + value);
        }
        return lb.build();
    }

    private void checkToken(StringTokenizer st, String expected) throws AssertionError {
        String token;
        do {
            token = st.nextToken().trim();
        } while (token.length() == 0);
        if (!token.equals(expected)) {
            throw new AssertionError("Expected token " + expected + " but found " + token);
        }
    }

    @Override
    public String toString(Link value) {
        throwIllegalArgumentExceptionIfNull(value, LocalizationMessages.LINK_IS_NULL());

        Map<String, String> map = value.getParams();
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(value.getUri()).append('>');

        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("; ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
        return sb.toString();
    }

    /**
     * Extract the list of link relations from the string value of a {@link Link#REL} attribute.
     *
     * @param rel string value of the link {@code "rel"} attribute.
     * @return list of relations in the {@code "rel"} attribute string value.
     */
    static List<String> getLinkRelations(final String rel) {
        return (rel == null) ? null : Arrays.asList(Tokenizer.tokenize(rel, "\" "));
    }
}
