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
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.glassfish.jersey.message.internal.HttpDateFormat;
import org.glassfish.jersey.message.internal.HttpHeaderReader;

/**
 * A content disposition header.
 *
 * @author Paul Sandoz
 * @author imran@smartitengineering.com
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContentDisposition {

    private final CharSequence type;
    private final Map<String, String> parameters;
    private String fileName;
    private Date creationDate;
    private Date modificationDate;
    private Date readDate;
    private long size;

    protected ContentDisposition(final String type, final String fileName, final Date creationDate,
                                 final Date modificationDate, final Date readDate, final long size) {
        this.type = type;
        this.fileName = fileName;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
        this.readDate = readDate;
        this.size = size;
        this.parameters = Collections.emptyMap();
    }

    public ContentDisposition(final String header) throws ParseException {
        this(header, false);
    }

    public ContentDisposition(final String header, final boolean fileNameFix) throws ParseException {
        this(HttpHeaderReader.newInstance(header), fileNameFix);
    }

    public ContentDisposition(final HttpHeaderReader reader, final boolean fileNameFix) throws ParseException {
        reader.hasNext();

        type = reader.nextToken();

        final Map<String, String> paramsOrNull = reader.hasNext()
                ? HttpHeaderReader.readParameters(reader, fileNameFix)
                : null;

        parameters = paramsOrNull == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(paramsOrNull);

        createParameters();
    }

    /**
     * Get the type.
     *
     * @return the type
     */
    public String getType() {
        return (type == null) ? null : type.toString();
    }

    /**
     * Get the parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Get the filename parameter.
     *
     * @return the size
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the creation-date parameter.
     *
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Get the modification-date parameter.
     *
     * @return the modificationDate
     */
    public Date getModificationDate() {
        return modificationDate;
    }

    /**
     * Get the read-date parameter.
     *
     * @return the readDate
     */
    public Date getReadDate() {
        return readDate;
    }

    /**
     * Get the size parameter.
     *
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * Convert the disposition to a "Content-Disposition" header value.
     *
     * @return the "Content-Disposition" value.
     */
    @Override
    public String toString() {
        return toStringBuffer().toString();
    }

    protected StringBuilder toStringBuffer() {
        final StringBuilder sb = new StringBuilder();

        sb.append(type);
        addStringParameter(sb, "filename", fileName);
        addDateParameter(sb, "creation-date", creationDate);
        addDateParameter(sb, "modification-date", modificationDate);
        addDateParameter(sb, "read-date", readDate);
        addLongParameter(sb, "size", size);

        return sb;
    }

    protected void addStringParameter(final StringBuilder sb, final String name, final String p) {
        if (p != null) {
            sb.append("; ").append(name).append("=\"").append(p).append("\"");
        }
    }

    protected void addDateParameter(final StringBuilder sb, final String name, final Date p) {
        if (p != null) {
            sb.append("; ").append(name).append("=\"").append(HttpDateFormat.getPreferredDateFormat().format(p)).append("\"");
        }
    }

    protected void addLongParameter(final StringBuilder sb, final String name, final Long p) {
        if (p != -1) {
            sb.append("; ").append(name).append('=').append(Long.toString(p));
        }
    }

    private void createParameters() throws ParseException {
        fileName = parameters.get("filename");

        creationDate = createDate("creation-date");

        modificationDate = createDate("modification-date");

        readDate = createDate("read-date");

        size = createLong("size");
    }

    private Date createDate(final String name) throws ParseException {
        final String value = parameters.get(name);
        if (value == null) {
            return null;
        }
        return HttpDateFormat.getPreferredDateFormat().parse(value);
    }

    private long createLong(final String name) throws ParseException {
        final String value = parameters.get(name);
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException e) {
            throw new ParseException("Error parsing size parameter of value, " + value, 0);
        }
    }

    /**
     * Start building content disposition.
     *
     * @param type the disposition type.
     * @return the content disposition builder.
     */
    public static ContentDispositionBuilder type(final String type) {
        return new ContentDispositionBuilder(type);
    }

    /**
     * Builder to build content disposition.
     *
     * @param <T> the builder type.
     * @param <V> the content disposition type.
     */
    public static class ContentDispositionBuilder<T extends ContentDispositionBuilder, V extends ContentDisposition> {

        protected String type;
        protected String fileName;
        protected Date creationDate;
        protected Date modificationDate;
        protected Date readDate;
        protected long size = -1;

        ContentDispositionBuilder(final String type) {
            this.type = type;
        }

        /**
         * Add the "file-name" parameter.
         *
         * @param fileName the "file-name" parameter. If null the value
         *        is removed
         * @return this builder.
         */
        @SuppressWarnings("unchecked")
        public T fileName(final String fileName) {
            this.fileName = fileName;
            return (T) this;
        }

        /**
         * Add the "creation-date" parameter.
         *
         * @param creationDate the "creation-date" parameter. If null the value
         *        is removed
         * @return this builder.
         */
        @SuppressWarnings("unchecked")
        public T creationDate(final Date creationDate) {
            this.creationDate = creationDate;
            return (T) this;
        }

        /**
         * Add the "modification-date" parameter.
         *
         * @param modificationDate the "modification-date" parameter. If null the value
         *        is removed
         * @return this builder.
         */
        @SuppressWarnings("unchecked")
        public T modificationDate(final Date modificationDate) {
            this.modificationDate = modificationDate;
            return (T) this;
        }

        /**
         * Add the "read-date" parameter.
         *
         * @param readDate the "read-date" parameter. If null the value
         *        is removed
         * @return this builder.
         */
        @SuppressWarnings("unchecked")
        public T readDate(final Date readDate) {
            this.readDate = readDate;
            return (T) this;
        }

        /**
         * Add the "size" parameter.
         *
         * @param size the "size" parameter. If -1 the value is removed.
         * @return this builder.
         */
        @SuppressWarnings("unchecked")
        public T size(final long size) {
            this.size = size;
            return (T) this;
        }

        /**
         * Build the content disposition.
         *
         * @return the content disposition.
         */
        @SuppressWarnings("unchecked")
        public V build() {
            final ContentDisposition cd = new ContentDisposition(type, fileName, creationDate, modificationDate, readDate, size);
            return (V) cd;
        }
    }
}
