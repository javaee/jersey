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
package org.glassfish.jersey.message.internal;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.annotation.Priority;

import org.glassfish.jersey.internal.PropertiesDelegate;

/**
 * Low level Tracing support API.
 *
 * Use one instance per request.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @since 2.3
 */
public abstract class TracingLogger {

    /**
     * {@code TracingLogger} instance is placed in request context properties under this name.
     */
    public static final String PROPERTY_NAME = TracingLogger.class.getName();
    /**
     * HTTP header prefix.
     */
    private static final String HEADER_TRACING_PREFIX = "X-Jersey-Tracing-";
    /**
     * Request header name to change application default tracing level.
     */
    public static final String HEADER_THRESHOLD = HEADER_TRACING_PREFIX + "Threshold";
    /**
     * Request header name to switch on request tracing.
     * Make sense in case of tracing support enabled by ON_DEMAND value.
     */
    public static final String HEADER_ACCEPT = HEADER_TRACING_PREFIX + "Accept";
    /**
     * Request header name to set JDK logger name suffix to identify a request logs.
     */
    public static final String HEADER_LOGGER = HEADER_TRACING_PREFIX + "Logger";
    /**
     * Response header name format.
     */
    private static final String HEADER_RESPONSE_FORMAT = HEADER_TRACING_PREFIX + "%03d";
    /**
     * Default event level.
     */
    public static final Level DEFAULT_LEVEL = Level.TRACE;
    /**
     * JDK logger name prefix.
     */
    private static final String TRACING_LOGGER_NAME_PREFIX = "org.glassfish.jersey.tracing";
    /**
     * Default JDK logger name suffix. This can be overwrite by header {@link #HEADER_LOGGER}.
     */
    private static final String DEFAULT_LOGGER_NAME_SUFFIX = "general";
    /**
     * Empty (no-op) tracing logger.
     */
    private static final TracingLogger EMPTY = new TracingLogger() {

        @Override
        public boolean isLogEnabled(final Event event) {
            return false;
        }

        @Override
        public void log(final Event event, final Object... args) {
            // no-op
        }

        @Override
        public void logDuration(final Event event, final long fromTimestamp, final Object... args) {
            // no-op
        }

        @Override
        public long timestamp(final Event event) {
            return -1;
        }

        @Override
        public void flush(final MultivaluedMap<String, Object> headers) {
            // no-op
        }
    };

    /**
     * Returns instance of {@code TracingLogger} associated with current request processing
     * ({@code propertiesDelegate}).
     *
     * @param propertiesDelegate request associated runtime properties. Can be {@code null} if not running on server side.
     * @return returns instance of {@code TracingLogger} from {@code propertiesDelegate}. Does not return {@code null}.
     */
    // TODO look for places where getInstance(RequestProcessingContext) would make sense
    public static TracingLogger getInstance(final PropertiesDelegate propertiesDelegate) {
        if (propertiesDelegate == null) {
            //not server side
            return EMPTY;
        }
        final TracingLogger tracingLogger = (TracingLogger) propertiesDelegate.getProperty(PROPERTY_NAME);
        return (tracingLogger != null) ? tracingLogger : EMPTY;
    }

    /**
     * Create new Tracing logger.
     *
     * @param threshold        tracing level threshold.
     * @param loggerNameSuffix tracing logger name suffix.
     * @return new tracing logger.
     */
    public static TracingLogger create(final Level threshold, final String loggerNameSuffix) {
        return new TracingLoggerImpl(threshold, loggerNameSuffix);
    }

    /**
     * Get an empty (no-op) tracing logger instance.
     *
     * @return empty tracing logger instance.
     */
    public static TracingLogger empty() {
        return EMPTY;
    }

    /**
     * Test if a tracing support is enabled (according to {@code propertiesDelegate} setting) and
     * if {@code event} can be logged (according to {@code event.level} and threshold level set).
     *
     * @param event event type to be tested
     * @return {@code true} if {@code event} can be logged
     */
    public abstract boolean isLogEnabled(Event event);

