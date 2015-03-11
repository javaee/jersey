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
package org.glassfish.jersey.message.internal;

import java.text.ParseException;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.spi.HeaderDelegateProvider;
import static org.glassfish.jersey.message.internal.Utils.throwIllegalArgumentExceptionIfNull;

/**
 * Header delegate provider for MediaType.
 *
 * @author Marc Hadley
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Martin Matula
 */
@Singleton
public class MediaTypeProvider implements HeaderDelegateProvider<MediaType> {

    private static final String MEDIA_TYPE_IS_NULL = LocalizationMessages.MEDIA_TYPE_IS_NULL();

    @Override
    public boolean supports(Class<?> type) {
        return MediaType.class.isAssignableFrom(type);
    }

    @Override
    public String toString(MediaType header) {

        throwIllegalArgumentExceptionIfNull(header, MEDIA_TYPE_IS_NULL);

        StringBuilder b = new StringBuilder();
        b.append(header.getType()).append('/').append(header.getSubtype());
        for (Map.Entry<String, String> e : header.getParameters().entrySet()) {
            b.append(";").append(e.getKey()).append('=');
            StringBuilderUtils.appendQuotedIfNonToken(b, e.getValue());
        }
        return b.toString();
    }

    @Override
    public MediaType fromString(String header) {

        throwIllegalArgumentExceptionIfNull(header, MEDIA_TYPE_IS_NULL);

        try {
            return valueOf(HttpHeaderReader.newInstance(header));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(
                    "Error parsing media type '" + header + "'", ex);
        }
    }

    /**
     * Create a new {@link javax.ws.rs.core.MediaType} instance from a header reader.
     *
     * @param reader header reader.
     * @return new {@code MediaType} instance.
     *
     * @throws ParseException in case of a header parsing error.
     */
    public static MediaType valueOf(HttpHeaderReader reader) throws ParseException {
        // Skip any white space
        reader.hasNext();

        // Get the type
        final String type = reader.nextToken().toString();
        reader.nextSeparator('/');
        // Get the subtype
        final String subType = reader.nextToken().toString();

        Map<String, String> params = null;

        if (reader.hasNext()) {
            params = HttpHeaderReader.readParameters(reader);
        }

        return new MediaType(type, subType, params);
    }
}
