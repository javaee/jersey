/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.client.WebTarget;

/**
 * Client for reading and processing Server Sent {@link InboundEvent}s.
 *
 * <p>When {@link EventSource} is created, it makes GET request to given {@link URI} and waits for incoming {@link InboundEvent}s.
 * Whenever any event is received, {@link EventSource#onEvent(InboundEvent)} is called and listeners (if any) are notified (see
 * {@link EventSource#addEventListener(String, EventListener)} and {@link EventSource#addEventListener(String, EventListener)}.</p>
 *
 * <p>Instances of this class are thread safe.</p>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EventSource implements EventListener {

    private final WebTarget target;

    private final ConcurrentSkipListSet<EventListener> generalListeners = new ConcurrentSkipListSet<EventListener>(new Comparator<EventListener>() {
        @Override
        public int compare(EventListener eventListener, EventListener eventListener1) {
            return eventListener.hashCode() - eventListener1.hashCode();
        }
    });

    private final ConcurrentSkipListMap<String, List<EventListener>> namedListeners = new ConcurrentSkipListMap<String, List<EventListener>>();

    /**
     * Create new instance and start processing incoming {@link InboundEvent}s in newly created {@link ExecutorService}
     * ({@link Executors#newCachedThreadPool()}.
     *
     * @param target JAX-RS {@link WebTarget} instance which will be used to obtain {@link InboundEvent}s.
     */
    public EventSource(WebTarget target) {
        this.target = target;

        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
    }

    /**
     * Create new instance and start processing incoming {@link InboundEvent}s in provided {@link ExecutorService}.
     *
     * @param target JAX-RS {@link WebTarget} instance which will be used to obtain {@link InboundEvent}s.
     * @param executorService used for processing events.
     */
    public EventSource(WebTarget target, ExecutorService executorService) {
        this.target = target;

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
    }

    /**
     * Add {@link EventListener}.
     *
     * @param listener {@link EventListener} to add to current instance.
     */
    public void addEventListener(EventListener listener) {
        addEventListener(null, listener);
    }

    /**
     * Add {@link EventListener} which will be called only when {@link InboundEvent} with certain name is received.
     *
     * @param eventName {@link InboundEvent} name.
     * @param listener {@link EventListener} to add to current instance.
     */
    public void addEventListener(String eventName, EventListener listener) {
        if(eventName == null) {
            generalListeners.add(listener);
        } else {
            final List<EventListener> eventListeners = namedListeners.get(eventName);
            if(eventListeners == null) {
                namedListeners.put(eventName, Arrays.asList(listener));
            } else {
                eventListeners.add(listener);
            }
        }
    }

    private void process() {
        target.configuration().register(EventProcessorReader.class);
        final EventProcessor eventProcessor = target.request().get(EventProcessor.class);
        eventProcessor.process(this);
    }

    private void notifyListeners(InboundEvent inboundEvent, Collection<EventListener> listeners) {
        for(EventListener eventListener : listeners) {
            eventListener.onEvent(inboundEvent);
        }
    }

    /**
     * Called when inboundEvent is received. Is responsible for calling {@link EventSource#onEvent(InboundEvent)} and all registered
     * {@link EventListener} instances.
     *
     * @param inboundEvent incoming {@link InboundEvent}.
     */
    void onReceivedEvent(InboundEvent inboundEvent) {
        onEvent(inboundEvent);

        notifyListeners(inboundEvent, generalListeners);

        final String eventName = inboundEvent.getName();
        if(eventName != null) {
            final List<EventListener> eventListeners = namedListeners.get(eventName);
            if(eventListeners != null) {
                notifyListeners(inboundEvent, eventListeners);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Empty implementations, users can override this method to handle incoming {@link InboundEvent}s. Please note that this
     * is the ONLY way how to be absolutely sure that you won't miss any incoming {@link} InboundEvent. Initial request is made
     * right after {@link EventSource} is created and processing starts immediately. {@link EventListener}s registered
     * after {@link InboundEvent} is received won't be notified.
     *
     * @param inboundEvent received inboundEvent.
     */
    @Override
    public void onEvent(InboundEvent inboundEvent) {
        // do nothing
    }
}
