/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.message.internal.MessageBodyFactory;

/**
 * {@link javax.ws.rs.ext.MessageBodyWriter} model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.16
 */
public final class WriterModel extends AbstractEntityProviderModel<MessageBodyWriter> {
    /**
     * Create new writer model instance.
     *
     * NOTE: This constructor is package-private on purpose.
     *
     * @param provider modelled message body writer instance.
     * @param types    supported media types as declared in {@code @Consumes} annotation attached to the provider class.
     * @param custom   custom flag.
     */
    public WriterModel(MessageBodyWriter provider, List<MediaType> types, Boolean custom) {
        super(provider, types, custom, MessageBodyWriter.class);
    }

    /**
     * Safely invokes {@link javax.ws.rs.ext.MessageBodyWriter#isWriteable isWriteable} method on the underlying provider.
     *
     * Any exceptions will be logged at finer level.
     *
     * @param type        the class of instance that is to be written.
     * @param genericType the type of instance to be written, obtained either
     *                    by reflection of a resource method return type or via inspection
     *                    of the returned instance. {@link javax.ws.rs.core.GenericEntity}
     *                    provides a way to specify this information at runtime.
     * @param annotations an array of the annotations attached to the message entity instance.
     * @param mediaType   the media type of the HTTP entity.
     * @return {@code true} if the type is supported, otherwise {@code false}.
     */
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MessageBodyFactory.isWriteable(super.provider(), type, genericType, annotations, mediaType);
    }
}
