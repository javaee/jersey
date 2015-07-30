/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.glassfish.jersey.internal.util.collection.StringKeyIgnoreCaseMultivaluedMap;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Utility class supporting the processing of message headers.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public final class HeaderUtils {

    private static final Logger LOGGER = Logger.getLogger(HeaderUtils.class.getName());

    /**
     * Create an empty inbound message headers container. Created container is mutable.
     *
     * @return a new empty mutable container for storing inbound message headers.
     */
    public static AbstractMultivaluedMap<String, String> createInbound() {
        return new StringKeyIgnoreCaseMultivaluedMap<String>();
    }

    /**
     * Get immutable empty message headers container. The factory method can be
     * used to for both message header container types&nbsp;&nbsp;&ndash;&nbsp;&nbsp;inbound
     * as well as outbound.
     *
     * @param <V> header value type. Typically {@link Object} in case of the outbound
     *            headers and {@link String} in case of the inbound headers.
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
     * Convert a message header value, represented as a general object, to it's
     * string representation. If the supplied header value is {@code null},
     * this method returns {@code null}.
     * <p>
     * This method defers to {@link RuntimeDelegate#createHeaderDelegate} to
     * obtain a {@link HeaderDelegate} to convert the value to a {@code String}.
     * If a {@link HeaderDelegate} is not found then the {@code toString()}
     * method on the header object is utilized.
     *
     * @param headerValue the header value represented as an object.
     * @param rd          runtime delegate instance to be used for header delegate
     *                    retrieval. If {@code null}, a default {@code RuntimeDelegate}
     *                    instance will be {@link RuntimeDelegate#getInstance() obtained} and
     *                    used.
     * @return the string representation of the supplied header value or {@code null}
     *         if the supplied header value is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static String asString(final Object headerValue, RuntimeDelegate rd) {
        if (headerValue == null) {
            return null;
        }
        if (headerValue instanceof String) {
            return (String) headerValue;
        }
        if (rd == null) {
            rd = RuntimeDelegate.getInstance();
        }

        final HeaderDelegate hp = rd.createHeaderDelegate(headerValue.getClass());
        return (hp != null) ? hp.toString(headerValue) : headerValue.toString();
    }

    /**
     * Returns string view of list of header values. Any modifications to the underlying list are visible to the view,
     * the view also supports removal of elements. Does not support other modifications.
     *
     * @param headerValues header values.
     * @param rd           RuntimeDelegate instance or {@code null} (in that case {@link RuntimeDelegate#getInstance()}
     *                     will be called for before element conversion.
     * @return String view of header values.
     */
    public static List<String> asStringList(final List<Object> headerValues, final RuntimeDelegate rd) {
        if (headerValues == null || headerValues.isEmpty()) {
            return Collections.emptyList();
        }

        final RuntimeDelegate delegate;
        if (rd == null) {
            delegate = RuntimeDelegate.getInstance();
        } else {
            delegate = rd;
        }

        return Lists.transform(headerValues, new Function<Object, String>() {
            @Override
            public String apply(final Object input) {
                return (input == null) ? "[null]" : HeaderUtils.asString(input, delegate);
            }

        });
    }

    /**
     * Returns string view of passed headers. Any modifications to the headers are visible to the view, the view also
     * supports removal of elements. Does not support other modifications.
     *
     * @param headers headers.
     * @return String view of headers or {@code null} if {code headers} input parameter is {@code null}.
     */
    public static MultivaluedMap<String, String> asStringHeaders(final MultivaluedMap<String, Object> headers) {
        if (headers == null) {
            return null;
        }

        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        return new AbstractMultivaluedMap<String, String>(
                Maps.transformValues(headers, new Function<List<Object>, List<String>>() {
                    @Override
                    public List<String> apply(final List<Object> input) {
                        return HeaderUtils.asStringList(input, rd);
                    }
                })
        ) {
        };
    }

    /**
     * Transforms multi value map of headers to single {@code String} value map.
     *
     * Returned map is immutable. Map values are formatted using method {@link #asHeaderString}.
     *
     * @param headers headers to be formatted
     * @return immutable single {@code String} value map or
     *      {@code null} if {@code headers} input parameter is {@code null}.
     */
    public static Map<String, String> asStringHeadersSingleValue(final MultivaluedMap<String, Object> headers) {
        if (headers == null) {
            return null;
        }

        final RuntimeDelegate rd = RuntimeDelegate.getInstance();
        final ImmutableMap.Builder<String, String> immutableMapBuilder = new ImmutableMap.Builder<String, String>();
        for (final Map.Entry<? extends String, ? extends List<Object>> entry : headers.entrySet()) {
            immutableMapBuilder.put(entry.getKey(), asHeaderString(entry.getValue(), rd));
        }
        return immutableMapBuilder.build();
    }

    /**
     * Converts a list of message header values to a single string value (with individual values separated by
     * {@code ','}).
     *
     * Each single header value is converted to String using a
     * {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if one is available
     * via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(java.lang.Class)}
     * for the header value class or using its {@code toString()} method if a header
     * delegate is not available.
     *
     * @param values list of individual header values.
     * @param rd     {@link RuntimeDelegate} instance or {@code null} (in that case {@link RuntimeDelegate#getInstance()}
     *               will be called for before conversion of elements).
     * @return single string consisting of all the values passed in as a parameter. If values parameter is {@code null},
     *         {@code null} is returned. If the list of values is empty, an empty string is returned.
     */
    public static String asHeaderString(final List<Object> values, final RuntimeDelegate rd) {
        if (values == null) {
            return null;
        }
        final Iterator<String> stringValues = asStringList(values, rd).iterator();
        if (!stringValues.hasNext()) {
            return "";
        }

        final StringBuilder buffer = new StringBuilder(stringValues.next());
        while (stringValues.hasNext()) {
            buffer.append(',').append(stringValues.next());
        }

        return buffer.toString();
    }

    /**
     * Compares two snapshots of headers from jersey {@code ClientRequest} and logs {@code WARNING} in case of difference.
     *
     * Current container implementations does not support header modification in {@link javax.ws.rs.ext.WriterInterceptor}
     * and {@link javax.ws.rs.ext.MessageBodyWriter}. The method checks there are some newly added headers
     * (probably by WI or MBW) and logs {@code WARNING} message about it.
     *
     * @param headersSnapshot first immutable snapshot of headers
     * @param currentHeaders  current instance of headers tobe compared to
     * @param connectorName   name of connector the method is invoked from, used just in logged message
     * @see <a href="https://java.net/jira/browse/JERSEY-2341">JERSEY-2341</a>
     */
    public static void checkHeaderChanges(final Map<String, String> headersSnapshot,
                                          final MultivaluedMap<String, Object> currentHeaders,
                                          final String connectorName) {
        if (HeaderUtils.LOGGER.isLoggable(Level.WARNING)) {
            final RuntimeDelegate rd = RuntimeDelegate.getInstance();
            final Set<String> changedHeaderNames = new HashSet<String>();
            for (final Map.Entry<? extends String, ? extends List<Object>> entry : currentHeaders.entrySet()) {
                if (!headersSnapshot.containsKey(entry.getKey())) {
                    changedHeaderNames.add(entry.getKey());
                } else {
                    final String prevValue = headersSnapshot.get(entry.getKey());
                    final String newValue = asHeaderString(currentHeaders.get(entry.getKey()), rd);
                    if (!prevValue.equals(newValue)) {
                        changedHeaderNames.add(entry.getKey());
                    }
                }
            }
            if (!changedHeaderNames.isEmpty()) {
                if (HeaderUtils.LOGGER.isLoggable(Level.WARNING)) {
                    HeaderUtils.LOGGER.warning(LocalizationMessages.SOME_HEADERS_NOT_SENT(connectorName,
                            changedHeaderNames.toString()));
                }
            }
        }
    }

    /**
     * Preventing instantiation.
     */
    private HeaderUtils() {
        throw new AssertionError("No instances allowed.");
    }
}
