/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.inject.ServiceLocatorSupplier;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.OutboundMessageContext;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.base.Preconditions;

/**
 * Jersey client request context.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ClientRequest extends OutboundMessageContext implements ClientRequestContext, ServiceLocatorSupplier {

    // Request-scoped configuration instance
    private final ClientConfig clientConfig;
    // Request-scoped properties delegate
    private final PropertiesDelegate propertiesDelegate;
    // Absolute request URI
    private URI requestUri;
    // Request method
    private String httpMethod;
    // Request filter chain execution aborting response
    private Response abortResponse;
    // Entity providers
    private MessageBodyWorkers workers;
    // Flag indicating whether the request is asynchronous
    private boolean asynchronous;
    // true if writeEntity() was already called
    private boolean entityWritten;
    // writer interceptors used to write the request
    private Iterable<WriterInterceptor> writerInterceptors;
    // reader interceptors used to write the request
    private Iterable<ReaderInterceptor> readerInterceptors;
    // do not add user-agent header (if not directly set) to the request.
    private boolean ignoreUserAgent;

    private static final Logger LOGGER = Logger.getLogger(ClientRequest.class.getName());

    /**
     * Create new Jersey client request context.
     *
     * @param requestUri         request Uri.
     * @param clientConfig      request configuration.
     * @param propertiesDelegate properties delegate.
     */
    protected ClientRequest(
            final URI requestUri, final ClientConfig clientConfig, final PropertiesDelegate propertiesDelegate) {
        clientConfig.checkClient();

        this.requestUri = requestUri;
        this.clientConfig = clientConfig;
        this.propertiesDelegate = propertiesDelegate;
    }

    /**
     * Copy constructor.
     *
     * @param original original instance.
     */
    public ClientRequest(final ClientRequest original) {
        super(original);
        this.requestUri = original.requestUri;
        this.httpMethod = original.httpMethod;
        this.workers = original.workers;
        this.clientConfig = original.clientConfig.snapshot();
        this.asynchronous = original.isAsynchronous();
        this.readerInterceptors = original.readerInterceptors;
        this.writerInterceptors = original.writerInterceptors;
        this.propertiesDelegate = new MapPropertiesDelegate(original.propertiesDelegate);
        this.ignoreUserAgent = original.ignoreUserAgent;
    }

    /**
     * Resolve a property value for the specified property {@code name}.
     *
     * <p>
     * The method returns the value of the property registered in the request-specific
     * property bag, if available. If no property for the given property name is found
     * in the request-specific property bag, the method looks at the properties stored
     * in the {@link #getConfiguration() global client-runtime configuration} this request
     * belongs to. If there is a value defined in the client-runtime configuration,
     * it is returned, otherwise the method returns {@code null} if no such property is
     * registered neither in the client runtime nor in the request-specific property bag.
     * </p>
     *
     * @param name property name.
     * @param type expected property class type.
     * @param <T> property Java type.
     * @return resolved property value or {@code null} if no such property is registered.
     */
    public <T> T resolveProperty(final String name, final Class<T> type) {
        return resolveProperty(name, null, type);
    }

    /**
     * Resolve a property value for the specified property {@code name}.
     *
     * <p>
     * The method returns the value of the property registered in the request-specific
     * property bag, if available. If no property for the given property name is found
     * in the request-specific property bag, the method looks at the properties stored
     * in the {@link #getConfiguration() global client-runtime configuration} this request
     * belongs to. If there is a value defined in the client-runtime configuration,
     * it is returned, otherwise the method returns {@code defaultValue} if no such property is
     * registered neither in the client runtime nor in the request-specific property bag.
     * </p>
     *
     * @param name property name.
     * @param defaultValue default value to return if the property is not registered.
     * @param <T> property Java type.
     * @return resolved property value or {@code defaultValue} if no such property is registered.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolveProperty(final String name, final T defaultValue) {
        return resolveProperty(name, defaultValue, (Class<T>) defaultValue.getClass());
    }

    private <T> T resolveProperty(final String name, Object defaultValue, final Class<T> type) {
        // Check runtime configuration first
        Object result = clientConfig.getProperty(name);
        if (result != null) {
            defaultValue = result;
        }

        // Check request properties next
        result = propertiesDelegate.getProperty(name);
        if (result == null) {
            result = defaultValue;
        }

        return (result == null) ? null : PropertiesHelper.convertValue(result, type);
    }

    @Override
    public Object getProperty(final String name) {
        return propertiesDelegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertiesDelegate.getPropertyNames();
    }

    @Override
    public void setProperty(final String name, final Object object) {
        propertiesDelegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(final String name) {
        propertiesDelegate.removeProperty(name);
    }

    /**
     * Get the underlying properties delegate.
     *
     * @return underlying properties delegate.
     */
    PropertiesDelegate getPropertiesDelegate() {
        return propertiesDelegate;
    }

    /**
     * Get the underlying client runtime.
     *
     * @return underlying client runtime.
     */
    ClientRuntime getClientRuntime() {
        return clientConfig.getRuntime();
    }

    @Override
    public URI getUri() {
        return requestUri;
    }

    @Override
    public void setUri(final URI uri) {
        this.requestUri = uri;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public void setMethod(final String method) {
        this.httpMethod = method;
    }

    @Override
    public JerseyClient getClient() {
        return clientConfig.getClient();
    }

    @Override
    public void abortWith(final Response response) {
        this.abortResponse = response;
    }

    /**
     * Get the request filter chain aborting response if set, or {@code null} otherwise.
     *
     * @return request filter chain aborting response if set, or {@code null} otherwise.
     */
    public Response getAbortResponse() {
        return abortResponse;
    }

    @Override
    public Configuration getConfiguration() {
        return clientConfig.getRuntime().getConfig();
    }

    /**
     * Get internal client configuration state.
     *
     * @return internal client configuration state.
     */
    ClientConfig getClientConfig() {
        return clientConfig;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return super.getRequestCookies();
    }

    /**
     * Get the message body workers associated with the request.
     *
     * @return message body workers.
     */
    public MessageBodyWorkers getWorkers() {
        return workers;
    }

    /**
     * Set the message body workers associated with the request.
     *
     * @param workers message body workers.
     */
    public void setWorkers(final MessageBodyWorkers workers) {
        this.workers = workers;
    }

    /**
     * Add new accepted types to the message headers.
     *
     * @param types accepted types to be added.
     */
    public void accept(final MediaType... types) {
        getHeaders().addAll(HttpHeaders.ACCEPT, (Object[]) types);
    }

    /**
     * Add new accepted types to the message headers.
     *
     * @param types accepted types to be added.
     */
    public void accept(final String... types) {
        getHeaders().addAll(HttpHeaders.ACCEPT, (Object[]) types);
    }

    /**
     * Add new accepted languages to the message headers.
     *
     * @param locales accepted languages to be added.
     */
    public void acceptLanguage(final Locale... locales) {
        getHeaders().addAll(HttpHeaders.ACCEPT_LANGUAGE, (Object[]) locales);
    }

    /**
     * Add new accepted languages to the message headers.
     *
     * @param locales accepted languages to be added.
     */
    public void acceptLanguage(final String... locales) {
        getHeaders().addAll(HttpHeaders.ACCEPT_LANGUAGE, (Object[]) locales);
    }

    /**
     * Add new cookie to the message headers.
     *
     * @param cookie cookie to be added.
     */
    public void cookie(final Cookie cookie) {
        getHeaders().add(HttpHeaders.COOKIE, cookie);
    }

    /**
     * Add new cache control entry to the message headers.
     *
     * @param cacheControl cache control entry to be added.
     */
    public void cacheControl(final CacheControl cacheControl) {
        getHeaders().add(HttpHeaders.CACHE_CONTROL, cacheControl);
    }

    /**
     * Set message encoding.
     *
     * @param encoding message encoding to be set.
     */
    public void encoding(final String encoding) {
        if (encoding == null) {
            getHeaders().remove(HttpHeaders.CONTENT_ENCODING);
        } else {
            getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, encoding);
        }
    }

    /**
     * Set message language.
     *
     * @param language message language to be set.
     */
    public void language(final String language) {
        if (language == null) {
            getHeaders().remove(HttpHeaders.CONTENT_LANGUAGE);
        } else {
            getHeaders().putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        }
    }

    /**
     * Set message language.
     *
     * @param language message language to be set.
     */
    public void language(final Locale language) {
        if (language == null) {
            getHeaders().remove(HttpHeaders.CONTENT_LANGUAGE);
        } else {
            getHeaders().putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        }
    }

    /**
     * Set message content type.
     *
     * @param type message content type to be set.
     */
    public void type(final MediaType type) {
        setMediaType(type);
    }

    /**
     * Set message content type.
     *
     * @param type message content type to be set.
     */
    public void type(final String type) {
        type(type == null ? null : MediaType.valueOf(type));
    }

    /**
     * Set message content variant (type, language and encoding).
     *
     * @param variant message content content variant (type, language and encoding)
     *                to be set.
     */
    public void variant(final Variant variant) {
        if (variant == null) {
            type((MediaType) null);
            language((String) null);
            encoding(null);
        } else {
            type(variant.getMediaType());
            language(variant.getLanguage());
            encoding(variant.getEncoding());
        }
    }

    /**
     * Returns true if the request is called asynchronously using {@link javax.ws.rs.client.AsyncInvoker}
     *
     * @return True if the request is asynchronous; false otherwise.
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }

    /**
     * Sets the flag indicating whether the request is called asynchronously using {@link javax.ws.rs.client.AsyncInvoker}.
     *
     * @param async True if the request is asynchronous; false otherwise.
     */
    void setAsynchronous(final boolean async) {
        asynchronous = async;
    }

    /**
     * Enable a buffering of serialized entity. The buffering will be configured from runtime configuration
     * associated with this request. The property determining the size of the buffer
     * is {@link org.glassfish.jersey.CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER}.
     * <p/>
     * The buffering functionality is by default disabled and could be enabled by calling this method. In this case
     * this method must be called before first bytes are written to the {@link #getEntityStream() entity stream}.
     *
     */
    public void enableBuffering() {
        enableBuffering(getConfiguration());
    }

    /**
     * Write (serialize) the entity set in this request into the {@link #getEntityStream() entity stream}. The method
     * use {@link javax.ws.rs.ext.WriterInterceptor writer interceptors} and {@link javax.ws.rs.ext.MessageBodyWriter
     * message body writer}.
     * <p/>
     * This method modifies the state of this request and therefore it can be called only once per request life cycle otherwise
     * IllegalStateException is thrown.
     * <p/>
     * Note that {@link #setStreamProvider(org.glassfish.jersey.message.internal.OutboundMessageContext.StreamProvider)}
     * and optionally {@link #enableBuffering()} must be called before calling this method.
     *
     * @throws IOException In the case of IO error.
     */
    public void writeEntity() throws IOException {
        Preconditions.checkState(!entityWritten, LocalizationMessages.REQUEST_ENTITY_ALREADY_WRITTEN());
        entityWritten = true;
        ensureMediaType();
        final GenericType<?> entityType = new GenericType(getEntityType());
        doWriteEntity(workers, entityType);
    }

    /**
     * Added only to make the code testable.
     *
     * @param writeWorkers Message body workers instance used to write the entity.
     * @param entityType   entity type.
     * @throws IOException when {@link MessageBodyWorkers#writeTo(Object, Class, Type, Annotation[], MediaType,
     *                     MultivaluedMap, PropertiesDelegate, OutputStream, Iterable)} throws an {@link IOException}.
     *                     This state is always regarded as connection failure.
     */
    /* package */ void doWriteEntity(final MessageBodyWorkers writeWorkers, final GenericType<?> entityType) throws IOException {
        OutputStream entityStream = null;
        boolean connectionFailed = false;
        boolean runtimeException = false;
        try {
            try {
                entityStream = writeWorkers.writeTo(
                        getEntity(),
                        entityType.getRawType(),
                        entityType.getType(),
                        getEntityAnnotations(),
                        getMediaType(),
                        getHeaders(),
                        getPropertiesDelegate(),
                        getEntityStream(),
                        writerInterceptors);
                setEntityStream(entityStream);
            } catch (final IOException e) {
                // JERSEY-2728 - treat SSLException as connection failure
                connectionFailed = true;
                throw e;
            } catch (final RuntimeException e) {
                runtimeException = true;
                throw e;
            }
        } finally {
            // in case we've seen the ConnectException, we won't try to close/commit stream as this would produce just
            // another instance of ConnectException (which would be logged even if the previously thrown one is propagated)
            // However, if another failure occurred, we still have to try to close and commit the stream - and if we experience
            // another failure, there is a valid reason to log it
            if (!connectionFailed) {
                if (entityStream != null) {
                    try {
                        entityStream.close();
                    } catch (final IOException e) {
                        ExceptionUtils.conditionallyReThrow(e, !runtimeException, LOGGER,
                                LocalizationMessages.ERROR_CLOSING_OUTPUT_STREAM(), Level.FINE);
                    } catch (final RuntimeException e) {
                        ExceptionUtils.conditionallyReThrow(e, !runtimeException, LOGGER,
                                LocalizationMessages.ERROR_CLOSING_OUTPUT_STREAM(), Level.FINE);
                    }
                }
                try {
                    commitStream();
                } catch (final IOException e) {
                    ExceptionUtils.conditionallyReThrow(e, !runtimeException, LOGGER,
                            LocalizationMessages.ERROR_COMMITTING_OUTPUT_STREAM(), Level.FINE);
                } catch (final RuntimeException e) {
                    ExceptionUtils.conditionallyReThrow(e, !runtimeException, LOGGER,
                            LocalizationMessages.ERROR_COMMITTING_OUTPUT_STREAM(), Level.FINE);
                }
            }
        }
    }

    private void ensureMediaType() {
        if (getMediaType() == null) {
            // Content-Type is not present choose a default type
            final GenericType<?> entityType = new GenericType(getEntityType());
            final List<MediaType> mediaTypes = workers.getMessageBodyWriterMediaTypes(
                    entityType.getRawType(), entityType.getType(), getEntityAnnotations());

            setMediaType(getMediaType(mediaTypes));
        }
    }

    private MediaType getMediaType(final List<MediaType> mediaTypes) {
        if (mediaTypes.isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            MediaType mediaType = mediaTypes.get(0);
            if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            }
            return mediaType;
        }
    }

    /**
     * Set writer interceptors for this request.
     * @param writerInterceptors Writer interceptors in the interceptor execution order.
     */
    void setWriterInterceptors(final Iterable<WriterInterceptor> writerInterceptors) {
        this.writerInterceptors = writerInterceptors;
    }

    /**
     * Get writer interceptors of this request.
     * @return Writer interceptors in the interceptor execution order.
     */
    public Iterable<WriterInterceptor> getWriterInterceptors() {
        return writerInterceptors;
    }

    /**
     * Get reader interceptors of this request.
     * @return Reader interceptors in the interceptor execution order.
     */
    public Iterable<ReaderInterceptor> getReaderInterceptors() {
        return readerInterceptors;
    }

    /**
     * Set reader interceptors for this request.
     * @param readerInterceptors Reader interceptors in the interceptor execution order.
     */
    void setReaderInterceptors(final Iterable<ReaderInterceptor> readerInterceptors) {
        this.readerInterceptors = readerInterceptors;
    }

    @Override
    public ServiceLocator getServiceLocator() {
        return getClientRuntime().getServiceLocator();
    }

    /**
     * Indicates whether the User-Agent header should be omitted if not directly set to the map of headers.
     *
     * @return {@code true} if the header should be omitted, {@code false} otherwise.
     */
    public boolean ignoreUserAgent() {
        return ignoreUserAgent;
    }

    /**
     * Indicates whether the User-Agent header should be omitted if not directly set to the map of headers.
     *
     * @param ignore {@code true} if the header should be omitted, {@code false} otherwise.
     */
    public void ignoreUserAgent(final boolean ignore) {
        this.ignoreUserAgent = ignore;
    }
}
