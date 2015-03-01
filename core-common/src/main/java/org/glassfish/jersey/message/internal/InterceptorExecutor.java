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

package org.glassfish.jersey.message.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.InterceptorContext;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.internal.PropertiesDelegate;

/**
 * Abstract class with implementation of {@link InterceptorContext} which is common for {@link ReaderInterceptorContext}
 * and {@link WriterInterceptorContext} implementations.
 *
 * @author Miroslav Fuksa
 */
abstract class InterceptorExecutor<T> implements InterceptorContext, PropertiesDelegate {

    private final PropertiesDelegate propertiesDelegate;
    private Annotation[] annotations;
    private Class<?> type;
    private Type genericType;
    private MediaType mediaType;

    private final TracingLogger tracingLogger;
    private InterceptorTimestampPair<T> lastTracedInterceptor;

    /**
     * Holder of interceptor instance and timestamp of the interceptor invocation (in ns).
     */
    private static class InterceptorTimestampPair<T> {

        private final T interceptor;
        private final long timestamp;

        private InterceptorTimestampPair(final T interceptor, final long timestamp) {
            this.interceptor = interceptor;
            this.timestamp = timestamp;
        }

        private T getInterceptor() {
            return interceptor;
        }

        private long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Constructor initializes common properties of this abstract class.
     *
     * @param rawType            raw Java entity type.
     * @param type               generic Java entity type.
     * @param annotations        Annotations on the formal declaration of the resource
     *                           method parameter that is the target of the message body
     *                           conversion. See {@link InterceptorContext#getAnnotations()}.
     * @param mediaType          MediaType of HTTP entity. See {@link InterceptorContext#getMediaType()}.
     * @param propertiesDelegate request-scoped properties delegate.
     */
    public InterceptorExecutor(final Class<?> rawType, final Type type, final Annotation[] annotations, final MediaType mediaType,
                               final PropertiesDelegate propertiesDelegate) {
        super();
        this.type = rawType;
        this.genericType = type;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.propertiesDelegate = propertiesDelegate;
        this.tracingLogger = TracingLogger.getInstance(propertiesDelegate);
    }

    @Override
    public Object getProperty(final String name) {
        return propertiesDelegate.getProperty(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertiesDelegate.getPropertyNames();
    }

    @Override
    public void setProperty(final String name, final Object object) {
        propertiesDelegate.setProperty(name, object);
    }

    @Override
    public void removeProperty(final String name) {
        propertiesDelegate.removeProperty(name);
    }

    /**
     * Get tracing logger instance configured in via properties.
     *
     * @return tracing logger instance.
     */
    protected final TracingLogger getTracingLogger() {
        return tracingLogger;
    }

    /**
     * Tracing support - log invocation of interceptor BEFORE context.proceed() call.
     *
     * @param interceptor invoked interceptor
     * @param event       event type to be tested
     */
    protected final void traceBefore(final T interceptor, final TracingLogger.Event event) {
        if (tracingLogger.isLogEnabled(event)) {
            if ((lastTracedInterceptor != null) && (interceptor != null)) {
                tracingLogger.logDuration(event, lastTracedInterceptor.getTimestamp(), lastTracedInterceptor.getInterceptor());
            }
            lastTracedInterceptor = new InterceptorTimestampPair<T>(interceptor, System.nanoTime());
        }
    }

    /**
     * Tracing support - log invocation of interceptor AFTER context.proceed() call.
     *
     * @param interceptor invoked interceptor
     * @param event       event type to be tested
     */
    protected final void traceAfter(final T interceptor, final TracingLogger.Event event) {
        if (tracingLogger.isLogEnabled(event)) {
            if ((lastTracedInterceptor != null) && (lastTracedInterceptor.getInterceptor() != null)) {
                tracingLogger.logDuration(event, lastTracedInterceptor.getTimestamp(), interceptor);
            }
            lastTracedInterceptor = new InterceptorTimestampPair<T>(interceptor, System.nanoTime());
        }
    }

    /**
     * Clear last traced interceptor information.
     */
    protected final void clearLastTracedInterceptor() {
        lastTracedInterceptor = null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(final Annotation[] annotations) {
        if (annotations == null) {
            throw new NullPointerException("Annotations must not be null.");
        }
        this.annotations = annotations;
    }

    @Override
    public Class getType() {
        return this.type;
    }

    @Override
    public void setType(final Class type) {
        this.type = type;
    }

    @Override
    public Type getGenericType() {
        return genericType;
    }

    @Override
    public void setGenericType(final Type genericType) {
        this.genericType = genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void setMediaType(final MediaType mediaType) {
        this.mediaType = mediaType;
    }

}
