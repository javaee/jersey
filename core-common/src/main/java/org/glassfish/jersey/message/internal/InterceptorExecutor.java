/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.InterceptorContext;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptorContext;

/**
 * Abstract class with implementation of {@link InterceptorContext} which is common for {@link ReaderInterceptorContext}
 * and {@link WriterInterceptorContext} implementations.
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
@SuppressWarnings("rawtypes")
abstract class InterceptorExecutor implements InterceptorContext {
    private final Map<String, Object> properties;
    private Annotation[] annotations;
    private Class type;
    private Type genericType;
    private MediaType mediaType;

    /**
     * Constructor initializes common properties of this abstract class.
     *
     * @param genericType Generic Type (to be read or written to). See {@link InterceptorContext#getGenericType()}
     * @param annotations Annotations on the formal declaration of the resource
     *  method parameter that is the target of the message body
     *  conversion. See {@link InterceptorContext#getAnnotations()}.
     * @param mediaType MediaType of HTTP entity. See {@link InterceptorContext#getMediaType()}.
     * @param properties Get a mutable map of request-scoped properties. See {@link InterceptorContext#getProperties()}.
     */
    public InterceptorExecutor(GenericType genericType, Annotation[] annotations, MediaType mediaType,
            Map<String, Object> properties) {
        super();
        this.type = genericType.getRawType();
        this.genericType = genericType.getType();
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.properties = properties;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    @Override
    public Class getType() {
        return this.type;
    }

    @Override
    public void setType(Class type) {
        this.type = type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public void setGenericType(Type genericType) {
        this.genericType = genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

}
