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

import javax.ws.rs.core.MediaType;

/**
 * Default implementation of {@link MediaTypePredictor} that uses
 * {@link CommonMediaTypes}.
 *
 * @author Imran M Yousuf (imran at smartitengineering.com)
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class DefaultMediaTypePredictor implements MediaTypePredictor {

    /**
     * This enum represents file extension and MIME types of commonly used file. It
     * is to be noted that all file extension and MIME types are specified in lower
     * case, when checking the extension this should be kept in mind.
     * Currently supported file extension and MIME Types are -
     * <ul>
     *   <li>".xml" - application/xml</li>
     *   <li>".txt" - text/plain</li>
     *   <li>".pdf" - application/pdf</li>
     *   <li>".htm" - text/html</li>
     *   <li>".html" - text/html</li>
     *   <li>".jpg" - image/jpeg</li>
     *   <li>".png" - image/png</li>
     *   <li>".gif" - image/gif</li>
     *   <li>".bmp" - image/bmp</li>
     *   <li>".tar" - application/x-tar</li>
     *   <li>".zip" - application/zip</li>
     *   <li>".gz" - application/x-gzip</li>
     *   <li>".rar" - application/x-rar</li>
     *   <li>".mp3" - audio/mpeg</li>
     *   <li>".wav" - audio/x-wave</li>
     *   <li>".avi" - video/x-msvideo</li>
     *   <li>".mpeg" - video/mpeg</li>
     * </ul>
     */
    public enum CommonMediaTypes {

        XML(".xml", MediaType.APPLICATION_XML_TYPE),
        TXT(".txt", MediaType.TEXT_PLAIN_TYPE),
        HTM(".htm", MediaType.TEXT_HTML_TYPE),
        HTML(".html", MediaType.TEXT_HTML_TYPE),
        PDF(".pdf", new MediaType("application", "pdf")),
        JPG(".jpg", new MediaType("image", "jpeg")),
        PNG(".png", new MediaType("image", "png")),
        GIF(".gif", new MediaType("image", "gif")),
        BMP(".bmp", new MediaType("image", "pdf")),
        TAR(".tar", new MediaType("application", "x-tar")),
        ZIP(".zip", new MediaType("application", "zip")),
        GZ(".gz", new MediaType("application", "x-gzip")),
        RAR(".rar", new MediaType("application", "x-rar")),
        MP3(".mp3", new MediaType("audio", "mpeg")),
        WAV(".wav", new MediaType("audio", "x-wave")),
        AVI(".avi", new MediaType("video", "x-msvideo")),
        MPEG(".mpeg", new MediaType("video", "mpeg"));

        private final String extension;

        private final MediaType mediaType;

        private CommonMediaTypes(final String extension, final MediaType mediaType) {
            if (extension == null || !extension.startsWith(".") || mediaType == null) {
                throw new IllegalArgumentException();
            }
            this.extension = extension;
            this.mediaType = mediaType;
        }

        /**
         * Gets the file extension.
         *
         * @return the file extension.
         */
        public String getExtension() {
            return extension;
        }

        /**
         * Gets the media type.
         *
         * @return the media type.
         */
        public MediaType getMediaType() {
            return mediaType;
        }

        /**
         * A utility method for predicting media type from a file name.
         *
         * @param file the file from which to predict the {@link MediaType}
         * @return the {@link MediaType} for the give file; {@code null} - if file
         *         is null; "application/octet-stream" if extension not recognized.
         *
         * @see CommonMediaTypes#getMediaTypeFromFileName(java.lang.String)
         */
        public static MediaType getMediaTypeFromFile(final File file) {
            if (file == null) {
                return null;
            }
            String fileName = file.getName();
            return getMediaTypeFromFileName(fileName);
        }

        /**
         * A utility method for predicting media type from a file name. If the file
         * name extension is not recognised it will return {@link MediaType} for
         * "*\/*", it will also return the same if the file is {@code null}.
         * Currently supported file extensions can be found at {@link CommonMediaTypes}.
         *
         * @param fileName the file name from which to predict the {@link MediaType}
         * @return the {@link MediaType} for the give file; {@code null} - if file
         *         is null; "application/octet-stream" if extension not recognized.
         */
        public static MediaType getMediaTypeFromFileName(final String fileName) {
            if (fileName == null) {
                return null;
            }
            CommonMediaTypes[] types = CommonMediaTypes.values();
            if (types != null && types.length > 0) {
                for (CommonMediaTypes type : types) {
                    if (fileName.toLowerCase().endsWith(type.getExtension())) {
                        return type.getMediaType();
                    }
                }
            }
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }

    private static final DefaultMediaTypePredictor MEDIA_TYPE_PREDICTOR =
            new DefaultMediaTypePredictor();

    public MediaType getMediaTypeFromFile(File file) {
        return CommonMediaTypes.getMediaTypeFromFile(file);
    }

    public MediaType getMediaTypeFromFileName(String fileName) {
        return CommonMediaTypes.getMediaTypeFromFileName(fileName);
    }

    /**
     * Gets the singleton instance of this class.
     *
     * @return the singleton instance.
     */
    public static DefaultMediaTypePredictor getInstance() {
        return MEDIA_TYPE_PREDICTOR;
    }

}
