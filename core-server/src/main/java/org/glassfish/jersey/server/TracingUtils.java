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
package org.glassfish.jersey.server;

import java.util.List;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.server.internal.ServerTraceEvent;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Utilities for tracing support.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @since 2.3
 */
public final class TracingUtils {

    private static final List<String> SUMMARY_HEADERS = Lists.newArrayList();

    static {
        SUMMARY_HEADERS.add(HttpHeaders.ACCEPT.toLowerCase());
        SUMMARY_HEADERS.add(HttpHeaders.ACCEPT_ENCODING.toLowerCase());
        SUMMARY_HEADERS.add(HttpHeaders.ACCEPT_CHARSET.toLowerCase());
        SUMMARY_HEADERS.add(HttpHeaders.ACCEPT_LANGUAGE.toLowerCase());
        SUMMARY_HEADERS.add(HttpHeaders.CONTENT_TYPE.toLowerCase());
        SUMMARY_HEADERS.add(HttpHeaders.CONTENT_LENGTH.toLowerCase());
    }

    private static final TracingConfig DEFAULT_CONFIGURATION_TYPE = TracingConfig.OFF;

    private TracingUtils() {
    }

    /**
     * According to configuration/request header it initialize {@link TracingLogger} and put it to the request properties.
     *
     * @param type             application-wide tracing configuration type.
     * @param appThreshold     application-wide tracing level threshold.
     * @param containerRequest request instance to get runtime properties to store {@link TracingLogger} instance to
     *                         if tracing support is enabled for the request.
     */
    public static void initTracingSupport(TracingConfig type,
                                          TracingLogger.Level appThreshold,
                                          ContainerRequest containerRequest) {
        final TracingLogger tracingLogger;
        if (isTracingSupportEnabled(type, containerRequest)) {
            tracingLogger = TracingLogger.create(
                    getTracingThreshold(appThreshold, containerRequest),
                    getTracingLoggerNameSuffix(containerRequest));
        } else {
            tracingLogger = TracingLogger.empty();
        }

        containerRequest.setProperty(TracingLogger.PROPERTY_NAME, tracingLogger);
    }

    /**
     * Log tracing messages START events.
     *
     * @param request container request instance to get runtime properties
     *                to check if tracing support is enabled for the request.
     */
    public static void logStart(ContainerRequest request) {
        TracingLogger tracingLogger = TracingLogger.getInstance(request);
        if (tracingLogger.isLogEnabled(ServerTraceEvent.START)) {
            StringBuilder textSB = new StringBuilder();
            textSB.append(String.format("baseUri=[%s] requestUri=[%s] method=[%s] authScheme=[%s]",
                    request.getBaseUri(), request.getRequestUri(), request.getMethod(),
                    toStringOrNA(request.getSecurityContext().getAuthenticationScheme())));
            for (String header : SUMMARY_HEADERS) {
                textSB.append(String.format(" %s=%s", header, toStringOrNA(request.getRequestHeaders().get(header))));
            }
            tracingLogger.log(ServerTraceEvent.START, textSB.toString());
        }
        if (tracingLogger.isLogEnabled(ServerTraceEvent.START_HEADERS)) {
            StringBuilder textSB = new StringBuilder();
            for (String header : request.getRequestHeaders().keySet()) {
                if (!SUMMARY_HEADERS.contains(header)) {
                    textSB.append(String.format(" %s=%s", header, toStringOrNA(request.getRequestHeaders().get(header))));
                }
            }
            if (textSB.length() > 0) {
                textSB.insert(0, "Other request headers:");
            }
            tracingLogger.log(ServerTraceEvent.START_HEADERS, textSB.toString());
        }
    }

    /**
     * Test if application and request settings enabled tracing support.
     *
     * @param type             application tracing configuration type.
     * @param containerRequest request instance to check request headers.
     * @return {@code true} if tracing support is switched on for the request.
     */
    private static boolean isTracingSupportEnabled(TracingConfig type, ContainerRequest containerRequest) {
        return (type == TracingConfig.ALL)
                || ((type == TracingConfig.ON_DEMAND) && (containerRequest.getHeaderString(TracingLogger.HEADER_ACCEPT) != null));
    }

    /**
     * Return configuration type of tracing support according to application configuration.
     *
     * By default tracing support is switched OFF.
     *
     * @param configuration application configuration.
     * @return configuration type, transformed text value to enum read from configuration or default.
     */
    /*package*/
    static TracingConfig getTracingConfig(Configuration configuration) {
        final String tracingText = ServerProperties.getValue(configuration.getProperties(),
                ServerProperties.TRACING, String.class);

        final TracingConfig result;
        if (tracingText != null) {
            result = TracingConfig.valueOf(tracingText);
        } else {
            result = DEFAULT_CONFIGURATION_TYPE;
        }
        return result;
    }

    /**
     * Get request header specified JDK logger name suffix.
     *
     * @param request container request instance to get request header {@link TracingLogger#HEADER_LOGGER} value.
     * @return Logger name suffix or {@code null} if not set.
     */
    private static String getTracingLoggerNameSuffix(ContainerRequest request) {
        return request.getHeaderString(TracingLogger.HEADER_LOGGER);
    }

    /**
     * Get application-wide tracing level threshold.
     *
     * @param configuration application configuration.
     * @return tracing level threshold.
     */
    /*package*/
    static TracingLogger.Level getTracingThreshold(Configuration configuration) {
        final String thresholdText = ServerProperties.getValue(
                configuration.getProperties(),
                ServerProperties.TRACING_THRESHOLD, String.class);

        return (thresholdText == null) ? TracingLogger.DEFAULT_LEVEL : TracingLogger.Level.valueOf(thresholdText);
    }

    private static TracingLogger.Level getTracingThreshold(TracingLogger.Level appThreshold, ContainerRequest containerRequest) {
        final String thresholdText = containerRequest.getHeaderString(TracingLogger.HEADER_THRESHOLD);

        return (thresholdText == null) ? appThreshold : TracingLogger.Level.valueOf(thresholdText);
    }

    private static String toStringOrNA(Object object) {
        if (object == null) {
            return "n/a";
        } else {
            return String.valueOf(object);
        }
    }

}
