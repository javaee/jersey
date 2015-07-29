/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.multipart;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.text.ParseException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.glassfish.jersey.media.multipart.internal.LocalizationMessages;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.HeaderUtils;
import org.glassfish.jersey.message.internal.ParameterizedHeader;

/**
 * A mutable model representing a body part nested inside a MIME MultiPart entity.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class BodyPart {

    protected ContentDisposition contentDisposition = null;

    private Object entity;

    private final MultivaluedMap<String, String> headers = HeaderUtils.createInbound();

    /**
     * Media type of this body part.
     */
    private MediaType mediaType = null;

    public MessageBodyWorkers messageBodyWorkers;

    /**
     * Parent of this body part.
     */
    private MultiPart parent = null;

    private Providers providers = null;

    /**
     * Instantiates a new {@code BodyPart} with a {@code mediaType} of
     * {@code text/plain}.
     */
    public BodyPart() {
        this(MediaType.TEXT_PLAIN_TYPE);
    }

    /**
     * Instantiates a new {@code BodyPart} with the specified characteristics.
     *
     * @param mediaType {@link MediaType} for this body part.
     */
    public BodyPart(final MediaType mediaType) {
        setMediaType(mediaType);
    }

    /**
     * Instantiates a new {@code BodyPart} with the specified characteristics.
     *
     * @param entity entity for this body part.
     * @param mediaType {@link MediaType} for this body part.
     */
    public BodyPart(final Object entity, final MediaType mediaType) {
        setEntity(entity);
        setMediaType(mediaType);
    }

    /**
     * Returns the entity object to be unmarshalled from a request, or to be
     * marshalled on a response.
     *
     * @return an entity of this body part.
     * @throws IllegalStateException if this method is called on a {@link MultiPart} instance; access the underlying
     * {@code BodyPart}s instead
     */
    public Object getEntity() {
        return this.entity;
    }

    /**
     * Set the entity object to be unmarshalled from a request, or to be marshalled on a response.
     *
     * @param entity the new entity object.
     * @throws IllegalStateException if this method is called on a {@link MultiPart} instance; access the underlying
     * {@code BodyPart}s instead
     */
    public void setEntity(final Object entity) {
        this.entity = entity;
    }

    /**
     * Returns a mutable map of HTTP header value(s) for this {@code BodyPart}, keyed by the header name. Key comparisons in
     * the returned map must be case-insensitive.
     * <p/>
     * Note: MIME specifications says only headers that match {@code Content-*} should be included on a {@code BodyPart}.
     *
     * @return mutable map of HTTP header values.
     */
    public MultivaluedMap<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Returns an immutable map of parameterized HTTP header value(s) for this {@code BodyPart},
     * keyed by header name. Key comparisons in the returned map must be case-insensitive. If you wish to modify the headers
     * map for this {@code BodyPart}, modify the map returned by {@code getHeaders()} instead.
     *
     * @return immutable map of HTTP header values.
     * @throws ParseException if an un-expected/in-correct value is found during parsing the headers.
     */
    public MultivaluedMap<String, ParameterizedHeader> getParameterizedHeaders() throws ParseException {
        return new ImmutableMultivaluedMap<>(new ParameterizedHeadersMap(headers));
    }

    /**
     * Gets the content disposition.
     * <p/>
     * The "Content-Disposition" header, if present, will be parsed.
     *
     * @return the content disposition, will be null if not present.
     * @throws IllegalArgumentException if the content disposition header cannot be parsed.
     */
    public ContentDisposition getContentDisposition() {
        if (contentDisposition == null) {
            final String scd = headers.getFirst("Content-Disposition");

            if (scd != null) {
                try {
                    contentDisposition = new ContentDisposition(scd);
                } catch (final ParseException ex) {
                    throw new IllegalArgumentException("Error parsing content disposition: " + scd, ex);
                }
            }
        }
        return contentDisposition;
    }

    /**
     * Sets the content disposition.
     *
     * @param contentDisposition the content disposition.
     */
    public void setContentDisposition(final ContentDisposition contentDisposition) {
        this.contentDisposition = contentDisposition;
        headers.remove("Content-Disposition");
    }

    /**
     * Returns the {@link MediaType} for this {@code BodyPart}. If not
     * set, the default {@link MediaType} MUST be {@code text/plain}.
     *
     * @return media type for this body part.
     */
    public MediaType getMediaType() {
        return this.mediaType;
    }

    /**
     * Sets the {@link MediaType} for this {@code BodyPart}.
     *
     * @param mediaType the new {@link MediaType}.
     * @throws IllegalArgumentException if the {@code mediaType} is {@code null}.
     */
    public void setMediaType(final MediaType mediaType) {
        if (mediaType == null) {
            throw new IllegalArgumentException("mediaType cannot be null");
        }

        this.mediaType = mediaType;
    }

    /**
     * Returns the parent {@link MultiPart} (if any) for this {@code BodyPart}.
     *
     * @return parent of this body type, {@code null} if not set.
     */
    public MultiPart getParent() {
        return this.parent;
    }

    /**
     * Sets the parent {@link MultiPart} (if any) for this {@code BodyPart}.
     *
     * @param parent the new parent.
     */
    public void setParent(final MultiPart parent) {
        this.parent = parent;
    }

    /**
     * Returns the configured {@link Providers} for this {@code BodyPart}.
     *
     * @return providers of this body part.
     */
    public Providers getProviders() {
        return this.providers;
    }

    /**
     * Sets the configured {@link Providers} for this {@code BodyPart}.
     *
     * @param providers the new {@link Providers}.
     */
    public void setProviders(final Providers providers) {
        this.providers = providers;
    }

    /**
     * Perform any necessary cleanup at the end of processing this
     * {@code BodyPart}.
     */
    public void cleanup() {
        if ((getEntity() != null) && (getEntity() instanceof BodyPartEntity)) {
            ((BodyPartEntity) getEntity()).cleanup();
        }
    }

    /**
     * Builder pattern method to return this {@code BodyPart} after additional configuration.
     *
     * @param entity entity to set for this {@code BodyPart}.
     * @return body-part instance.
     */
    public BodyPart entity(final Object entity) {
        setEntity(entity);
        return this;
    }

    /**
     * Returns the entity after appropriate conversion to the requested type. This is useful only when the containing
     * {@link MultiPart} instance has been received, which causes the {@code providers} property to have been set.
     *
     * @param clazz desired class into which the entity should be converted.
     * @return entity after appropriate conversion to the requested type.
     *
     * @throws ProcessingException if an IO error arises during reading an entity.
     * @throws IllegalArgumentException if no {@link MessageBodyReader} can be found to perform the requested conversion.
     * @throws IllegalStateException if this method is called when the {@code providers} property has not been set or when the
     * entity instance is not the unconverted content of the body part entity.
     */
    public <T> T getEntityAs(final Class<T> clazz) {
        if (entity == null || !(entity instanceof BodyPartEntity)) {
            throw new IllegalStateException(LocalizationMessages.ENTITY_HAS_WRONG_TYPE());
        }
        if (clazz == BodyPartEntity.class) {
            return clazz.cast(entity);
        }

        final Annotation[] annotations = new Annotation[0];
        final MessageBodyReader<T> reader = messageBodyWorkers.getMessageBodyReader(clazz, clazz, annotations, mediaType);
        if (reader == null) {
            throw new IllegalArgumentException(LocalizationMessages.NO_AVAILABLE_MBR(clazz, mediaType));
        }

        try {
            return reader.readFrom(clazz, clazz, annotations, mediaType, headers, ((BodyPartEntity) entity).getInputStream());
        } catch (final IOException ioe) {
            throw new ProcessingException(LocalizationMessages.ERROR_READING_ENTITY(String.class), ioe);
        }
    }

    /**
     * Builder pattern method to return this {@code BodyPart} after additional configuration.
     *
     * @param type media type to set for this {@code BodyPart}.
     * @return body-part instance.
     */
    public BodyPart type(final MediaType type) {
        setMediaType(type);
        return this;
    }

    /**
     * Builder pattern method to return this {@code BodyPart} after
     * additional configuration.
     *
     * @param contentDisposition content disposition to set for this {@code BodyPart}.
     * @return body-part instance.
     */
    public BodyPart contentDisposition(final ContentDisposition contentDisposition) {
        setContentDisposition(contentDisposition);
        return this;
    }

    /**
     * Set message body workers used to transform an entity stream into particular Java type.
     *
     * @param messageBodyWorkers message body workers.
     */
    public void setMessageBodyWorkers(final MessageBodyWorkers messageBodyWorkers) {
        this.messageBodyWorkers = messageBodyWorkers;
    }
}
