/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.internal.LocalizationMessages;

/**
 * Used for broadcasting response chunks to multiple {@link ChunkedOutput} instances.
 *
 * @param <T> broadcast type.
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Martin Matula
 */
public class Broadcaster<T> implements BroadcasterListener<T> {
    // We do not expect large amounts of broadcaster listeners additions/removals, but large amounts of traversals.
    private final CopyOnWriteArrayList<BroadcasterListener<T>> listeners =
            new CopyOnWriteArrayList<BroadcasterListener<T>>();

    private final ConcurrentLinkedQueue<ChunkedOutput<T>> chunkedOutputs =
            new ConcurrentLinkedQueue<ChunkedOutput<T>>();

    /**
     * Creates a new instance.
     * If this constructor is called by a subclass, it assumes the the reason for the subclass to exist is to implement
     * {@link #onClose(ChunkedOutput)} and {@link #onException(ChunkedOutput, Exception)} methods, so it adds
     * the newly created instance as the listener. To avoid this, subclasses may call {@link #Broadcaster(Class)}
     * passing their class as an argument.
     */
    public Broadcaster() {
        this(Broadcaster.class);
    }

    /**
     * Can be used by subclasses to override the default functionality of adding self to the set of
     * {@link BroadcasterListener listeners}. If creating a direct instance of a subclass passed in the parameter,
     * the broadcaster will not register itself as a listener.
     *
     * @param subclass subclass of Broadcaster that should not be registered as a listener - if creating a direct instance
     *                 of this subclass, this constructor will not register the new instance as a listener.
     * @see #Broadcaster()
     */
    protected Broadcaster(final Class<? extends Broadcaster> subclass) {
        if (subclass != getClass()) {
            listeners.add(this);
        }
    }

    /**
     * Register {@link ChunkedOutput} to this {@code Broadcaster} instance.
     *
     * @param chunkedOutput {@link ChunkedOutput} to register.
     * @return {@code true} if the instance was successfully registered, {@code false} otherwise.
     */
    public <OUT extends ChunkedOutput<T>> boolean add(final OUT chunkedOutput) {
        return chunkedOutputs.offer(chunkedOutput);
    }

    /**
     * Un-register {@link ChunkedOutput} from this {@code Broadcaster} instance.
     *
     * This method does not close the {@link ChunkedOutput} being unregistered.
     *
     * @param chunkedOutput {@link ChunkedOutput} instance to un-register from this broadcaster.
     * @return {@code true} if the instance was unregistered, {@code false} otherwise.
     */
    public <OUT extends ChunkedOutput<T>> boolean remove(final OUT chunkedOutput) {
        return chunkedOutputs.remove(chunkedOutput);
    }

    /**
     * Register {@link BroadcasterListener} for {@code Broadcaster} events listening.
     * <p>
     * This operation is potentially slow, especially if large number of listeners get registered in the broadcaster.
     * The {@code Broadcaster} implementation is optimized to efficiently handle small amounts of
     * concurrent listener registrations and removals and large amounts of registered listener notifications.
     * </p>
     *
     * @param listener listener to be registered.
     * @return {@code true} if registered, {@code false} otherwise.
     */
    public boolean add(final BroadcasterListener<T> listener) {
        return listeners.add(listener);
    }

    /**
     * Un-register {@link BroadcasterListener}.
     * <p>
     * This operation is potentially slow, especially if large number of listeners get registered in the broadcaster.
     * The {@code Broadcaster} implementation is optimized to efficiently handle small amounts of
     * concurrent listener registrations and removals and large amounts of registered listener notifications.
     * </p>
     *
     * @param listener listener to be unregistered.
     * @return {@code true} if unregistered, {@code false} otherwise.
     */
    public boolean remove(final BroadcasterListener<T> listener) {
        return listeners.remove(listener);
    }

    /**
     * Broadcast a chunk to all registered {@link ChunkedOutput} instances.
     *
     * @param chunk chunk to be sent.
     */
    public void broadcast(final T chunk) {
        forEachOutput(new Task<ChunkedOutput<T>>() {
            @Override
            public void run(final ChunkedOutput<T> cr) throws IOException {
                cr.write(chunk);
            }
        });
    }

    /**
     * Close all registered {@link ChunkedOutput} instances.
     */
    public void closeAll() {
        forEachOutput(new Task<ChunkedOutput<T>>() {
            @Override
            public void run(final ChunkedOutput<T> cr) throws IOException {
                cr.close();
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * Can be implemented by subclasses to handle the event of exception thrown from a particular {@link ChunkedOutput}
     * instance when trying to write to it or close it.
     *
     * @param chunkedOutput instance that threw exception.
     * @param exception     exception that was thrown.
     */
    @Override
    public void onException(final ChunkedOutput<T> chunkedOutput, final Exception exception) {
    }

    /**
     * {@inheritDoc}
     *
     * Can be implemented by subclasses to handle the event of {@link ChunkedOutput} being closed.
     *
     * @param chunkedOutput instance that was closed.
     */
    @Override
    public void onClose(final ChunkedOutput<T> chunkedOutput) {
    }

    private static interface Task<T> {
        void run(T parameter) throws IOException;
    }

    private void forEachOutput(final Task<ChunkedOutput<T>> t) {
        for (Iterator<ChunkedOutput<T>> iterator = chunkedOutputs.iterator(); iterator.hasNext(); ) {
            ChunkedOutput<T> chunkedOutput = iterator.next();
            if (!chunkedOutput.isClosed()) {
                try {
                    t.run(chunkedOutput);
                } catch (Exception e) {
                    fireOnException(chunkedOutput, e);
                }
            }
            if (chunkedOutput.isClosed()) {
                iterator.remove();
                fireOnClose(chunkedOutput);
            }
        }
    }

    private void forEachListener(final Task<BroadcasterListener<T>> t) {
        for (BroadcasterListener<T> listener : listeners) {
            try {
                t.run(listener);
            } catch (Exception e) {
                // log, but don't break
                Logger.getLogger(Broadcaster.class.getName()).log(Level.WARNING,
                        LocalizationMessages.BROADCASTER_LISTENER_EXCEPTION(e.getClass().getSimpleName()), e);
            }
        }
    }

    private void fireOnException(final ChunkedOutput<T> chunkedOutput, final Exception exception) {
        forEachListener(new Task<BroadcasterListener<T>>() {
            @Override
            public void run(BroadcasterListener<T> parameter) throws IOException {
                parameter.onException(chunkedOutput, exception);
            }
        });
    }

    private void fireOnClose(final ChunkedOutput<T> chunkedOutput) {
        forEachListener(new Task<BroadcasterListener<T>>() {
            @Override
            public void run(BroadcasterListener<T> parameter) throws IOException {
                parameter.onClose(chunkedOutput);
            }
        });
    }
}
