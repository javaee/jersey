/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.media.sse;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ChunkedInput;

/**
 * Client for reading and processing {@link InboundEvent incoming Server-Sent Events}.
 * <p>
 * Once an {@link EventSource} is created, it {@link #open opens a connection} to the associated {@link WebTarget web target}
 * and starts processing any incoming inbound events.
 * </p>
 * <p>
 * Whenever a new event is received, an {@link EventSource#onEvent(InboundEvent)} method is called as well as any
 * registered {@link EventListener event listeners} are notified (see {@link EventSource#register(EventListener)}
 * and {@link EventSource#register(EventListener, String, String...)}.
 * </p>
 * <p>
 * Instances of this class are thread safe.
 * </p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class EventSource implements EventListener {
    private static final Logger LOGGER = Logger.getLogger(EventSource.class.getName());

    // Keeping the target is necessary for future reconnect support.
    private final WebTarget target;

    private ExecutorService executorService;
    private Future<?> process;
    private final Object connectionLock = new Object();

    private final EventListener listenerAggregator = new EventListener() {
        /**
         * Called by the event source when an inbound event is received.
         *
         * This listener aggregator method is responsible for invoking {@link EventSource#onEvent(InboundEvent)}
         * method on the owning event source as well as for notifying all registered {@link EventListener event listeners}.
         *
         * @param inboundEvent incoming {@link InboundEvent inbound event}.
         */
        @Override
        public void onEvent(final InboundEvent inboundEvent) {
            EventSource.this.onEvent(inboundEvent);

            EventSource.notify(inboundEvent, unboundListeners);

            final String eventName = inboundEvent.getName();
            if (eventName != null) {
                final List<EventListener> eventListeners = boundListeners.get(eventName);
                if (eventListeners != null) {
                    EventSource.notify(inboundEvent, eventListeners);
                }
            }
        }
    };

    /**
     * List of all listeners not bound to receive only events of a particular name.
     */
    private final List<EventListener> unboundListeners =
            new CopyOnWriteArrayList<EventListener>();

    /**
     * A map of listeners bound to receive only events of a particular name.
     */
    private final ConcurrentMap<String, List<EventListener>> boundListeners =
            new ConcurrentHashMap<String, List<EventListener>>();

    /**
     * Create new SSE event source and open a connection it to the supplied SSE streaming {@link WebTarget web target}.
     * <p>
     * The created event source instance automatically {@link #open opens a connection} to the supplied SSE streaming
     * web target and starts processing incoming {@link InboundEvent events}.
     * </p>
     * <p>
     * The incoming events are processed in an asynchronous task running in an internal
     * {@link java.util.concurrent.Executors#newSingleThreadExecutor() single thread executor}.
     * </p>
     *
     * @param target SSE streaming web target. Must not be {@code null}.
     * @throws NullPointerException in case the supplied web target is {@code null}.
     */
    public EventSource(WebTarget target) throws NullPointerException {
        this(target, true);

    }

    /**
     * Create new SSE event source pointing at a SSE streaming {@link WebTarget web target}.
     * <p>
     * If the supplied {@code open} flag is {@code true}, the created event source instance automatically
     * {@link #open opens a connection} to the supplied SSE streaming web target and starts processing incoming
     * {@link InboundEvent events}.
     * Otherwise, if the {@code open} flag is set to {@code false}, the created event source instance
     * is not automatically connected to the web target. In this case it is expected that the user who
     * created the event source will manually invoke its {@link #open()} method.
     * </p>
     * <p>
     * The incoming events are processed in an asynchronous task running in an internal
     * {@link java.util.concurrent.Executors#newSingleThreadExecutor() single thread executor}.
     * </p>
     *
     * @param target SSE streaming web target. Must not be {@code null}.
     * @throws NullPointerException in case the supplied web target is {@code null}.
     */
    public EventSource(WebTarget target, boolean open) {
        if (target == null) {
            throw new NullPointerException("Web target is 'null'.");
        }

        this.target = SseFeature.register(target);

        if (open) {
            open();
        }
    }

    /**
     * Open the connection to the supplied SSE underlying {@link WebTarget web target} and start processing incoming
     * {@link InboundEvent events}.
     *
     * @throws IllegalStateException in case the event source has already been opened earlier.
     */
    public void open() throws IllegalStateException {
        synchronized (connectionLock) {
            if (process != null) {
                throw new IllegalStateException(LocalizationMessages.EVENT_SOURCE_ALREADY_CONNECTED());
            }

            executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("jersey-sse-event-source-[%s]", target.getUri().toASCIIString()));
                }
            });


            final EventInput eventInput = target.request(SseFeature.SERVER_SENT_EVENTS_TYPE).get(EventInput.class);
            eventInput.setParser(ChunkedInput.createParser("\n\n"));

            final Future<?> p = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (!eventInput.isClosed()) {
                        synchronized (listenerAggregator) {
                            final InboundEvent event = eventInput.read();
                            if (event != null) {
                                listenerAggregator.onEvent(event);
                            }
                        }
                    }
                }
            });

            process = new Future<Object>() {
                public boolean cancel(boolean mayInterruptIfRunning) {
                    eventInput.close();
                    return p.cancel(mayInterruptIfRunning);
                }

                public boolean isCancelled() {
                    return p.isCancelled();
                }

                public boolean isDone() {
                    return p.isDone();
                }

                public Object get() throws InterruptedException, ExecutionException {
                    return p.get();
                }

                public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return p.get(timeout, unit);
                }
            };
        }
    }

    /**
     * Check if this event source instance is open.
     *
     * @return {@code true} if this event source is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        synchronized (connectionLock) {
            return process != null;
        }
    }

    /**
     * Register new {@link EventListener event listener} to receive all streamed {@link InboundEvent SSE events}.
     *
     * @param listener event listener to be registered with the event source.
     * @see #register(EventListener, String, String...)
     */
    public void register(EventListener listener) {
        register(listener, null);
    }

    /**
     * Add name-bound {@link EventListener event listener} which will be called only for incoming SSE
     * {@link InboundEvent events} whose {@link InboundEvent#getName() name} is equal to the specified
     * name(s).
     *
     * @param listener   event listener to register with this event source.
     * @param eventName  inbound event name.
     * @param eventNames additional event names.
     * @see #register(EventListener)
     */
    public void register(EventListener listener, String eventName, String... eventNames) {
        if (eventName == null) {
            unboundListeners.add(listener);
        } else {
            addBoundListener(eventName, listener);

            if (eventNames != null) {
                for (String name : eventNames) {
                    addBoundListener(name, listener);
                }
            }
        }
    }

    private void addBoundListener(String name, EventListener listener) {
        List<EventListener> listeners = boundListeners.get(name);
        if (listeners == null) {
            listeners = boundListeners.putIfAbsent(name, new CopyOnWriteArrayList<EventListener>());
        }
        listeners.add(listener);
    }

    private static void notify(InboundEvent inboundEvent, Collection<EventListener> listeners) {
        for (EventListener listener : listeners) {
            listener.onEvent(inboundEvent);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The default {@code EventSource} implementation is empty, users can override this method to handle
     * incoming {@link InboundEvent}s.
     * </p>
     * <p>
     * Note that overriding this method may be necessary to make sure no {@code InboundEvent incoming events}
     * are lost in case the event source is constructed using {@link #EventSource(javax.ws.rs.client.WebTarget)}
     * constructor or in case a {@code true} flag is passed to the {@link #EventSource(javax.ws.rs.client.WebTarget, boolean)}
     * constructor, since the connection is opened as as part of the constructor call and the event processing starts
     * immediately. Therefore any {@link EventListener}s registered later after the event source has been constructed
     * may miss the notifications about the one or more events that arrive immediately after the connection to the
     * event source is established.
     * </p>
     *
     * @param inboundEvent received inbound event.
     */
    @Override
    public void onEvent(InboundEvent inboundEvent) {
        // do nothing
    }

    /**
     * Close this event source.
     *
     * The method will wait up to 5 seconds for the internal event processing task to complete.
     */
    public void close() {
        close(5, TimeUnit.SECONDS);
    }

    /**
     * Close this event source and wait for the internal event processing task to complete
     * for up to the specified amount of wait time.
     * <p>
     * The method blocks until the event processing task has completed execution after a shutdown
     * request, or until the timeout occurs, or the current thread is interrupted, whichever happens
     * first.
     * </p>
     * <p>
     * In case the waiting for the event processing task has been interrupted, this method restores
     * the {@link Thread#interrupted() interrupt} flag on the thread before returning {@code false}.
     * </p>
     *
     * @param timeout the maximum time to wait.
     * @param unit    the time unit of the timeout argument.
     * @return {@code true} if this executor terminated and {@code false} if the timeout elapsed
     *         before termination or the termination was interrupted.
     */
    public boolean close(long timeout, TimeUnit unit) {
        synchronized (connectionLock) {
            if (process == null) {
                return true;
            }

            process.cancel(true);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(timeout, unit)) {
                    LOGGER.warning(LocalizationMessages.EVENT_SOURCE_SHUTDOWN_TIMEOUT(target.getUri().toString()));
                    return false;
                }
            } catch (InterruptedException e) {
                LOGGER.fine(LocalizationMessages.EVENT_SOURCE_SHUTDOWN_INTERRUPTED(target.getUri().toString()));
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return true;
    }
}
