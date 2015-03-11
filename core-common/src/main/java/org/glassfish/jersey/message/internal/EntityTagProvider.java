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

import javax.ws.rs.core.EntityTag;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.message.internal.HttpHeaderReader.Event;
import org.glassfish.jersey.spi.HeaderDelegateProvider;

import static org.glassfish.jersey.message.internal.Utils.throwIllegalArgumentExceptionIfNull;

/**
 * {@code ETag} {@link HeaderDelegateProvider header delegate provider}.
 *
 * @author Marc Hadley
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Singleton
public class EntityTagProvider implements HeaderDelegateProvider<EntityTag> {

    @Override
    public boolean supports(Class<?> type) {
        return type == EntityTag.class;
    }

    @Override
    public String toString(EntityTag header) {

        throwIllegalArgumentExceptionIfNull(header, LocalizationMessages.ENTITY_TAG_IS_NULL());

        StringBuilder b = new StringBuilder();
        if (header.isWeak()) {
            b.append("W/");
        }
        StringBuilderUtils.appendQuoted(b, header.getValue());
        return b.toString();
    }

    @Override
    public EntityTag fromString(String header) {

        throwIllegalArgumentExceptionIfNull(header, LocalizationMessages.ENTITY_TAG_IS_NULL());

        try {
            HttpHeaderReader reader = HttpHeaderReader.newInstance(header);
            Event e = reader.next(false);
            if (e == Event.QuotedString) {
                return new EntityTag(reader.getEventValue().toString());
            } else if (e == Event.Token) {
                final CharSequence ev = reader.getEventValue();
                if (ev != null && ev.length() > 0 && ev.charAt(0) == 'W') {
                    reader.nextSeparator('/');
                    return new EntityTag(reader.nextQuotedString().toString(), true);
                }
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(
                    "Error parsing entity tag '" + header + "'", ex);
        }

        throw new IllegalArgumentException(
                "Error parsing entity tag '" + header + "'");
    }
}
