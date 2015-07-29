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
package org.glassfish.jersey.media.multipart.file;

import java.io.InputStream;
import java.text.MessageFormat;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * Represents an {@link InputStream} based file submission as a part of the
 * multipart/form-data.
 * <p/>
 * It sets the {@link InputStream} as a body part with the default
 * {@link MediaType#APPLICATION_OCTET_STREAM_TYPE} (if not specified by the
 * user).
 * <p/>
 * Note: The MIME type of the entity cannot be automatically predicted as in case
 * of {@link FileDataBodyPart}.
 * <p/>
 * The filename of the attachment is set by the user or defaults to the part's
 * name.
 *
 * @author Pedro Kowalski (pallipp at gmail.com)
 * @author Michal Gajdos
 *
 * @see FileDataBodyPart
 */
public class StreamDataBodyPart extends FormDataBodyPart {

    /**
     * Underlying input stream entity to be sent.
     */
    private InputStream streamEntity;

    /**
     * Filename of the attachment (stream) set by the user.
     */
    private String filename;

    /**
     * Default constructor which forces user to manually set
     * the required ({@code name} and {@code streamEntity})
     * properties.
     * <p/>
     * {@link StreamDataBodyPart#setFilename(String)} can be used to set
     * user-specified attachment filename instead of the default one.
     *
     * @see FormDataBodyPart#setName(String)
     * @see StreamDataBodyPart#setStreamEntity(InputStream, MediaType)
     */
    public StreamDataBodyPart() {
    }

    /**
     * Convenience constructor which assumes the defaults for:
     * {@code filename} (part's name) and {@code mediaType} (
     * {@link MediaType#APPLICATION_OCTET_STREAM_TYPE}).
     * <p/>
     * It builds the requested body part and makes the part ready for
     * submission.
     *
     * @param name         name of the form-data field.
     * @param streamEntity entity to be set as a body part.
     */
    public StreamDataBodyPart(final String name, final InputStream streamEntity) {
        this(name, streamEntity, null, null);
    }

    /**
     * Convenience constructor which assumes the defaults for the
     * {@code mediaType} ({@link MediaType#APPLICATION_OCTET_STREAM_TYPE}).
     * <p/>
     * It builds the requested body part and makes the part ready for
     * submission.
     *
     * @param name         name of the form-data field.
     * @param streamEntity entity to be set as a body part.
     * @param filename     filename of the sent attachment (to be set as a part of
     *                     {@code content-disposition}).
     */
    public StreamDataBodyPart(final String name, final InputStream streamEntity, final String filename) {
        this(name, streamEntity, filename, null);
    }

    /**
     * All-arguments constructor with all requested parameters set by the
     * caller.
     * <p/>
     * It builds the requested body part and makes the part ready for
     * submission.
     *
     * @param name         name of the form-data field.
     * @param streamEntity entity to be set as a body part.
     * @param filename     filename of the sent attachment (to be set as a part of
     *                     {@code content-disposition}).
     * @param mediaType    MIME type of the {@code streamEntity} attachment.
     * @throws IllegalArgumentException if {@code name} or {@code streamEntity} are {@code null}.
     */
    public StreamDataBodyPart(final String name, final InputStream streamEntity,
                              final String filename,
                              final MediaType mediaType) {

        // Not allowed in non-default constructor invocation.
        if (name == null || streamEntity == null) {
            MessageFormat msg = new MessageFormat(
                    "Neither the \"name\" nor \"streamEntity\" can be null. Passed values: \"{0}\" \"{1}\"");
            throw new IllegalArgumentException(msg.format(new Object[] {name, streamEntity}));
        }

        setFilename(filename);

        // Be sure to hit the parent (non-overloaded) method.
        super.setName(name);

        if (mediaType != null) {
            setStreamEntity(streamEntity, mediaType);
        } else {
            setStreamEntity(streamEntity, getDefaultMediaType());
        }
    }

