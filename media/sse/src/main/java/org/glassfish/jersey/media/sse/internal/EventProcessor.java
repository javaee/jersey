/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.sse.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEvent;

import org.glassfish.jersey.client.ClientExecutor;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.LocalizationMessages;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 * Private event processor task responsible for connecting to the SSE stream and processing
 * incoming SSE events as well as handling any connection issues.
 */
public class EventProcessor implements Runnable, EventListener {

    private static final Level CONNECTION_ERROR_LEVEL = Level.FINE;
    private static final ExtendedLogger LOGGER =
            new ExtendedLogger(Logger.getLogger(EventProcessor.class.getName()), Level.FINEST);

    /**
     * Open connection response arrival synchronization latch.
     */
    private final CountDownLatch firstContactSignal;
    /**
     * Last received event id.
     */
    private String lastEventId;
    /**
     * Re-connect delay.
     */
    private long reconnectDelay;
    /**
     * SSE streaming resource target.
     */
    private final WebTarget target;
    /**
     * Flag indicating if the persistent HTTP connections should be disabled.
     */
    private final boolean disableKeepAlive;
    /**
     * Incoming SSE event processing task executor.
     */
    private final ClientExecutor executor;
    /**
     * Event source internal state.
     */
    private final AtomicReference<State> state;
    /**
     * List of all listeners not bound to receive only events of a particular name.
     */
    private final List<EventListener> unboundListeners;
    /**
     * A map of listeners bound to receive only events of a particular name.
     */
    private final Map<String, List<EventListener>> boundListeners;

    /**
     * Shutdown handler is invoked when Event processor reaches terminal stage.
     */
    private final ShutdownHandler shutdownHandler;

    /**
     * Invoked whenever an event is received.
     */
    private final EventListener eventListener;

    private EventProcessor(final EventProcessor that) {
        this.firstContactSignal = null;

        this.reconnectDelay = that.reconnectDelay;
        this.lastEventId = that.lastEventId;
        this.target = that.target;
        this.disableKeepAlive = that.disableKeepAlive;
        this.executor = that.executor;
        this.state = that.state;
        this.boundListeners = that.boundListeners;
        this.unboundListeners = that.unboundListeners;
        this.eventListener = that.eventListener;
        this.shutdownHandler = that.shutdownHandler;
    }

    private EventProcessor(Builder builder) {
        // Synchronization barrier used to signal that the initial contact with SSE endpoint
        // has been made.
        this.firstContactSignal = new CountDownLatch(1);

        this.reconnectDelay = builder.reconnectDelay;
        this.lastEventId = builder.lastEventId;
        this.target = builder.target;
        this.disableKeepAlive = builder.disableKeepAlive;
        this.executor = builder.clientExecutor;
        this.state = builder.state;
        this.boundListeners = builder.boundListeners == null ? Collections.EMPTY_MAP : builder.boundListeners;
        this.unboundListeners = builder.unboundListeners == null ? Collections.EMPTY_LIST : builder.unboundListeners;
        this.eventListener = builder.eventListener;
        this.shutdownHandler = builder.shutdownHandler;
    }

    /**
     * Create new Event processor builder.
     *
     * @param target web target to be used to call remote resource.
     * @param state state shared with the owner of event processor instance.
     * @param clientExecutor executor service used for consuming events and scheduling reconnects.
     * @param eventListener event listener.
     * @param shutdownHandler shutdown callback.
     * @return new {@link Builder} instance.
     */
    public static Builder builder(WebTarget target,
                                  AtomicReference<State> state,
                                  ClientExecutor clientExecutor,
                                  EventListener eventListener,
                                  ShutdownHandler shutdownHandler) {

        return new Builder(target, state, clientExecutor, eventListener, shutdownHandler);
    }

