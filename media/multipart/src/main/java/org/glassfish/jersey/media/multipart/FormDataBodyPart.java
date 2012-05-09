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
package org.glassfish.jersey.media.multipart;

import java.text.ParseException;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.message.internal.ContentDisposition;
import org.glassfish.jersey.message.internal.FormDataContentDisposition;
import org.glassfish.jersey.message.internal.MediaTypes;

/**
 * Subclass of {@link BodyPart} with specialized support for media type
 * <code>multipart/form-data</code>.  See
 * <a href="http://www.ietf.org/rfc/rfc2388.txt">RFC 2388</a>
 * for the formal definition of this media type.
 * <p/>
 * For a server side application wishing to process an incoming
 * <code>multipart/form-data</code> message, the following features
 * are provided:
 * <ul>
 * <li>Property accessor to retrieve the control name.</li>
 * <li>Property accessor to retrieve the field value for a simple
 * String field.</li>
 * <li>Convenience accessor to retrieve the field value after conversion
 * through an appropriate <code>MessageBodyReader</code>.</li>
 * </ul>
 * <p/>
 * For a client side application wishing to construct an outgoing
 * <code>multipart/form-data</code> message, the following features
 * are provided:
 * <ul>
 * <li>Convenience constructors for named fields with either
 * simple string values, or arbitrary entities and media types.</li>
 * <li>Property accessor to set the control name.</li>
 * <li>Property accessor to set the field value for a simple
 * String field.</li>
 * <li>Convenience accessor to set the media type and value of a
 * "file" field.</li>
 * </ul>
 *
 * @author Craig McClanahan
 * @author Imran M Yousuf (imran at smartitengineering.com)
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class FormDataBodyPart extends BodyPart {

    /**
     * Instantiates an unnamed new {@link FormDataBodyPart} with a
     * <code>mediaType</code> of <code>text/plain</code>.
     */
    public FormDataBodyPart() {
        super();
    }

    /**
     * Instantiates an unnamed {@link FormDataBodyPart} with the
     * specified characteristics.
     *
     * @param mediaType the {@link MediaType} for this body part.
     */
    public FormDataBodyPart(MediaType mediaType) {
        super(mediaType);
    }

    /**
     * Instantiates an unnamed {@link FormDataBodyPart} with the
     * specified characteristics.
     *
     * @param entity the entity for this body part.
     * @param mediaType the {@link MediaType} for this body part.
     */
    public FormDataBodyPart(Object entity, MediaType mediaType) {
        super(entity, mediaType);
    }

    /**
     * Instantiates a named {@link FormDataBodyPart} with a
     * media type of <code>text/plain</code> and String value.
     *
     * @param name the control name for this body part.
     * @param value the value for this body part.
     */
    public FormDataBodyPart(String name, String value) {
        super(value, MediaType.TEXT_PLAIN_TYPE);
        setName(name);
    }

    /**
     * Instantiates a named {@link FormDataBodyPart} with the
     * specified characteristics.
     *
     * @param name the control name for this body part.
     * @param entity the entity for this body part.
     * @param mediaType the {@link MediaType} for this body part.
     */
    public FormDataBodyPart(String name, Object entity, MediaType mediaType) {
        super(entity, mediaType);
        setName(name);
    }

    /**
     * Instantiates a named {@link FormDataBodyPart} with the
     * specified characteristics.
     *
     * @param formDataContentDisposition the content disposition header for this body part.
     * @param value the value for this body part.
     */
    public FormDataBodyPart(FormDataContentDisposition formDataContentDisposition, String value) {
        super(value, MediaType.TEXT_PLAIN_TYPE);
        setFormDataContentDisposition(formDataContentDisposition);
    }

    /**
     * Instantiates a named {@link FormDataBodyPart} with the
     * specified characteristics.
     *
     * @param formDataContentDisposition the content disposition header for this body part.
     * @param entity the entity for this body part.
     * @param mediaType the {@link MediaType} for this body part.
     */
    public FormDataBodyPart(FormDataContentDisposition formDataContentDisposition, Object entity, MediaType mediaType) {
        super(entity, mediaType);
        setFormDataContentDisposition(formDataContentDisposition);
    }

    /**
     * Gets the form data content disposition.
     *
     * @return the form data content disposition.
     */
    public FormDataContentDisposition getFormDataContentDisposition() {
        return (FormDataContentDisposition)getContentDisposition();
    }

    /**
     * Sets the form data content disposition.
     *
     * @param formDataContentDisposition the form data content disposition.
     */
    public void setFormDataContentDisposition(FormDataContentDisposition formDataContentDisposition) {
        super.setContentDisposition(formDataContentDisposition);
    }

    /**
     * Overrides the behaviour on {@link BodyPart} to ensure that
     * only instances of {@link FormDataContentDisposition} can be obtained.
     *
     * @return the content disposition.
     * @throws IllegalArgumentException if the content disposition header
     *                                  cannot be parsed.
     */
    @Override
    public ContentDisposition getContentDisposition() {
        if (contentDisposition == null) {
            String scd = getHeaders().getFirst("Content-Disposition");
            if (scd != null) {
                try {
                    contentDisposition = new FormDataContentDisposition(scd);
                } catch (ParseException ex) {
                    throw new IllegalArgumentException("Error parsing content disposition: " + scd, ex);
                }
            }
        }
        return contentDisposition;
    }

    /**
     * Overrides the behaviour on {@link BodyPart} to ensure that
     * only instances of {@link FormDataContentDisposition} can be set.
     *
     * @param contentDisposition the content disposition which must be an instance of {@link FormDataContentDisposition}.
     * @throws IllegalArgumentException if the content disposition is not an instance of {@link FormDataContentDisposition}.
     */
    @Override
    public void setContentDisposition(ContentDisposition contentDisposition) {
        if (contentDisposition instanceof FormDataContentDisposition) {
            super.setContentDisposition(contentDisposition);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the control name.
     *
     * @return the control name.
     */
    public String getName() {
        FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition();
        if (formDataContentDisposition == null) {
            return null;
        }

        return formDataContentDisposition.getName();
    }

    /**
     * Sets the control name.
     *
     * @param name the control name.
     */
    public void setName(String name) {
        if(name == null) {
            throw new IllegalArgumentException("Name can not be null.");
        }

        if (getFormDataContentDisposition() == null) {
            FormDataContentDisposition contentDisposition;
            contentDisposition = FormDataContentDisposition.name(name)
                .build();
            super.setContentDisposition(contentDisposition);
        } else {
            FormDataContentDisposition formDataContentDisposition = FormDataContentDisposition.name(name)
                    .fileName(contentDisposition.getFileName())
                    .creationDate(contentDisposition.getCreationDate())
                    .modificationDate(contentDisposition.getModificationDate())
                    .readDate(contentDisposition.getReadDate())
                    .size(contentDisposition.getSize())
                    .build();
            super.setContentDisposition(formDataContentDisposition);
        }
    }


    /**
     * Gets the field value for this body part. This should be called
     * only on body parts representing simple field values.
     *
     * @return the simple field value.
     * @throws IllegalStateException if called on a body part with a media type other than <code>text/plain</code>
     */
    public String getValue() {
        if (!MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, getMediaType())) {
            throw new IllegalStateException("Media type is not text/plain");
        }

        if (getEntity() instanceof BodyPartEntity) {
            return getEntityAs(String.class);
        } else {
            return (String) getEntity();
        }
    }

    /**
     * Gets the field value after appropriate conversion to the requested
     * type. This is useful only when the containing {@link FormDataMultiPart}
     * instance has been received, which causes the <code>providers</code>
     * property to have been set.
     *
     * @param <T> the type of the field value.
     * @param clazz Desired class into which the field value should be converted.
     * @return the field value.
     * @throws IllegalArgumentException if no {@code MessageBodyReader} can be found to perform the requested conversion.
     * @throws IllegalStateException if this method is called when the <code>providers</code> property has not been set or when
     * the entity instance is not the unconverted content of the body part entity.
     */
    public <T> T getValueAs(Class<T> clazz) {
        return getEntityAs(clazz);
    }

    /**
     * Sets the field value for this body part. This should be called
     * only on body parts representing simple field values.
     *
     * @param value the field value.
     * @throws IllegalStateException if called on a body part with a media type other than <code>text/plain</code>.
     */
    public void setValue(String value) {
        if (!MediaType.TEXT_PLAIN_TYPE.equals(getMediaType())) {
            throw new IllegalStateException("Media type is not text/plain");
        }
        setEntity(value);
    }

    /**
     * Sets the field media type and value for this body part.
     *
     * @param mediaType the media type for this field value.
     * @param value the field value as a Java object.
     */
    public void setValue(MediaType mediaType, Object value) {
        setMediaType(mediaType);
        setEntity(value);
    }

    /**
     * @return {@code true} if this body part represents a simple, string-based, field value, otherwise {@code false}.
     */
    public boolean isSimple() {
        return MediaType.TEXT_PLAIN_TYPE.equals(getMediaType());
    }

}
