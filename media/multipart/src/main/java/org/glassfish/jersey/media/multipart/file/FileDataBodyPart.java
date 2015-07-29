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

import java.io.File;
import java.util.Date;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * An extension of {@link FormDataBodyPart} for associating
 * {@link File} File as a body part entity.
 * <p/>
 * This class may be used to create body parts that contains a file attachments.
 * Appropriate Content-Disposition parameters and Content-Type header will be derived from the file.
 *
 * @author Imran M Yousuf (imran at smartitengineering.com)
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class FileDataBodyPart extends FormDataBodyPart {

    private File fileEntity;

    private MediaTypePredictor predictor = DefaultMediaTypePredictor.getInstance();

    /**
     * A no-args constructor which expects its client to set the values
     * individually, the attributes to be set are fileEntity and name; the
     * media type will be predicted from the fileEntity if not set explicitly.
     */
    public FileDataBodyPart() {
        super();
    }

    /**
     * Constructs the body part with the provided name and file, it predicts the
     * {@link MediaType} of the file provided. For the known media types client
     * will not need to set the media type explicitly.
     *
     * @param name the name of body part.
     * @param fileEntity the file that represents the entity.
     *
     * @see MediaTypePredictor#getMediaTypeFromFile(java.io.File)
     * @see FileDataBodyPart#FileDataBodyPart(java.lang.String, java.io.File, javax.ws.rs.core.MediaType)
     */
    public FileDataBodyPart(final String name, final File fileEntity) {
        this(name, fileEntity, null);
    }

    /**
     * Constructs the body part with all the attributes set for its proper
     * function. If this constructor is used to construct the body part then it
     * is not required to set any other attributes for proper behavior.
     *
     * @param name the name of body part.
     * @param fileEntity the file that represents the entity.
     * @param mediaType the {@link MediaType} of the body part.
     * @throws java.lang.IllegalArgumentException if the fileEntity is {@code null}.
     */
    public FileDataBodyPart(final String name, final File fileEntity, final MediaType mediaType) throws IllegalArgumentException {
        super();
        super.setName(name);

        if (mediaType != null) {
            setFileEntity(fileEntity, mediaType);
        } else {
            setFileEntity(fileEntity, predictMediaType(fileEntity));
        }
    }

    /**
     * Gets the file for this body part.
     *
     * @return file entity for this body part.
     */
    public File getFileEntity() {
        return fileEntity;
    }

    /**
     * This operation is not supported from this implementation.
     *
     * @param mediaType the media type for this field value.
     * @param value the field value as a Java object.
     * @throws java.lang.UnsupportedOperationException Operation not supported.
     *
     * @see FileDataBodyPart#setFileEntity(java.io.File, javax.ws.rs.core.MediaType)
     */
    @Override
    public void setValue(final MediaType mediaType, final Object value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("It is unsupported, please use setFileEntity instead!");
    }

    /**
     * This operation is not supported from this implementation.
     *
     * @param entity the new entity object.
     * @throws java.lang.UnsupportedOperationException Operation not supported.
     *
     * @see FileDataBodyPart#setFileEntity(java.io.File)
     */
    @Override
    public void setEntity(final Object entity) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("It is unsupported, please use setFileEntity instead!");
    }

    /**
     * Sets the fileEntity for this {@link FormDataBodyPart}.
     *
     * @param fileEntity the entity of this {@link FormDataBodyPart}.
     */
    public void setFileEntity(final File fileEntity) {
        this.setFileEntity(fileEntity, predictMediaType());
    }

    /**
     * Sets the {@link MediaType} and fileEntity for this {@link FormDataBodyPart}.
     *
     * @param fileEntity the entity of this body part.
     * @param mediaType the media type.
     */
    public void setFileEntity(final File fileEntity, final MediaType mediaType) {
        super.setMediaType(mediaType);

        super.setEntity(fileEntity);
        this.fileEntity = fileEntity;

        if (fileEntity != null) {
            FormDataContentDisposition.FormDataContentDispositionBuilder builder =
                    FormDataContentDisposition.name(getName());
            builder.fileName(fileEntity.getName());
            if (fileEntity.exists()) {
                builder.size(fileEntity.length());
                builder.modificationDate(new Date(fileEntity.lastModified()));
            }
            setFormDataContentDisposition(builder.build());
        }
    }

    /**
     * Predicts the media type of the current fileEntity.
     *
     * @return predicted {@link MediaType}.
     */
    protected MediaType predictMediaType() {
        return predictMediaType(getFileEntity());
    }

    /**
     * Predicts the media type of the provided {@link File}.
     *
     * @param file the file from which the media type is predicted.
     * @return predicted {@link MediaType}.
     */
    protected MediaType predictMediaType(final File file) {
        return getPredictor().getMediaTypeFromFile(file);
    }

    /**
     * Gets the media type predictor.
     *
     * @return the media type predictor.
     */
    public MediaTypePredictor getPredictor() {
        return predictor;
    }

    /**
     * Sets the media type predictor.
     *
     * @param predictor the media type predictor.
     */
    public void setPredictor(MediaTypePredictor predictor) {
        if (predictor == null) {
            throw new IllegalArgumentException();
        }

        this.predictor = predictor;
    }

}
