/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey._remove;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;

/**
 * An interface used to build {@link javax.ws.rs.core.Request} instances, typically used in
 * JAX-RS filters. An initial instance may be obtained via {@link FilterContext}
 * that is passed to the filters.
 * <p/>
 * Methods of this interface provide the ability to set request metadata, such
 * as headers or entity.
 * <p/>
 * Where multiple variants of the same method are provided, the type of
 * the supplied parameter is retained in the metadata of the built {@code Request}.
 *
 * @since 2.0
 */
public interface RequestBuilder extends Cloneable {

    // Headers
    // General headers

    /**
     * Set the list of allowed methods for the resource. Any duplicate method
     * names will be truncated to a single entry.
     *
     * @param methods the methods to be listed as allowed for the resource,
     *                if {@code null} any existing allowed method list will be removed.
     * @return the updated request builder.
     */
    public RequestBuilder allow(String... methods);

    /**
     * Set the list of allowed methods for the resource.
     *
     * @param methods the methods to be listed as allowed for the resource,
     *                if {@code null} any existing allowed method list will be removed.
     * @return the updated request builder.
     */
    public RequestBuilder allow(Set<String> methods);

    /**
     * Set the cache control data of the message.
     *
     * @param cacheControl the cache control directives, if {@code null}
     *                     any existing cache control directives will be removed.
     * @return the updated request builder.
     */
    public RequestBuilder cacheControl(CacheControl cacheControl);

    /**
     * Set the message entity content encoding.
     *
     * @param encoding the content encoding of the message entity,
     *                 if {@code null} any existing value for content encoding will be
     *                 removed.
     * @return the updated request builder.
     */
    public RequestBuilder encoding(String encoding);

    /**
     * Add an arbitrary header.
     *
     * @param name  the name of the header
     * @param value the value of the header, the header will be serialized
     *              using a {@link javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate} if
     *              one is available via {@link javax.ws.rs.ext.RuntimeDelegate#createHeaderDelegate(Class)}
     *              for the class of {@code value} or using its {@code toString} method
     *              if a header delegate is not available. If {@code value} is {@code null}
     *              then all current headers of the same name will be removed.
     * @return the updated request builder.
     */
    public RequestBuilder header(String name, Object value);

    /**
     * Set the message entity language.
     *
     * @param language the language of the message entity, if {@code null} any
     *                 existing value for language will be removed.
     * @return the updated request builder.
     */
    public RequestBuilder language(String language);

    /**
     * Set the message entity language.
     *
     * @param language the language of the message entity, if {@code null} any
     *                 existing value for type will be removed.
     * @return the updated request builder.
     */
    public RequestBuilder language(Locale language);

    /**
     * Set the message entity media type.
     *
     * @param type the media type of the message entity. If {@code null}, any
     *             existing value for type will be removed
     * @return the updated request builder.
     */
    public RequestBuilder type(MediaType type);

    /**
     * Set the message entity media type.
     *
     * @param type the media type of the message entity. If {@code null}, any
     *             existing value for type will be removed
     * @return the updated request builder.
     */
    public RequestBuilder type(String type);

    /**
     * Set message entity representation metadata.
     * <p/>
     * Equivalent to setting the values of content type, content language,
     * and content encoding separately using the values of the variant properties.
     *
     * @param variant metadata of the message entity, a {@code null} value is
     *                equivalent to a variant with all {@code null} properties.
     * @return the updated request builder.
     * @see #encoding(String)
     * @see #language(java.util.Locale)
     * @see #type(javax.ws.rs.core.MediaType)
     */
    public RequestBuilder variant(Variant variant);

    // Request-specific headers

    /**
     * Add acceptable media types.
     *
     * @param types an array of the acceptable media types
     * @return updated request builder.
     */
    public RequestBuilder accept(MediaType... types);

    /**
     * Add acceptable media types.
     *
     * @param types an array of the acceptable media types
     * @return updated request builder.
     */
    public RequestBuilder accept(String... types);

    /**
     * Add acceptable languages.
     *
     * @param locales an array of the acceptable languages
     * @return updated request builder.
     */
    public RequestBuilder acceptLanguage(Locale... locales);

    /**
     * Add acceptable languages.
     *
     * @param locales an array of the acceptable languages
     * @return updated request builder.
     */
    public RequestBuilder acceptLanguage(String... locales);

    /**
     * Add a cookie to be set.
     *
     * @param cookie to be set.
     * @return updated request builder.
     */
    public RequestBuilder cookie(Cookie cookie);

    // Request URI, entity....
    public RequestBuilder redirect(String uri);

    public RequestBuilder redirect(URI uri);

    public RequestBuilder redirect(UriBuilder uri);

    /**
     * Modify the HTTP method of the request.
     * <p />
     * The method name parameter can be any arbitrary, non-empty string, containing
     * but NOT limited to the command verbs of HTTP, WebDAV and other protocols.
     * An implementation MUST NOT expect the method to be part of any particular set
     * of methods. Any provided method name MUST be forwarded to the resource without
     * any limitations.
     *
     * @param httpMethod new method to be set on the request.
     * @return updated request builder instance.
     */
    public RequestBuilder method(String httpMethod);

    /**
     * Set the request entity in the builder.
     * <p />
     * Any Java type instance for a request entity, that is supported by the
     * runtime can be passed. It is the callers responsibility to wrap the
     * actual entity with {@link javax.ws.rs.core.GenericEntity} if preservation of its generic
     * type is required. Note that the entity can be also set as an
     * {@link java.io.InputStream input stream}.
     * <p />
     * A specific entity media type can be set using one of the {@code type(...)}
     * methods.
     *
     * @param entity the request entity.
     * @return updated request builder instance.
     * @see #entity(Object, java.lang.annotation.Annotation[])
     * @see #type(javax.ws.rs.core.MediaType)
     * @see #type(String)
     */
    public RequestBuilder entity(Object entity);

    /**
     * Set the request entity in the builder.
     * <p />
     * Any Java type instance for a request entity, that is supported by the
     * runtime can be passed. It is the callers responsibility to wrap the
     * actual entity with {@link javax.ws.rs.core.GenericEntity} if preservation of its generic
     * type is required. Note that the entity can be also set as an
     * {@link java.io.InputStream input stream}.
     * <p />
     * A specific entity media type can be set using one of the {@code type(...)}
     * methods.
     *
     * @param entity      the request entity.
     * @param annotations annotations that will be passed to the {@link javax.ws.rs.ext.MessageBodyWriter}.
     * @return updated request builder instance.
     * @see #entity(Object)
     * @see #type(javax.ws.rs.core.MediaType)
     * @see #type(String)
     */
    public RequestBuilder entity(Object entity, Annotation[] annotations);

    /**
     * Create a copy of the request builder preserving its state.
     *
     * Note that the returned builder has its own {@code RequestHeaders request
     * headers} but the header values are shared with the original
     * {@code RequestBuilder} instance. Similarly, entity instance is also
     * shared with the original {@code RequestBuilder} instance.
     *
     * @return a copy of the request builder.
     */
    public RequestBuilder clone();

    public Request build();
}
