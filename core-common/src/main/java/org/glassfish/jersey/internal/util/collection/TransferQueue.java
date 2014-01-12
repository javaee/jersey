/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */
package org.glassfish.jersey.internal.util.collection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link BlockingQueue} in which producers may wait for consumers
 * to receive elements.  A {@code TransferQueue} may be useful for
 * example in message passing applications in which producers
 * sometimes (using method {@code transfer}) await receipt of
 * elements by consumers invoking {@code take} or {@code poll},
 * while at other times enqueue elements (via method {@code put})
 * without waiting for receipt. Non-blocking and time-out versions of
 * {@code tryTransfer} are also available.  A TransferQueue may also
 * be queried via {@code hasWaitingConsumer} whether there are any
 * threads waiting for items, which is a converse analogy to a
 * {@code peek} operation.
 *
 * <p>Like any {@code BlockingQueue}, a {@code TransferQueue} may be
 * capacity bounded. If so, an attempted {@code transfer} operation
 * may initially block waiting for available space, and/or
 * subsequently block waiting for reception by a consumer.  Note that
 * in a queue with zero capacity, such as {@link SynchronousQueue},
 * {@code put} and {@code transfer} are effectively synonymous.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements held in this collection
 * @author Doug Lea
 */
public interface TransferQueue<E> extends BlockingQueue<E> {
    /**
     * Transfers the specified element if there exists a consumer
     * already waiting to receive it, otherwise returning {@code false}
     * without enqueuing the element.
     *
     * @param e the element to transfer
     * @return {@code true} if the element was transferred, else
     *         {@code false}
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    boolean tryTransfer(E e);

    /**
     * Inserts the specified element into this queue, waiting if
     * necessary for space to become available and the element to be
     * dequeued by a consumer invoking {@code take} or {@code poll}.
     *
     * @param e the element to transfer
     * @throws InterruptedException     if interrupted while waiting,
     *                                  in which case the element is not enqueued.
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    void transfer(E e) throws InterruptedException;

    /**
     * Inserts the specified element into this queue, waiting up to
     * the specified wait time if necessary for space to become
     * available and the element to be dequeued by a consumer invoking
     * {@code take} or {@code poll}.
     *
     * @param e       the element to transfer
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before completion,
     *         in which case the element is not enqueued.
     * @throws InterruptedException     if interrupted while waiting,
     *                                  in which case the element is not enqueued.
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    boolean tryTransfer(E e, long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Returns {@code true} if there is at least one consumer waiting
     * to dequeue an element via {@code take} or {@code poll}.
     * The return value represents a momentary state of affairs.
     *
     * @return {@code true} if there is at least one waiting consumer
     */
    boolean hasWaitingConsumer();

    /**
     * Returns an estimate of the number of consumers waiting to
     * dequeue elements via {@code take} or {@code poll}. The return
     * value is an approximation of a momentary state of affairs, that
     * may be inaccurate if consumers have completed or given up
     * waiting. The value may be useful for monitoring and heuristics,
     * but not for synchronization control. Implementations of this
     * method are likely to be noticeably slower than those for
     * {@link #hasWaitingConsumer}.
     *
     * @return the number of consumers waiting to dequeue elements
     */
    int getWaitingConsumerCount();
}
