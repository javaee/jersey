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

package org.glassfish.jersey.oauth1.signature;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Paul C. Bryan <pbryan@sun.com>
 */
class DummyRequest implements OAuth1Request {

    private HashMap<String, ArrayList<String>> headers = new HashMap<String, ArrayList<String>>();

    private HashMap<String, ArrayList<String>> params = new HashMap<String, ArrayList<String>>();

    private String requestMethod;

    private String requestURL;

    public DummyRequest() {
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String method) {
        requestMethod = method;
    }

    public DummyRequest requestMethod(String method) {
        setRequestMethod(method);
        return this;
    }

    public URL getRequestURL() {
        try {
            return new URL(requestURL);
        } catch (MalformedURLException ex) {
            Logger.getLogger(DummyRequest.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void setRequestURL(String url) {
        requestURL = url;
    }

    public DummyRequest requestURL(String url) {
        setRequestURL(url);
        return this;
    }

    public List<String> getHeaderValues(String name) {
        return headers.get(name);
    }

    public void addHeaderValue(String name, String value) {
        ArrayList<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            headers.put(name, values);
        }
        values.add(value);
    }

    public DummyRequest headerValue(String name, String value) {
        addHeaderValue(name, value);
        return this;
    }

    public Set<String> getParameterNames() {
        return params.keySet();
    }

    public List<String> getParameterValues(String name) {
        return params.get(name);
    }

    public synchronized void addParameterValue(String name, String value) {
        ArrayList<String> values = params.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            params.put(name, values);
        }
        values.add(value);
    }

    public DummyRequest parameterValue(String name, String value) {
        addParameterValue(name, value);
        return this;
    }
}
