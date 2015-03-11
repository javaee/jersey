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

import java.util.ArrayList;
import java.util.List;

/**
 * Collects tracing messages for a request.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @since 2.3
 */
final class TracingInfo {

    private final List<Message> messageList = new ArrayList<Message>();

    /**
     * Format time duration in millis with accurate to 2 decimal places.
     *
     * @param duration time duration in nanos
     * @return Formatted duration in millis.
     */
    public static String formatDuration(final long duration) {
        if (duration == 0) {
            return " ----";
        } else {
            return String.format("%5.2f", (duration / 1000000.0));
        }
    }

    /**
     * Format time duration in millis with accurate to 2 decimal places.
     *
     * @param fromTimestamp start of time interval in nanos
     * @param toTimestamp   end of time interval in nanos
     * @return Formatted duration in millis.
     */
    public static String formatDuration(final long fromTimestamp, final long toTimestamp) {
        return formatDuration(toTimestamp - fromTimestamp);
    }

    /**
     * Format {@code value} from {@code top} value in percent with accurate to 2 decimal places.
     *
     * @param value part value according to top
     * @param top   100% value
     * @return Formatted value in percent.
     */
    public static String formatPercent(final long value, final long top) {
        if (value == 0) {
            return "  ----";
        } else {
            return String.format("%6.2f", 100.0 * value / top);
        }
    }

    /**
     * Returns all collected messages enhanced by time duration data.
     *
     * @return all formatted messages
     */
    public String[] getMessages() {
        // Format: EventCategory [duration / sinceRequestTime | duration/requestTime % ]
        // e.g.:   RI [ 3.88 / 8.93 ms | 1.37 %] message text

        final long fromTimestamp = messageList.get(0).getTimestamp() - messageList.get(0).getDuration();
        final long toTimestamp = messageList.get(messageList.size() - 1).getTimestamp();

        final String[] messages = new String[messageList.size()];

        for (int i = 0; i < messages.length; i++) {
            final Message message = messageList.get(i);
            final StringBuilder textSB = new StringBuilder();
            // event
            textSB.append(String.format("%-11s ", message.getEvent().category()));
            // duration
            textSB.append('[')
                    .append(formatDuration(message.getDuration()))
                    .append(" / ")
                    .append(formatDuration(fromTimestamp, message.getTimestamp()))
                    .append(" ms |")
                    .append(formatPercent(message.getDuration(), toTimestamp - fromTimestamp))
                    .append(" %] ");
            // text
            textSB.append(message.toString());
            messages[i] = textSB.toString();
        }
        return messages;
    }

    /**
     * Add other tracing message.
     *
     * @param message tracing message.
     */
    public void addMessage(final Message message) {
        messageList.add(message);
    }

    /**
     * A trace message.
     * It implements message formatting.
     */
    public static class Message {

        /**
         * Event type.
         */
        private final TracingLogger.Event event;
        /**
         * In nanos.
         */
        private final long duration;
        /**
         * In nanos.
         */
        private final long timestamp;
        /**
         * Already formatted text.
         */
        private final String text;

        /**
         * Create a new trace message.
         *
         * @param event trace event.
         * @param duration event duration.
         * @param args message arguments.
         */
        public Message(final TracingLogger.Event event, final long duration, final String[] args) {
            this.event = event;
            this.duration = duration;

            this.timestamp = System.nanoTime();
            if (event.messageFormat() != null) {
                this.text = String.format(event.messageFormat(), (Object[]) args);
            } else {
                final StringBuilder textSB = new StringBuilder();
                for (final String arg : args) {
                    textSB.append(arg).append(' ');
                }
                this.text = textSB.toString();
            }
        }

        private TracingLogger.Event getEvent() {
            return event;
        }

        private long getDuration() {
            return duration;
        }

        private long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
