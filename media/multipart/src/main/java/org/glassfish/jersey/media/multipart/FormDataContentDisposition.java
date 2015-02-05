/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Date;

import org.glassfish.jersey.message.internal.HttpHeaderReader;

/**
 * A form-data content disposition header.
 *
 * @author Paul Sandoz
 * @author imran@smartitengineering.com
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class FormDataContentDisposition extends ContentDisposition {

    private final String name;

    /**
     * Constructor for the builder.
     *
     * @param type the disposition type. will be "form-data".
     * @param name the control name.
     * @param fileName the file name.
     * @param creationDate the creation date.
     * @param modificationDate the modification date.
     * @param readDate the read date.
     * @param size the size.
     * @throws IllegalArgumentException if the type is not equal to "form-data"
     *         or the name is {@code null}
     */
    protected FormDataContentDisposition(String type, String name, String fileName,
            Date creationDate, Date modificationDate, Date readDate,
            long size) {
        super(type, fileName, creationDate, modificationDate, readDate, size);
        this.name = name;

        if (!"form-data".equalsIgnoreCase(getType())) {
            throw new IllegalArgumentException("The content disposition type is not equal to form-data");
        }

        if (name == null) {
            throw new IllegalArgumentException("The name parameter is not present");
        }
    }

    public FormDataContentDisposition(String header) throws ParseException {
        this(header, false);
    }

    public FormDataContentDisposition(String header, boolean fileNameFix) throws ParseException {
        this(HttpHeaderReader.newInstance(header), fileNameFix);
    }

    public FormDataContentDisposition(HttpHeaderReader reader, boolean fileNameFix) throws ParseException {
        super(reader, fileNameFix);
        if (!"form-data".equalsIgnoreCase(getType())) {
            throw new IllegalArgumentException("The content disposition type is not equal to form-data");
        }

        name = getParameters().get("name");
        if (name == null) {
            throw new IllegalArgumentException("The name parameter is not present");
        }
    }

    /**
     * Get the name parameter.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    protected StringBuilder toStringBuffer() {
        StringBuilder sb = super.toStringBuffer();

        addStringParameter(sb, "name", name);

        return sb;
    }

    /**
     * Start building a form data content disposition.
     *
     * @param name the control name.
     * @return the form data content disposition builder.
     */
    public static FormDataContentDispositionBuilder name(String name) {
        return new FormDataContentDispositionBuilder(name);
    }

    /**
     * Builder to build form data content disposition.
     *
     */
    public static class FormDataContentDispositionBuilder
            extends ContentDispositionBuilder<FormDataContentDispositionBuilder, FormDataContentDisposition> {

        private final String name;

        FormDataContentDispositionBuilder(String name) {
            super("form-data");
            this.name = name;
        }

        @Override
        public FormDataContentDisposition build() {
            return new FormDataContentDisposition(type, name, fileName, creationDate, modificationDate, readDate, size);
        }
    }
}