    /**
     * Try to log event according to event level and request context threshold level setting.
     *
     * @param event event type to be logged
     * @param args  message arguments (in relation to {@link org.glassfish.jersey.message.internal.TracingLogger
     * .Event#messageFormat()}
     */
    public abstract void log(Event event, Object... args);

    /**
     * Try to log event according to event level and request context threshold level setting.
     *
     * If logging support is switched on for current request and event setting the method computes duration of event and log
     * message. If {@code fromTimestamp} is not set (i.e. {@code -1}) then duration of event
     * is {@code 0}.
     *
     * @param event         event type to be logged
     * @param fromTimestamp logged event is running from the timestamp in nanos. {@code -1} in case event has no duration
     * @param args          message arguments (in relation to {@link org.glassfish.jersey.message.internal.TracingLogger
     * .Event#messageFormat()}
     */
    public abstract void logDuration(Event event, long fromTimestamp, Object... args);

    /**
     * If logging support is switched on for current request and event setting the method returns current timestamp in nanos.
     *
     * @param event event type to be logged
     * @return Current timestamp in nanos or {@code -1} if tracing is not enabled
     */
    public abstract long timestamp(Event event);

    /**
     * Stores collected tracing messages to response HTTP header.
     *
     * @param headers message headers.
     */
    public abstract void flush(MultivaluedMap<String, Object> headers);

    /**
     * Real implementation of tracing logger.
     */
    private static final class TracingLoggerImpl extends TracingLogger {

        private final Logger logger;
        private final Level threshold;
        private final TracingInfo tracingInfo;

        public TracingLoggerImpl(final Level threshold, String loggerNameSuffix) {
            this.threshold = threshold;

            this.tracingInfo = new TracingInfo();

            loggerNameSuffix = loggerNameSuffix != null ? loggerNameSuffix : DEFAULT_LOGGER_NAME_SUFFIX;
            this.logger = Logger.getLogger(TRACING_LOGGER_NAME_PREFIX + "." + loggerNameSuffix);
        }

        @Override
        public boolean isLogEnabled(final Event event) {
            return isEnabled(event.level());
        }

        @Override
        public void log(final Event event, final Object... args) {
            logDuration(event, -1, args);
        }

        @Override
        public void logDuration(final Event event, final long fromTimestamp, final Object... args) {
            if (isEnabled(event.level())) {
                final long toTimestamp;
                if (fromTimestamp == -1) {
                    toTimestamp = -1;
                } else {
                    toTimestamp = System.nanoTime();
                }
                long duration = 0;
                if ((fromTimestamp != -1) && (toTimestamp != -1)) {
                    duration = toTimestamp - fromTimestamp;
                }
                logImpl(event, duration, args);
            }
        }

        @Override
        public long timestamp(final Event event) {
            if (isEnabled(event.level())) {
                return System.nanoTime();
            }
            return -1;
        }

        @Override
        public void flush(final MultivaluedMap<String, Object> headers) {
            final String[] messages = tracingInfo.getMessages();
            for (int i = 0; i < messages.length; i++) {
                headers.putSingle(String.format(TracingLogger.HEADER_RESPONSE_FORMAT, i), messages[i]);
            }
        }

        /**
         * Log message for specified event type.
         *
         * The event contains name, category, level and also message format.
         * Message format will be formatted by parameter {@code messageArgs} is used to format event.
         * If there is no message format then each message arg is separated by space.
         * Final message also contains event name (JDK Log) or category (HTTP header) and time stamp.
         *
         * @param event       Event type of log
         * @param duration    Time duration of logged event. Can be {@code 0}.
         * @param messageArgs message arguments
         */
        private void logImpl(final Event event, final long duration, final Object... messageArgs) {
            if (isEnabled(event.level())) {
                final String[] messageArgsStr = new String[messageArgs.length];
                for (int i = 0; i < messageArgs.length; i++) {
                    messageArgsStr[i] = formatInstance(messageArgs[i]);
                }
                final TracingInfo.Message message = new TracingInfo.Message(event, duration, messageArgsStr);
                tracingInfo.addMessage(message);

                final java.util.logging.Level loggingLevel;
                switch (event.level()) {
                    case SUMMARY:
                        loggingLevel = java.util.logging.Level.FINE;
                        break;
                    case TRACE:
                        loggingLevel = java.util.logging.Level.FINER;
                        break;
                    case VERBOSE:
                        loggingLevel = java.util.logging.Level.FINEST;
                        break;
                    default:
                        loggingLevel = java.util.logging.Level.OFF;
                }
                if (logger.isLoggable(loggingLevel)) {
                    logger.log(loggingLevel,
                            event.name() + ' ' + message.toString() + " [" + TracingInfo.formatDuration(duration) + " ms]");
                }
            }
        }

