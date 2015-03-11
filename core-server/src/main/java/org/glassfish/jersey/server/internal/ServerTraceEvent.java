/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.message.internal.TracingLogger;

/**
 * Server side tracing events.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @since 2.3
 */
public enum ServerTraceEvent implements TracingLogger.Event {
    /**
     * Request processing started.
     */
    START(TracingLogger.Level.SUMMARY, "START", null),
    /**
     * All HTTP request headers.
     */
    START_HEADERS(TracingLogger.Level.VERBOSE, "START", null),
    /**
     * {@link PreMatching} {@link ContainerRequestFilter} invoked.
     */
    PRE_MATCH(TracingLogger.Level.TRACE, "PRE-MATCH", "Filter by %s"),
    /**
     * {@link PreMatching} {@link ContainerRequestFilter} invocation summary.
     */
    PRE_MATCH_SUMMARY(TracingLogger.Level.SUMMARY, "PRE-MATCH", "PreMatchRequest summary: %s filters"),
    /**
     * Matching path pattern.
     */
    MATCH_PATH_FIND(TracingLogger.Level.TRACE, "MATCH", "Matching path [%s]"),
    /**
     * Path pattern not matched.
     */
    MATCH_PATH_NOT_MATCHED(TracingLogger.Level.VERBOSE, "MATCH", "Pattern [%s] is NOT matched"),
    /**
     * Path pattern matched/selected.
     */
    MATCH_PATH_SELECTED(TracingLogger.Level.TRACE, "MATCH", "Pattern [%s] IS selected"),
    /**
     * Path pattern skipped as higher-priority pattern has been selected already.
     */
    MATCH_PATH_SKIPPED(TracingLogger.Level.VERBOSE, "MATCH", "Pattern [%s] is skipped"),
    /**
     * Matched sub-resource locator method.
     */
    MATCH_LOCATOR(TracingLogger.Level.TRACE, "MATCH", "Matched locator : %s"),
    /**
     * Matched resource method.
     */
    MATCH_RESOURCE_METHOD(TracingLogger.Level.TRACE, "MATCH", "Matched method  : %s"),
    /**
     * Matched runtime resource.
     */
    MATCH_RUNTIME_RESOURCE(TracingLogger.Level.TRACE, "MATCH",
            "Matched resource: template=[%s] regexp=[%s] matches=[%s] from=[%s]"),
    /**
     * Matched resource instance.
     */
    MATCH_RESOURCE(TracingLogger.Level.TRACE, "MATCH", "Resource instance: %s"),
    /**
     * Matching summary.
     */
    MATCH_SUMMARY(TracingLogger.Level.SUMMARY, "MATCH", "RequestMatching summary"),
    /**
     * Global {@link ContainerRequestFilter} invoked.
     */
    REQUEST_FILTER(TracingLogger.Level.TRACE, "REQ-FILTER", "Filter by %s"),
    /**
     * Global {@link ContainerRequestFilter} invocation summary.
     */
    REQUEST_FILTER_SUMMARY(TracingLogger.Level.SUMMARY, "REQ-FILTER", "Request summary: %s filters"),
    /**
     * Resource method invoked.
     */
    METHOD_INVOKE(TracingLogger.Level.SUMMARY, "INVOKE", "Resource %s method=[%s]"),
    /**
     * Resource method invocation results to JAX-RS {@link Response}.
     */
    DISPATCH_RESPONSE(TracingLogger.Level.TRACE, "INVOKE", "Response: %s"),
    /**
     * {@link ContainerResponseFilter} invoked.
     */
    RESPONSE_FILTER(TracingLogger.Level.TRACE, "RESP-FILTER", "Filter by %s"),
    /**
     * {@link ContainerResponseFilter} invocation summary.
     */
    RESPONSE_FILTER_SUMMARY(TracingLogger.Level.SUMMARY, "RESP-FILTER", "Response summary: %s filters"),
    /**
     * Request processing finished.
     */
    FINISHED(TracingLogger.Level.SUMMARY, "FINISHED", "Response status: %s"),
    /**
     * {@link ExceptionMapper} invoked.
     */
    EXCEPTION_MAPPING(TracingLogger.Level.SUMMARY, "EXCEPTION", "Exception mapper %s maps %s ('%s') to <%s>");

    private final TracingLogger.Level level;
    private final String category;
    private final String messageFormat;

    private ServerTraceEvent(TracingLogger.Level level, String category, String messageFormat) {
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
