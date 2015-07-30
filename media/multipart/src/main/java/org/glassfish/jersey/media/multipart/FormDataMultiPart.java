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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.internal.LocalizationMessages;
import org.glassfish.jersey.message.internal.MediaTypes;

/**
 * Subclass of {@link MultiPart} with specialized support for media type {@code multipart/form-data}. See
 * <a href="http://www.ietf.org/rfc/rfc2388.txt">RFC 2388</a> for the formal definition of this media type.
 * <p/>
 * For a server side application wishing to process an incoming {@code multipart/form-data} message, the following features
 * are provided:
 * <ul>
 * <li>Incoming entities will be of type {@link FormDataMultiPart}, enabling access to the specialized methods.</li>
 * <li>Incoming body parts will be of type {@link FormDataBodyPart}, enabling access to its specialized methods.</li>
 * <li>Convenience method to return the {@link FormDataBodyPart} for a specified control name.</li>
 * <li>Convenience method to return a {@code Map} of {@link FormDataBodyPart}s for all fields, keyed by field name.</li>
 * </ul>
 * <p/>
 * For a client side application wishing to construct an outgoing
 * {@code multipart/form-data} message, the following features
 * are provided:
 * <ul>
 * <li>Media type of the {@link FormDataMultiPart} instance will automatically set to {@code multipart/form-data}.</li>
 * <li>Builder pattern method to add simple field values as body parts of type {@code text/plain}.</li>
 * <li>Builder pattern method to add named "file" field values with arbitrary media types.</li>
 * </ul>
 * <p/>
 * TODO Consider supporting the use case of a nested {@code multipart/mixed} body part to contain multiple uploaded files.
 *
 * @author Craig McClanahan
 * @author Imran M Yousuf (imran at smartitengineering.com)
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class FormDataMultiPart extends MultiPart {

    /**
     * Instantiates a new {@code FormDataMultiPart} instance with
     * default characteristics.
     */
    public FormDataMultiPart() {
        super(MediaType.MULTIPART_FORM_DATA_TYPE);
    }

    /**
     * Builder pattern method to add a named field with a text value,
     * and return this instance.
     *
     * @param name the control name.
     * @param value the text value.
     * @return this instance.
     */
    public FormDataMultiPart field(String name, String value) {
        getBodyParts().add(new FormDataBodyPart(name, value));
        return this;
    }

    /**
     * Builder pattern method to add a named field with an arbitrary
     * media type and entity, and return this instance.
     *
     * @param name the control name.
     * @param entity entity value for the new field.
     * @param mediaType media type for the new field.
     * @return this instance.
     */
    public FormDataMultiPart field(String name, Object entity, MediaType mediaType) {
        getBodyParts().add(new FormDataBodyPart(name, entity, mediaType));
        return this;
    }

    /**
     * Gets a form data body part given a control name.
     *
     * @param name the control name.
     * @return the form data body part, otherwise null if no part is present with the given control name. If more that one part
     * is present with the same control name, then the first part that occurs is returned.
     */
    public FormDataBodyPart getField(String name) {
        FormDataBodyPart result = null;
        for (BodyPart bodyPart : getBodyParts()) {
            if (!(bodyPart instanceof FormDataBodyPart)) {
                continue;
            }
            if (name.equals(((FormDataBodyPart) bodyPart).getName())) {
                result = (FormDataBodyPart) bodyPart;
            }
        }
        return result;
    }

    /**
     * Gets a list of one or more form data body parts given a control name.
     *
     * @param name the control name.
     * @return the list of form data body parts, otherwise null if no parts are present with the given control name.
     */
    public List<FormDataBodyPart> getFields(String name) {
        List<FormDataBodyPart> result = null;
        for (BodyPart bodyPart : getBodyParts()) {
            if (!(bodyPart instanceof FormDataBodyPart)) {
                continue;
            }
            if (name.equals(((FormDataBodyPart) bodyPart).getName())) {
                if (result == null) {
                    result = new ArrayList<>(1);
                }
                result.add((FormDataBodyPart) bodyPart);
            }
        }
        return result;
    }

    /**
     * Gets a map of form data body parts where the key is the control name
     * and the value is a list of one or more form data body parts.
     *
     * @return return the map of form data body parts.
     */
    public Map<String, List<FormDataBodyPart>> getFields() {
        Map<String, List<FormDataBodyPart>> map = new HashMap<>();
        for (BodyPart bodyPart : getBodyParts()) {
            if (!(bodyPart instanceof FormDataBodyPart)) {
                continue;
            }

            FormDataBodyPart p = (FormDataBodyPart) bodyPart;
            List<FormDataBodyPart> l = map.get(p.getName());
            if (l == null) {
                l = new ArrayList<>(1);
                map.put(p.getName(), l);
            }
            l.add(p);
        }
        return map;
    }

    /**
     * Disables changing the media type to anything other than {@code multipart/form-data}.
     *
     * @param mediaType the proposed media type.
     * @throws IllegalArgumentException if the proposed media type is not {@code multipart/form-data}.
     */
    @Override
    public void setMediaType(MediaType mediaType) {
        if (!MediaTypes.typeEqual(mediaType, MediaType.MULTIPART_FORM_DATA_TYPE)) {
            throw new IllegalArgumentException(LocalizationMessages.FORM_DATA_MULTIPART_CANNOT_CHANGE_MEDIATYPE());
        }
        super.setMediaType(mediaType);
    }

}
