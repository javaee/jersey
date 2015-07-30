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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.media.multipart.internal.LocalizationMessages;

import org.jvnet.mimepull.MIMEPart;

/**
 * Proxy class representing the entity of a {@link BodyPart} when a
 * {@link MultiPart} entity is received and parsed.
 * <p/>
 * Its primary purpose is to provide an input stream to retrieve the actual data.
 * However, it also transparently deals with storing the data in a temporary disk
 * file, if it is larger than a configurable size; otherwise, the data is stored
 * in memory for faster processing.
 *
 * @author Craig McClanahan
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class BodyPartEntity implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(BodyPartEntity.class.getName());

    private final MIMEPart mimePart;
    private volatile File file;

    /**
     * Constructs a new {@code BodyPartEntity} with a {@link MIMEPart}.
     *
     * @param mimePart MIMEPart containing the input stream of this body part entity.
     */
    public BodyPartEntity(final MIMEPart mimePart) {
        this.mimePart = mimePart;
    }

    /**
     * Gets the input stream of the raw bytes of this body part entity.
     *
     * @return the input stream of the body part entity.
     */
    public InputStream getInputStream() {
        return mimePart.read();
    }

    /**
     * Cleans up temporary file(s), if any were utilized.
     */
    public void cleanup() {
        mimePart.close();

        if (file != null) {
            final boolean deleted = file.delete();
            if (!deleted) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, LocalizationMessages.TEMP_FILE_NOT_DELETED(file.getAbsolutePath()));
                }
            }
        }
    }

    /**
     * Defers to {@link #cleanup}.
     */
    public void close() throws IOException {
        cleanup();
    }

    /**
     * Move the contents of the underlying {@link java.io.InputStream} or {@link java.io.File} to the given file.
     *
     * @param file destination file.
     */
    public void moveTo(final File file) {
        mimePart.moveTo(file);

        // Remember the file where the mime-part object should be stored. Mimepull would not be able to delete it after
        // it's moved.
        this.file = file;
    }
}
