/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.simple;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.SelectableChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.util.ExtendedLogger;

import org.simpleframework.common.thread.DaemonFactory;
import org.simpleframework.transport.trace.Trace;
import org.simpleframework.transport.trace.TraceAnalyzer;

/**
 * Tracing at a very low level can be performed with a {@link TraceAnalyzer}. This provides much
 * more useful information than the conventional {@link LoggingFilter} in that it provides details
 * at a very low level. This is very useful when monitoring performance interactions at the TCP
 * level between clients and servers.
 * <p/>
 * Performance overhead for the server is minimal as events are pumped out in batches. The amount of
 * logging information will increase quite significantly though.
 *
 * @author Niall Gallagher
 */
public class SimpleTraceAnalyzer implements TraceAnalyzer {

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(SimpleTraceAnalyzer.class.getName()), Level.FINEST);

    private final TraceConsumer consumer;
    private final ThreadFactory factory;
    private final AtomicBoolean active;
    private final AtomicLong count;

    /**
     * Creates an asynchronous trace event logger.
     */
    public SimpleTraceAnalyzer() {
        this.factory = new DaemonFactory(TraceConsumer.class);
        this.consumer = new TraceConsumer();
        this.active = new AtomicBoolean();
        this.count = new AtomicLong();
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public Trace attach(SelectableChannel channel) {
        long sequence = count.getAndIncrement();
        return new TraceFeeder(channel, sequence);
    }

    /**
     * Begin logging trace events to the underlying logger.
     */
    public void start() {
        if (active.compareAndSet(false, true)) {
            Thread thread = factory.newThread(consumer);
            thread.start();
        }
    }

    @Override
    public void stop() {
        active.set(false);
    }

    private class TraceConsumer implements Runnable {

        private final Queue<TraceRecord> queue;

        public TraceConsumer() {
            this.queue = new ConcurrentLinkedQueue<TraceRecord>();
        }

        public void consume(TraceRecord record) {
            queue.offer(record);
        }

        public void run() {
            try {
                while (active.get()) {
                    Thread.sleep(1000);
                    drain();
                }
            } catch (Exception e) {
                logger.info("Trace analyzer error");
            } finally {
                try {
                    drain();
                } catch (Exception e) {
                    logger.info("Trace analyzer could not drain queue");
                }
                active.set(false);
            }

        }

        private void drain() {
            while (!queue.isEmpty()) {
                TraceRecord record = queue.poll();

                if (record != null) {
                    String message = record.toString();
                    logger.info(message);
                }
            }
        }
    }

    private class TraceFeeder implements Trace {

        private final SelectableChannel channel;
        private final long sequence;

        public TraceFeeder(SelectableChannel channel, long sequence) {
            this.sequence = sequence;
            this.channel = channel;
        }

        @Override
        public void trace(Object event) {
            trace(event, null);
        }

        @Override
        public void trace(Object event, Object value) {
            if (active.get()) {
                TraceRecord record = new TraceRecord(channel, event, value, sequence);
                consumer.consume(record);
            }
        }

    }

    private class TraceRecord {

        private final SelectableChannel channel;
        private final String thread;
        private final Object event;
        private final Object value;
        private final long sequence;

        public TraceRecord(SelectableChannel channel, Object event, Object value, long sequence) {
            this.thread = Thread.currentThread().getName();
            this.sequence = sequence;
            this.channel = channel;
            this.event = event;
            this.value = value;
        }

        public String toString() {
            StringWriter builder = new StringWriter();
            PrintWriter writer = new PrintWriter(builder);

            writer.print(sequence);
            writer.print(" ");
            writer.print(channel);
            writer.print(" (");
            writer.print(thread);
            writer.print("): ");
            writer.print(event);

            if (value != null) {
                if (value instanceof Throwable) {
                    writer.print(" -> ");
                    ((Throwable) value).printStackTrace(writer);
                } else {
                    writer.print(" -> ");
                    writer.print(value);
                }
            }
            writer.close();
            return builder.toString();
        }
    }
}