    /**
     * This operation is not supported from this implementation.
     *
     * @throws java.lang.UnsupportedOperationException Operation not supported.
     *
     * @see StreamDataBodyPart#setStreamEntity(InputStream, MediaType)
     */
    @Override
    public void setValue(final MediaType mediaType, final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("It is unsupported, please use setStreamEntity(-) instead!");
    }

    /**
     * This operation is not supported from this implementation.
     *
     * @throws java.lang.UnsupportedOperationException Operation not supported.
     *
     * @see StreamDataBodyPart#setStreamEntity(InputStream)
     */
    @Override
    public void setValue(final String value) {
        throw new UnsupportedOperationException("It is unsupported, please use setStreamEntity(-) instead!");
    }

    /**
     * This operation is not supported from this implementation.
     *
     * @throws java.lang.UnsupportedOperationException Operation not supported.
     *
     * @see StreamDataBodyPart#setStreamEntity(InputStream, MediaType)
     */
    @Override
    public void setEntity(final Object entity) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("It is unsupported, please use setStreamEntity(-) instead!");
    }

    /**
     * Allows to explicitly set the body part entity. This method assumes the
     * default {@link MediaType#APPLICATION_OCTET_STREAM} MIME type and doesn't
     * have to be invoked if one of the non-default constructors was already
     * called.
     * <p/>
     * Either this method or {@link StreamDataBodyPart#setStreamEntity(InputStream, MediaType)}
     * <strong>must</strong> be invoked if the default constructor was called.
     *
     * @param streamEntity entity to be set as a body part.
     */
    public void setStreamEntity(final InputStream streamEntity) {
        this.setStreamEntity(streamEntity, getDefaultMediaType());
    }

    /**
     * Allows to explicitly set the value and the MIME type of the body part
     * entity. This method doesn't have to be invoked if one of the non-default
     * constructors was already called.
     * <p/>
     * Either this method or {@link StreamDataBodyPart#setStreamEntity(InputStream)}
     * <strong>must</strong> be invoked if the default constructor was called.
     *
     * @param streamEntity entity to be set as a body part.
     * @param mediaType MIME type of the {@code streamEntity} attachment.
     */
    public void setStreamEntity(final InputStream streamEntity, MediaType mediaType) {
        if (streamEntity == null) {
            throw new IllegalArgumentException("Stream body part entity cannot be null.");
        }

        if (mediaType == null) {
            mediaType = getDefaultMediaType();
        }

        this.streamEntity = streamEntity;

        // Be sure to hit the parent (non-overloaded) method.
        super.setMediaType(mediaType);
        super.setEntity(streamEntity);

        setFormDataContentDisposition(buildContentDisposition());
    }

    /**
     * Builds the body part content-disposition header which the specified
     * filename (or the default one if unspecified).
     *
     * @return ready to use content-disposition header.
     */
    protected FormDataContentDisposition buildContentDisposition() {
        FormDataContentDisposition.FormDataContentDispositionBuilder builder = FormDataContentDisposition.name(getName());

        if (filename != null) {
            builder.fileName(filename);
        } else {
            // Default is to set the name of the file as a form-field name.
            builder.fileName(getName());
        }

        return builder.build();
    }

    /**
     * Gets the default {@link MediaType} to be used if the user didn't specify
     * any.
     *
     * @return default {@link MediaType} for this body part entity.
     */
    protected static MediaType getDefaultMediaType() {
        return MediaType.APPLICATION_OCTET_STREAM_TYPE;
    }

    /**
     * Sets the body part entity filename value to be used in the
     * content-disposition header.
     *
     * @param filename name to be used.
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * Gets the underlying stream entity which will form the body part entity.
     *
     * @return underlying stream.
     */
    public InputStream getStreamEntity() {
        return streamEntity;
    }

    /**
     * Gets the filename value which is to be used in the content-disposition
     * header of this body part entity.
     *
     * @return filename.
     */
    public String getFilename() {
        return filename;
    }

}