        private boolean isEnabled(final Level level) {
            return threshold.ordinal() >= level.ordinal();
        }

        /**
         * Format info of instance.
         *
         * It shows its class name, identity hash code, and following info if available: priority value, response detail.
         *
         * @param instance instance to be formatted
         * @return Formatted info of instance.
         */
        private static String formatInstance(final Object instance) {
            final StringBuilder textSB = new StringBuilder();
            if (instance == null) {
                textSB.append("null");
            } else if ((instance instanceof Number) || (instance instanceof String) || (instance instanceof Method)) {
                textSB.append(instance.toString());
            } else if (instance instanceof Response.StatusType) {
                textSB.append(formatStatusInfo((Response.StatusType) instance));
            } else {
                textSB.append('[');
                formatInstance(instance, textSB);
                if (instance.getClass().isAnnotationPresent(Priority.class)) {
                    textSB.append(" #").append(instance.getClass().getAnnotation(Priority.class).value());
                }
                if (instance instanceof WebApplicationException) {
                    formatResponse(((WebApplicationException) instance).getResponse(), textSB);
                } else if (instance instanceof Response) {
                    formatResponse(((Response) instance), textSB);
                }
                textSB.append(']');
            }
            return textSB.toString();
        }

        /**
         * Basic format of instance - just class name and identity hash code.
         *
         * @param instance instance to be formatted
         * @param textSB   Formatted info will be appended to {@code StringBuilder}
         */
        private static void formatInstance(final Object instance, final StringBuilder textSB) {
            textSB.append(instance.getClass().getName()).append(" @")
                    .append(Integer.toHexString(System.identityHashCode(instance)));
        }

        /**
         * Format of response - status code, status family, reason phrase and info about entity.
         *
         * @param response response to be formatted
         * @param textSB   Formatted info will be appended to {@code StringBuilder}
         */
        private static void formatResponse(final Response response, final StringBuilder textSB) {
            textSB.append(" <").append(formatStatusInfo(response.getStatusInfo())).append('|');
            if (response.hasEntity()) {
                formatInstance(response.getEntity(), textSB);
            } else {
                textSB.append("-no-entity-");
            }
            textSB.append('>');
        }

        private static String formatStatusInfo(final Response.StatusType statusInfo) {
            return String.valueOf(statusInfo.getStatusCode()) + '/' + statusInfo.getFamily() + '|' + statusInfo.getReasonPhrase();
        }
    }

    /**
     * Level of tracing message.
     */
    public static enum Level {
        /**
         * Brief tracing information level.
         */
        SUMMARY,
        /**
         * Detailed tracing information level.
         */
        TRACE,
        /**
         * Extremely detailed tracing information level.
         */
        VERBOSE
    }

    /**
     * Type of event.
     */
    public static interface Event {

        /**
         * Name of event, should be unique.
         * Is logged by JDK logger.
         *
         * @return event name.
         */
        public String name();

        /**
         * Category of event, more events share same category.
         * Is used to format response HTTP header.
         *
         * @return event category.
         */
        public String category();

        /**
         * Level of event.
         * Is used to check if the event is logged according to application/request settings.
         *
         * @return event trace level.
         */
        public Level level();

        /**
         * Message format. Use {@link String#format(String, Object...)} format.
         * Can be null. In that case message arguments are separated by space.
         *
         * @return message format
         */
        public String messageFormat();
    }

}