    @Override
    public void run() {
        LOGGER.debugLog("Listener task started.");

        EventInput eventInput = null;
        try {
            try {
                final Invocation.Builder request = prepareHandshakeRequest();
                if (state.get() == State.OPEN) { // attempt to connect only if even source is open
                    LOGGER.debugLog("Connecting...");
                    eventInput = request.get(EventInput.class);
                    LOGGER.debugLog("Connected!");
                }
            } finally {
                if (firstContactSignal != null) {
                    // release the signal regardless of event source state or connection request outcome
                    firstContactSignal.countDown();
                }
            }

            final Thread execThread = Thread.currentThread();

            while (state.get() == State.OPEN && !execThread.isInterrupted()) {
                if (eventInput == null || eventInput.isClosed()) {
                    LOGGER.debugLog("Connection lost - scheduling reconnect in {0} ms", reconnectDelay);
                    scheduleReconnect(reconnectDelay);
                    break;
                } else {
                    this.onEvent(eventInput.read());
                }
            }
        } catch (ServiceUnavailableException ex) {
            LOGGER.debugLog("Received HTTP 503");
            long delay = reconnectDelay;
            if (ex.hasRetryAfter()) {
                LOGGER.debugLog("Recovering from HTTP 503 using HTTP Retry-After header value as a reconnect delay");
                final Date requestTime = new Date();
                delay = ex.getRetryTime(requestTime).getTime() - requestTime.getTime();
                delay = (delay > 0) ? delay : 0;
            }

            LOGGER.debugLog("Recovering from HTTP 503 - scheduling to reconnect in {0} ms", delay);
            scheduleReconnect(delay);
        } catch (Exception ex) {
            if (LOGGER.isLoggable(CONNECTION_ERROR_LEVEL)) {
                LOGGER.log(CONNECTION_ERROR_LEVEL, String.format("Unable to connect - closing the event source to %s.",
                        target.getUri().toASCIIString()), ex);
            }
            // if we're here, an unrecoverable error has occurred - just turn off the lights...
            shutdownHandler.shutdown();
        } finally {
            if (eventInput != null && !eventInput.isClosed()) {
                eventInput.close();
            }
            LOGGER.debugLog("Listener task finished.");
        }
    }

    /**
     * Called by the event source when an inbound event is received.
     *
     * This listener aggregator method is responsible for invoking {@link EventSource#onEvent(InboundEvent)}
     * method on the owning event source as well as for notifying all registered {@link EventListener event listeners}.
     *
     * @param event incoming {@link InboundEvent inbound event}.
     */
    @Override
    public void onEvent(final InboundEvent event) {
        if (event == null) {
            return;
        }

        LOGGER.debugLog("New event received.");

        if (event.getId() != null) {
            lastEventId = event.getId();
        }
        if (event.isReconnectDelaySet()) {
            reconnectDelay = event.getReconnectDelay();
        }

        notify(eventListener, event);
        notify(unboundListeners, event);

        final String eventName = event.getName();
        if (eventName != null) {
            final List<EventListener> eventListeners = boundListeners.get(eventName);
            if (eventListeners != null) {
                notify(eventListeners, event);
            }
        }
    }

    private void notify(final Collection<EventListener> listeners, final InboundEvent event) {
        for (EventListener listener : listeners) {
            notify(listener, event);
        }
    }

