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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.Flow;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

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
    private final CopyOnWriteArrayList<Consumer<Flow.Subscriber<? super OutboundSseEvent>>> onCloseListeners;

    /**
     * Callbacks notified when error occurs.
     */
    private final CopyOnWriteArrayList<BiConsumer<Flow.Subscriber<? super OutboundSseEvent>, Throwable>> onExceptionListeners;

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
    public void onError(final BiConsumer<Flow.Subscriber<? super OutboundSseEvent>, Throwable> onError) {
        if (onError == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("onError"));
        }
        onExceptionListeners.add(onError);
    }

    @Override
    public void onClose(final Consumer<Flow.Subscriber<? super OutboundSseEvent>> onClose) {
        if (onClose == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("onClose"));
        }
        onCloseListeners.add(onClose);
    }

    @Override
    public void broadcast(final OutboundSseEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("event"));
        }
        submit(event);
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super OutboundSseEvent> subscriber) {
        final Flow.Subscriber<OutboundSseEvent> wrapped = new Flow.Subscriber<OutboundSseEvent>() {

            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(final OutboundSseEvent item) {
                subscriber.onNext(item);
            }

            @Override
            public void onError(final Throwable throwable) {
                notifyOnErrorCallbacks(subscriber, throwable);
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                notifyOnCompleteHandlers(subscriber);
                subscriber.onComplete();
            }
        };
        super.subscribe(wrapped);
    }

    private void notifyOnCompleteHandlers(final Flow.Subscriber<? super OutboundSseEvent> subscriber) {
        onCloseListeners.forEach((listener) -> listener.accept(subscriber));
    }

    private void notifyOnErrorCallbacks(final Flow.Subscriber<? super OutboundSseEvent> subscriber, final Throwable throwable) {
        onExceptionListeners.forEach((listener) -> listener.accept(subscriber, throwable));
    }

}
