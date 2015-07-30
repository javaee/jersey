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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.MediaType;

/**
 * A mutable model representing a MIME MultiPart entity.  This class extends
 * {@link BodyPart} because MultiPart entities can be nested inside other
 * MultiPart entities to an arbitrary depth.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class MultiPart extends BodyPart implements Closeable {

    private BodyPartsList bodyParts = new BodyPartsList(this);

    /**
     * Instantiates a new {@link MultiPart} with a {@code mediaType} of
     * {@code multipart/mixed}.
     */
    public MultiPart() {
        super(new MediaType("multipart", "mixed"));
    }


    /**
     * Instantiates a new {@link MultiPart} with the specified characteristics.
     *
     * @param mediaType the {@link MediaType} for this multipart.
     */
    public MultiPart(MediaType mediaType) {
        super(mediaType);
    }

    /**
     * Return a mutable list of {@link BodyPart}s nested in this
     * {@link MultiPart}.
     */
    public List<BodyPart> getBodyParts() {
        return this.bodyParts;
    }

    /**
     * Disables access to the entity for a {@link MultiPart}. Use the list
     * returned by {@code getBodyParts()} to access the relevant
     * {@link BodyPart} instead.
     *
     * @throws IllegalStateException thrown unconditionally.
     */
    @Override
    public Object getEntity() {
        throw new IllegalStateException("Cannot get entity on a MultiPart instance");
    }

    /**
     * Disables access to the entity for a {@link MultiPart}. Use the list
     * returned by {@code getBodyParts()} to access the relevant
     * {@link BodyPart} instead.
     *
     * @param entity
     */
    @Override
    public void setEntity(Object entity) {
        throw new IllegalStateException("Cannot set entity on a MultiPart instance");
    }

    /**
     * Sets the {@link MediaType} for this {@link MultiPart}. If never set,
     * the default {@link MediaType} MUST be {@code multipart/mixed}.
     *
     * @param mediaType the new {@link MediaType}.
     * @throws IllegalArgumentException if the {@code type} property is not set to {@code multipart}.
     */
    @Override
    public void setMediaType(MediaType mediaType) {
        if (!"multipart".equals(mediaType.getType())) {
            throw new IllegalArgumentException(mediaType.toString());
        }
        super.setMediaType(mediaType);
    }

    /**
     * Builder pattern method to add the specified {@link BodyPart} to this
     * {@link MultiPart}.
     *
     * @param bodyPart {@link BodyPart} to be added.
     */
    public MultiPart bodyPart(BodyPart bodyPart) {
        getBodyParts().add(bodyPart);
        return this;
    }

    /**
     * Builder pattern method to add a newly configured {@link BodyPart}
     * to this {@link MultiPart}.
     *
     * @param entity entity object for this body part.
     * @param mediaType content type for this body part.
     */
    public MultiPart bodyPart(Object entity, MediaType mediaType) {
        BodyPart bodyPart = new BodyPart(entity, mediaType);
        return bodyPart(bodyPart);
    }

    /**
     * Override the entity set operation on a {@link MultiPart} to throw
     * {@code IllegalArgumentException}.
     *
     * @param entity entity to set for this {@link BodyPart}.
     */
    @Override
    public BodyPart entity(Object entity) {
        setEntity(entity);
        return this;
    }

    /**
     * Builder pattern method to return this {@link MultiPart} after
     * additional configuration.
     *
     * @param type media type to set for this {@link MultiPart}.
     */
    @Override
    public MultiPart type(MediaType type) {
        setMediaType(type);
        return this;
    }

    /**
     * Performs any necessary cleanup at the end of processing this
     * {@link MultiPart}.
     */
    @Override
    public void cleanup() {
        for (BodyPart bp : getBodyParts()) {
            bp.cleanup();
        }
    }

    public void close() throws IOException {
        cleanup();
    }

}