    private void notify(final EventListener listener, final InboundEvent event) {
        try {
            listener.onEvent(event);
        } catch (Exception ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, String.format("Event notification in a listener of %s class failed.",
                        listener.getClass().getName()), ex);
            }
        }
    }

    /**
     * Schedule a new event processor task to reconnect after the specified {@code delay} [milliseconds].
     *
     * If the {@code delay} is zero or negative, the new reconnect task will be scheduled immediately.
     * The {@code reconnectDelay} and {@code lastEventId} field values are propagated into the newly
     * scheduled task.
     * <p>
     * The method will silently abort in case the event source is not {@link EventSource#isOpen() open}.
     * </p>
     *
     * @param delay specifies the amount of time [milliseconds] to wait before attempting a reconnect.
     *              If zero or negative, the new reconnect task will be scheduled immediately.
     */
    private void scheduleReconnect(final long delay) {
        final State s = state.get();
        if (s != State.OPEN) {
            LOGGER.debugLog("Aborting reconnect of event source in {0} state", state);
            return;
        }

        // propagate the current reconnectDelay, but schedule based on the delay parameter
        final EventProcessor processor = new EventProcessor(this);
        if (delay > 0) {
            executor.schedule(processor, delay, TimeUnit.MILLISECONDS);
        } else {
            executor.submit(processor);
        }
    }

    private Invocation.Builder prepareHandshakeRequest() {
        final Invocation.Builder request = target.request(SseFeature.SERVER_SENT_EVENTS_TYPE);
        if (lastEventId != null && !lastEventId.isEmpty()) {
            request.header(SseFeature.LAST_EVENT_ID_HEADER, lastEventId);
        }
        if (disableKeepAlive) {
            request.header("Connection", "close");
        }
        return request;
    }

    /**
     * Await the initial contact with the SSE endpoint.
     */
    public void awaitFirstContact() {
        LOGGER.debugLog("Awaiting first contact signal.");
        try {
            if (firstContactSignal == null) {
                return;
            }

            try {
                firstContactSignal.await();
            } catch (InterruptedException ex) {
                LOGGER.log(CONNECTION_ERROR_LEVEL, LocalizationMessages.EVENT_SOURCE_OPEN_CONNECTION_INTERRUPTED(), ex);
                Thread.currentThread().interrupt();
            }
        } finally {
            LOGGER.debugLog("First contact signal released.");
        }
    }

    /**
     * Event processor state, which is shared with the owner (to be able to control bootstrap and shutdown).
     */
    public enum State {
        /**
         * Ready to connect.
         */
        READY,

        /**
         * Connection established, events can be received.
         */
        OPEN,

        /**
         * Closed, won't receive any events.
         */
        CLOSED
    }

    /**
     * {@link EventProcessor} builder.
     */
    public static class Builder {

        private final WebTarget target;
        private final AtomicReference<State> state;
        private final ClientExecutor clientExecutor;
        private final EventListener eventListener;
        private final ShutdownHandler shutdownHandler;

        private long reconnectDelay;
        private TimeUnit reconnectUnit;
        private String lastEventId;
        private boolean disableKeepAlive;
        private List<EventListener> unboundListeners;
        private Map<String, List<EventListener>> boundListeners;

        private Builder(WebTarget target,
                        AtomicReference<State> state,
                        ClientExecutor clientExecutor,
                        EventListener eventListener,
                        ShutdownHandler shutdownHandler) {

            this.target = target;
            this.state = state;
            this.clientExecutor = clientExecutor;
            this.eventListener = eventListener;
            this.shutdownHandler = shutdownHandler;
        }

        /**
         * Set initial reconnect delay.
         *
         * Reconnect delay can be controlled by the server side, adding specific properties to incoming events.
         *
         * @param reconnectDelay reconnect delay value.
         * @param unit reconnect delay timeunit.
         * @return updated builder instance.
         */
        public Builder reconnectDelay(long reconnectDelay, TimeUnit unit) {
            this.reconnectDelay = reconnectDelay;
            this.reconnectUnit = reconnectUnit;
            return this;
        }

        /**
         * Unbounded listeners will get notified about any incoming event.
         *
         * @param unboundListeners list of listeners.
         * @return updated builder instance.
         */
        public Builder unboundListeners(List<EventListener> unboundListeners) {
            this.unboundListeners = unboundListeners;
            return this;
        }

        /**
         * Unbounded listeners will get notified about incoming events with particular name.
         *
         * @param boundListeners map of bound listeners, key is a name to which listeners are bound to, value is a list
         *                      of listeners.
         * @return updated builder instance.
         * @see SseEvent#getName()
         */
        public Builder boundListeners(Map<String, List<EventListener>> boundListeners) {
            this.boundListeners = boundListeners;
            return this;
        }

        /**
         * Disables keepalive.
         *
         * @return updated builder instance.
         */
        public Builder disableKeepAlive() {
            this.disableKeepAlive = true;
            return this;
        }

        /**
         * Build the {@link EventProcessor}.
         *
         * @return built Event processor instance.
         */
        public EventProcessor build() {
            return new EventProcessor(this);
        }
    }

    /**
     * Used to signal that the {@link EventProcessor} reached terminal stage.
     */
    public interface ShutdownHandler {

        /**
         * Invoked when the {@link EventProcessor} reaches terminal stage.
         *
         * All resources should be freed at this point.
         */
        void shutdown();
    }
}
