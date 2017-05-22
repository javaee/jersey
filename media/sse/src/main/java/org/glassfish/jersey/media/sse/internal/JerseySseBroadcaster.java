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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.glassfish.jersey.internal.jsr166.Flow;
import org.glassfish.jersey.internal.util.JerseyPublisher;
import org.glassfish.jersey.media.sse.LocalizationMessages;

/**
 * Used for broadcasting SSE to multiple {@link javax.ws.rs.sse.SseEventSink} instances.
 * <p>
 * JAX-RS 2.1 {@link SseBroadcaster} implementation.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
class JerseySseBroadcaster extends JerseyPublisher<OutboundSseEvent> implements SseBroadcaster {

    /**
     * Callbacks notified when {@code SseBroadcaster} is being closed.
     */
    private final CopyOnWriteArrayList<Consumer<SseEventSink>> onCloseListeners;

    /**
     * Callbacks notified when error occurs.
     */
    private final CopyOnWriteArrayList<BiConsumer<SseEventSink, Throwable>> onExceptionListeners;

    /**
     * Package-private constructor.
     * <p>
     * The broadcaster instance should be obtained by calling {@link Sse#newBroadcaster()}, not directly.
     */
    JerseySseBroadcaster() {
        onExceptionListeners = new CopyOnWriteArrayList<>();
        onCloseListeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Package-private constructor.
     * <p>
     * The broadcaster instance should be obtained by calling {@link Sse#newBroadcaster()}, not directly.
     *
     * @param executorService {@code ExecutorService} the executor to use for async delivery,
     *                        supporting creation of at least one independent thread
     */
    JerseySseBroadcaster(final ExecutorService executorService) {
        super(executorService);
        onExceptionListeners = new CopyOnWriteArrayList<>();
        onCloseListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void register(SseEventSink sseEventSink) {
        super.subscribe(new SseEventSinkWrapper(sseEventSink));
    }

    @Override
    public void onError(BiConsumer<SseEventSink, Throwable> onError) {
        if (onError == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("onError"));
        }

        onExceptionListeners.add(onError);
    }

    @Override
    public void onClose(Consumer<SseEventSink> onClose) {
        if (onClose == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("onClose"));
        }

        onCloseListeners.add(onClose);
    }

    @Override
    public CompletionStage<?> broadcast(final OutboundSseEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("event"));
        }
        publish(event);

        // TODO JAX-RS 2.1
        return null;
    }

    private void notifyOnCompleteHandlers(Flow.Subscriber<? super OutboundSseEvent> subscriber) {
        if (subscriber instanceof SseEventSinkWrapper) {
            onCloseListeners.forEach((listener) -> listener.accept(((SseEventSinkWrapper) subscriber).sseEventSink));
        }
    }

    private void notifyOnErrorCallbacks(final Flow.Subscriber<? super OutboundSseEvent> subscriber, final Throwable throwable) {
        if (subscriber instanceof SseEventSinkWrapper) {
            onExceptionListeners.forEach(
                    (listener) -> listener.accept(((SseEventSinkWrapper) subscriber).sseEventSink, throwable));
        }
    }

    private class SseEventSinkWrapper implements Flow.Subscriber<OutboundSseEvent> {

        private final SseEventSink sseEventSink;

        SseEventSinkWrapper(SseEventSink sseEventSink) {
            this.sseEventSink = sseEventSink;
        }

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            // TODO JAX-RS 2.1
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(final OutboundSseEvent item) {
            sseEventSink.send(item);
        }

        @Override
        public void onError(final Throwable throwable) {
            // TODO JAX-RS 2.1
            sseEventSink.close();
            notifyOnErrorCallbacks(this, throwable);
        }

        @Override
        public void onComplete() {
            // TODO JAX-RS 2.1
            sseEventSink.close();
            notifyOnCompleteHandlers(this);

        }
    }
}
