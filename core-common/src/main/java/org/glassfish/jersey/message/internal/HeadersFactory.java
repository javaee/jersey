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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.glassfish.jersey.internal.util.collection.StringKeyIgnoreCaseMultivaluedMap;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utility class supporting the processing of message headers.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public final class HeadersFactory {

    /**
     * Create an empty inbound message headers container. Created container is mutable.
     *
     * @return a new empty mutable container for storing inbound message headers.
     */
    public static AbstractMultivaluedMap<String, String> createInbound() {
        return new StringKeyIgnoreCaseMultivaluedMap<String>();
    }

    /**
     * Create an inbound message headers container. Created container is mutable
     * and is initialized with the supplied initial collection of headers.
     *
     * @param initial initial collection of headers to be used to initialize the
     *     created message headers container.
     * @return a new mutable container for storing inbound message headers
     *     initialized with the supplied initial collection of headers.
     */
    public static AbstractMultivaluedMap<String, String> createInbound(MultivaluedMap<String, String> initial) {
        AbstractMultivaluedMap<String, String> headers = createInbound();
        headers.putAll(initial);
        return headers;
    }

    /**
     * Create an inbound message headers container. Created container is mutable
     * and is initialized with the supplied initial collection of headers supplied
     * as a {@link Map} of {@code String} keys and {@code List<String>} values.
     *
     * @param initial initial collection of headers to be used to initialize the
     *     created message headers container.
     * @return a new mutable container for storing inbound message headers
     *     initialized with the supplied initial collection of headers.
     */
    public static AbstractMultivaluedMap<String, String> createInbound(Map<String, List<String>> initial) {
        AbstractMultivaluedMap<String, String> headers = createInbound();
        headers.putAll(initial);
        return headers;
    }

    /**
     * Get immutable empty message headers container. The factory method can be
     * used to for both message header container types&nbsp;&nbsp;&ndash;&nbsp;&nbsp;inbound
     * as well as outbound.
     *
     * @param <V> header value type. Typically {@link Object} in case of the outbound
     *     headers and {@link String} in case of the inbound headers.
     * @return an immutable empty message headers container.
     */
    public static <V> MultivaluedMap<String, V> empty() {
        return ImmutableMultivaluedMap.empty();
    }

    /**
     * Create an empty outbound message headers container. Created container is mutable.
     *
     * @return a new empty mutable container for storing outbound message headers.
     */
    public static AbstractMultivaluedMap<String, Object> createOutbound() {
        return new StringKeyIgnoreCaseMultivaluedMap<Object>();
    }

    /**
     * Create an outbound message headers container. Created container is mutable
     * and is initialized with the supplied initial collection of headers supplied
     * as a {@link Map} of {@code String} keys and {@code List<String>} values.
     *
     * @param initial initial collection of headers to be used to initialize the
     *     created message headers container.
     * @return a new mutable container for storing outbound message headers
     *     initialized with the supplied initial collection of headers.
     */
    public static AbstractMultivaluedMap<String, Object> createOutbound(Map<String, List<Object>> initial) {
        AbstractMultivaluedMap<String, Object> headers = createOutbound();
        headers.putAll(initial);
        return headers;
    }

    /**
     * Convert a message header value, represented as a general object, to it's
     * string representation.
     * <p>
     * This method defers to {@link RuntimeDelegate#createHeaderDelegate} to
     * obtain a {@link HeaderDelegate} to convert the value to a {@code String}.
     * If a {@link HeaderDelegate} is not found then the {@code toString()}
     * method on the header object is utilized.
     *
     * @param headerValue the header value represented as an object.
     * @param rd runtime delegate instance to be used for header delegate
     *     retrieval. If {@code null}, a default {@code RuntimeDelegate}
     *     instance will be {@link RuntimeDelegate#getInstance() obtained} and
     *     used.
     * @return the string representation of the supplied header value.
     */
    @SuppressWarnings("unchecked")
    public static String toString(final Object headerValue, RuntimeDelegate rd) {
        if (headerValue instanceof String) {
            return (String) headerValue;
        }
        if (rd == null) {
            rd = RuntimeDelegate.getInstance();
        }

        final HeaderDelegate hp = rd.createHeaderDelegate(headerValue.getClass());
        return (hp != null) ? hp.toString(headerValue) : headerValue.toString();
    }

    public static List<String> toString(final List<Object> headerValues, final RuntimeDelegate rd) {
        // Lists#transform returns a list that do not support #add or #set operations so we need to wrap that list to one which
        // supports these operations.
        return new LinkedList<String>(Lists.transform(headerValues, new Function<Object, String>() {

            @Override
            public String apply(Object input) {
                return (input == null) ? "[null]" : HeadersFactory.toString(input, rd);
            }

        }));
    }

    public static MultivaluedMap<String, String> toString(final MultivaluedMap<String, Object> headers, final RuntimeDelegate rd) {
        return createInbound(Maps.transformValues(headers, new Function<List<Object>, List<String>>() {

            @Override
            public List<String> apply(List<Object> input) {
                return (input == null) ? Collections.singletonList("[null]") : HeadersFactory.toString(input, rd);
            }

        }));
    }

    /**
     * Preventing instantiation
     */
    private HeadersFactory() {
    }
}
