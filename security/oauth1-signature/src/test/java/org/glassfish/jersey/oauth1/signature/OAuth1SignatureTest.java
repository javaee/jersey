/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.oauth1.signature;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Martin Matula
 */
public class OAuth1SignatureTest {

    @Test
    public void testNormalizeParameters() {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<String, String>();
        params.add("org-country", "US");
        params.add("org", "acme");
        params.add("a", "b");
        params.add("org", "dummy");

        String normalizedParams = OAuth1Signature.normalizeParameters(new OAuth1Request() {
            @Override
            public String getRequestMethod() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public URL getRequestURL() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<String> getParameterNames() {
                return params.keySet();
            }

            @Override
            public List<String> getParameterValues(String name) {
                return params.get(name);
            }

            @Override
            public List<String> getHeaderValues(String name) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void addHeaderValue(String name, String value) throws IllegalStateException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, new OAuth1Parameters());
        assertEquals("a=b&org=acme&org=dummy&org-country=US", normalizedParams);
    }

    @Test
    public void testNullParamValue() {
        final MultivaluedMap<String, String> params = new MultivaluedHashMap<String, String>();
        params.add("org-country", "US");
        params.put("org", Arrays.asList(new String[]{null}));
        params.add("a", "b");

        String normalizedParams = OAuth1Signature.normalizeParameters(new OAuth1Request() {
            @Override
            public String getRequestMethod() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public URL getRequestURL() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<String> getParameterNames() {
                return params.keySet();
            }

            @Override
            public List<String> getParameterValues(String name) {
                return params.get(name);
            }

            @Override
            public List<String> getHeaderValues(String name) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void addHeaderValue(String name, String value) throws IllegalStateException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, new OAuth1Parameters());
        assertEquals("a=b&org=&org-country=US", normalizedParams);
    }
}
