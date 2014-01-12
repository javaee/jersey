/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Common tracing events.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @since 2.3
 */
public enum MsgTraceEvent implements TracingLogger.Event {
    /**
     * {@link javax.ws.rs.ext.ReaderInterceptor} invocation before a call to {@code context.proceed()}.
     */
    RI_BEFORE(TracingLogger.Level.TRACE, "RI", "%s BEFORE context.proceed()"),
    /**
     * {@link javax.ws.rs.ext.ReaderInterceptor} invocation after a call to {@code context.proceed()}.
     */
    RI_AFTER(TracingLogger.Level.TRACE, "RI", "%s AFTER context.proceed()"),
    /**
     * {@link javax.ws.rs.ext.ReaderInterceptor} invocation summary.
     */
    RI_SUMMARY(TracingLogger.Level.SUMMARY, "RI", "ReadFrom summary: %s interceptors"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyReader} lookup.
     */
    MBR_FIND(TracingLogger.Level.TRACE, "MBR", "Find MBR for type=[%s] genericType=[%s] mediaType=[%s] annotations=%s"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyReader#isReadable} returned {@code false}.
     */
    MBR_NOT_READABLE(TracingLogger.Level.VERBOSE, "MBR", "%s is NOT readable"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyReader} selected.
     */
    MBR_SELECTED(TracingLogger.Level.TRACE, "MBR", "%s IS readable"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyReader} skipped as higher-priority reader has been selected already.
     */
    MBR_SKIPPED(TracingLogger.Level.VERBOSE, "MBR", "%s is skipped"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyReader#readFrom} invoked.
     */
    MBR_READ_FROM(TracingLogger.Level.TRACE, "MBR", "ReadFrom by %s"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
     */
    MBW_FIND(TracingLogger.Level.TRACE, "MBW", "Find MBW for type=[%s] genericType=[%s] mediaType=[%s] annotations=%s"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyWriter#isWriteable} returned {@code false}.
     */
    MBW_NOT_WRITEABLE(TracingLogger.Level.VERBOSE, "MBW", "%s is NOT writeable"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyWriter#isWriteable} selected.
     */
    MBW_SELECTED(TracingLogger.Level.TRACE, "MBW", "%s IS writeable"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyWriter} skipped as higher-priority writer has been selected already.
     */
    MBW_SKIPPED(TracingLogger.Level.VERBOSE, "MBW", "%s is skipped"),
    /**
     * {@link javax.ws.rs.ext.MessageBodyWriter#writeTo} invoked.
     */
    MBW_WRITE_TO(TracingLogger.Level.TRACE, "MBW", "WriteTo by %s"),
    /**
     * {@link javax.ws.rs.ext.WriterInterceptor} invocation before a call to {@code context.proceed()}.
     */
    WI_BEFORE(TracingLogger.Level.TRACE, "WI", "%s BEFORE context.proceed()"),
    /**
     * {@link javax.ws.rs.ext.WriterInterceptor} invocation after a call to {@code context.proceed()}.
     */
    WI_AFTER(TracingLogger.Level.TRACE, "WI", "%s AFTER context.proceed()"),
    /**
     * {@link javax.ws.rs.ext.ReaderInterceptor} invocation summary.
     */
    WI_SUMMARY(TracingLogger.Level.SUMMARY, "WI", "WriteTo summary: %s interceptors");

    private final TracingLogger.Level level;
    private final String category;
    private final String messageFormat;

    private MsgTraceEvent(TracingLogger.Level level, String category, String messageFormat) {
        this.level = level;
        this.category = category;
        this.messageFormat = messageFormat;
    }

    @Override
    public String category() {
        return category;
    }

    @Override
    public TracingLogger.Level level() {
        return level;
    }

    @Override
    public String messageFormat() {
        return messageFormat;
    }
}
