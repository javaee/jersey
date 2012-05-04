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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;

/**
 * Mutable message headers implementation class.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
class MutableHeaders implements Headers, Headers.Builder<MutableHeaders> {

    private final AbstractMultivaluedMap<String, String> stringHeaders;
    private final AbstractMultivaluedMap<String, Object> objectHeaders;

    public MutableHeaders() {
        this.stringHeaders = HeadersFactory.createInbound();
        this.objectHeaders = HeadersFactory.createOutbound();
    }

    public MutableHeaders(MutableHeaders that) {
        this();

        this.stringHeaders.putAll(that.stringHeaders);
        this.objectHeaders.putAll(that.objectHeaders);
    }

    @Override
    public String header(String name) {
        final List<String> headers = headerValues(name);
        if (headers == null) {
            return null;
        }

        final Iterator<String> values = headerValues(name).iterator();
        if (!values.hasNext()) {
            return null;
        }

        StringBuilder buffer = new StringBuilder(values.next());
        while (values.hasNext()) {
            buffer.append(',').append(values.next());
        }

        return buffer.toString();
    }

    @Override
    public MultivaluedMap<String, String> headers() {
        fetchAll();
        return stringHeaders;
    }

    @Override
    public List<String> headerValues(String name) {
        fetch(name);
        return stringHeaders.get(name);
    }

    @Override
    public MutableHeaders header(String name, Object value) {
        objectHeaders.add(name, value);
        return this;
    }

    @Override
    public MutableHeaders header(String name, String value) {
        stringHeaders.add(name, value);
        return this;
    }

    @Override
    public MutableHeaders headers(String name, Object... values) {
        objectHeaders.addAll(name, values);
        return this;
    }

    @Override
    public MutableHeaders headers(String name, String... values) {
        stringHeaders.addAll(name, values);
        return this;
    }

    @Override
    public MutableHeaders headers(String name, Iterable<? extends Object> values) {
        objectHeaders.addAll(name, iterableToList(values));
        return this;
    }

    @Override
    public MutableHeaders headers(MultivaluedMap<String, ? extends Object> headers) {
        objectHeaders.putAll((Map<String, List<Object>>) headers);
        return this;
    }

    @Override
    public MutableHeaders headers(Map<String, List<String>> headers) {
        stringHeaders.putAll(HeadersFactory.createInbound(headers));
        return this;
    }

    @Override
    public MutableHeaders remove(String name) {
        objectHeaders.remove(name);
        stringHeaders.remove(name);
        return this;
    }

    @Override
    public MutableHeaders replace(String name, Iterable<? extends Object> values) {
        stringHeaders.remove(name);
        objectHeaders.put(name, iterableToList(values));
        return this;
    }

    @Override
    public MutableHeaders replaceAll(MultivaluedMap<String, String> headers) {
        objectHeaders.clear();
        stringHeaders.clear();

        stringHeaders.putAll(headers);

        return this;
    }

    private void fetch(final String headerName) {
        if (objectHeaders.isEmpty()) {
            return;
        }

        final List<Object> values = objectHeaders.remove(headerName);
        if (values == null || values.isEmpty()) {
            return;
        }

        stringHeaders.addAll(headerName, HeadersFactory.toString(values, RuntimeDelegate.getInstance()));
    }

    private void fetchAll() {
        if (!objectHeaders.isEmpty()) {
            stringHeaders.putAll(HeadersFactory.toString(objectHeaders, RuntimeDelegate.getInstance()));
            objectHeaders.clear();
        }
    }

    private List<Object> iterableToList(final Iterable<? extends Object> values) {
        final LinkedList<Object> linkedList = new LinkedList<Object>();

        for (Object element : values) {
            linkedList.add(element);
        }

        return linkedList;
    }

}
