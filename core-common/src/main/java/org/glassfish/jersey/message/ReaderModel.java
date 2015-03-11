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
import javax.ws.rs.ext.MessageBodyReader;

import org.glassfish.jersey.message.internal.MessageBodyFactory;

/**
 * {@link javax.ws.rs.ext.MessageBodyReader} model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.16
 */
public final class ReaderModel extends AbstractEntityProviderModel<MessageBodyReader> {

    /**
     * Create new reader model instance.
     *
     * NOTE: This constructor is package-private on purpose.
     *
     * @param provider modelled message body reader instance.
     * @param types    supported media types as declared in {@code @Consumes} annotation attached to the provider class.
     * @param custom   custom flag.
     */
    public ReaderModel(MessageBodyReader provider, List<MediaType> types, Boolean custom) {
        super(provider, types, custom, MessageBodyReader.class);
    }

    /**
     * Safely invokes {@link javax.ws.rs.ext.MessageBodyReader#isReadable isReadable} method on the underlying provider.
     *
     * Any exceptions will be logged at finer level.
     *
     * @param type        the class of instance to be produced.
     * @param genericType the type of instance to be produced. E.g. if the
     *                    message body is to be converted into a method parameter, this will be
     *                    the formal type of the method parameter as returned by
     *                    {@code Method.getGenericParameterTypes}.
     * @param annotations an array of the annotations on the declaration of the
     *                    artifact that will be initialized with the produced instance. E.g. if the
     *                    message body is to be converted into a method parameter, this will be
     *                    the annotations on that parameter returned by
     *                    {@code Method.getParameterAnnotations}.
     * @param mediaType   the media type of the HTTP entity, if one is not
     *                    specified in the request then {@code application/octet-stream} is
     *                    used.
     * @return {@code true} if the type is supported, otherwise {@code false}.
     */
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MessageBodyFactory.isReadable(super.provider(), type, genericType, annotations, mediaType);
    }
}
