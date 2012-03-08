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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.TypeLiteral;

import org.glassfish.jersey.message.MessageBodyWorkers;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Abstract mutable message implementation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class AbstractMutableMessage<M extends AbstractMutableMessage> {

    private MutableHeaders headers;
    private MutableEntity entity;
    private Map<String, Object> properties;

    protected AbstractMutableMessage() {
        this.headers = new MutableHeaders();
        this.properties = Maps.newHashMap();
        this.entity = MutableEntity.empty(this);
    }

    protected AbstractMutableMessage(AbstractMutableMessage<?> that) {
        this.headers = new MutableHeaders(that.headers);
        this.entity = new MutableEntity(this, that.entity);
        this.properties = Maps.newHashMap(that.properties);
    }

    public AbstractMutableMessage(ListMultimap<String, String> headers, InputStream entity,
            Map<String, Object> properties) {
        this();

        this.headers.replaceAll(headers);
        this.entity.content(entity);
        this.properties.putAll(properties);
    }

    // Accessors
    public MutableEntity entity() {
        return entity;
    }

    public Map<String, Object> properties() {
        return properties;
    }

    public MessageBodyWorkers entityWorkers() {
        return entity.workers();
    }

    // Mutators
    @SuppressWarnings("unchecked")
    public M properties(Map<String, Object> properties) {
        this.properties.putAll(properties);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M property(String name, Object value) {
        this.properties.put(name, value);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M clearProperties() {
        this.properties.clear();
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M build() {
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M toBuilder() {
        return (M) this;
    }

    // MutableEntity delegates
    public Type type() {
        return entity.type();
    }

    public boolean isEmpty() {
        return entity.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public M content(InputStream content) {
        entity.content(content);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public <T> M content(Object content, TypeLiteral<T> type) {
        entity.content(content, type);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M content(Object content, Type type) {
        entity.content(content, type);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M content(Object content) {
        entity.content(content);
        return (M) this;
    }

    public Object content() {
        return entity.content();
    }

    public <T> T content(TypeLiteral<T> type) {
        return entity.content(type);
    }

    public <T> T content(Class<T> type) {
        return entity.content(type);
    }

    public <T> T content(TypeLiteral<T> type, Annotation[] annotations) {
        return entity.content(type, annotations);
    }

    public <T> T content(Class<T> type, Annotation[] annotations) {
        return entity.content(type, annotations);
    }

    @SuppressWarnings("unchecked")
    public M writeAnnotations(Annotation[] annotations) {
        entity.writeAnnotations(annotations);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M entityWorkers(MessageBodyWorkers workers) {
        entity.workers(workers);
        return (M) this;
    }

    // MutableHeaders delegates
    public MultivaluedMap<String, String> toJaxrsHeaderMap() {
        return headers.toJaxrsHeaderMap();
    }

    @SuppressWarnings("unchecked")
    public M replaceAll(ListMultimap<String, String> headers) {
        this.headers.replaceAll(headers);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M replace(String name, Iterable<? extends Object> values) {
        headers.replace(name, values);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M remove(String name) {
        headers.remove(name);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M headers(Map<String, List<String>> headers) {
        this.headers.headers(headers);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M headers(Multimap<String, ? extends Object> headers) {
        this.headers.headers(headers);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M headers(String name, Iterable<? extends Object> values) {
        headers.headers(name, values);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M headers(String name, Object... values) {
        headers.headers(name, values);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M headers(String name, String... values) {
        headers.headers(name, values);
        return (M) this;
    }

    public ListMultimap<String, String> headers() {
        return headers.headers();
    }

    public List<String> headerValues(String name) {
        return headers.headerValues(name);
    }

    @SuppressWarnings("unchecked")
    public M header(String name, Object value) {
        headers.header(name, value);
        return (M) this;
    }

    @SuppressWarnings("unchecked")
    public M header(String name, String value) {
        headers.header(name, value);
        return (M) this;
    }

    public String header(String name) {
        return headers.header(name);
    }

    @SuppressWarnings("unchecked")
    public M cookie(Cookie cookie) {
        headers.header("Cookie", cookie);
        return (M) this;
    }
}
