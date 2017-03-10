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

package org.glassfish.jersey.internal.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import org.glassfish.jersey.internal.jsr166.Flow;
import org.glassfish.jersey.internal.jsr166.SubmissionPublisher;


/**
 * Implementation of {@link Flow.Publisher} corresponding to reactive streams specification.
 * <p>
 * Delegates to {@link SubmissionPublisher} repackaged from jsr166.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class JerseyPublisher<T> implements javax.ws.rs.Flow.Publisher<T> {

    private static final int DEFAULT_BUFFER_CAPACITY = 256;
    private SubmissionPublisher<T> submissionPublisher = new SubmissionPublisher<>();

    /**
     * Creates a new JerseyPublisher using the {@link ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two, in which case, a new Thread is created to run
     * each task), with maximum buffer capacity of {@value DEFAULT_BUFFER_CAPACITY}.
     */
    public JerseyPublisher() {
        this(ForkJoinPool.commonPool(), DEFAULT_BUFFER_CAPACITY);
    }

    /**
     * Creates a new JerseyPublisher using the {@link ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two, in which case, a new Thread is created to run
     * each task), with specified maximum buffer capacity.
     *
     * @param maxBufferCapacity the maximum capacity for each
     *                          subscriber's buffer (the enforced capacity may be rounded up to
     *                          the nearest power of two and/or bounded by the largest value
     *                          supported by this implementation; method {@link #getMaxBufferCapacity}
     *                          returns the actual value)
     */
    public JerseyPublisher(final int maxBufferCapacity) {
        this(ForkJoinPool.commonPool(), maxBufferCapacity);
    }

    /**
     * Creates a new JerseyPublisher using the given Executor for async delivery to subscribers, with the given
     * maximum buffer size for each subscriber.
     *
     * @param executorService   {@code ExecutorService} the executor to use for async delivery,
     *                          supporting creation of at least one independent thread
     * @param maxBufferCapacity the maximum capacity for each
     *                          subscriber's buffer (the enforced capacity may be rounded up to
     *                          the nearest power of two and/or bounded by the largest value
     *                          supported by this implementation; method {@link #getMaxBufferCapacity}
     *                          returns the actual value)
     *
     * @throws NullPointerException if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not positive
     */
    public JerseyPublisher(final ExecutorService executorService, final int maxBufferCapacity) {
        submissionPublisher = new SubmissionPublisher<>(executorService::execute, maxBufferCapacity);
    }

    @Override
    public void subscribe(final javax.ws.rs.Flow.Subscriber<? super T> subscriber) {
        submissionPublisher.subscribe(new Flow.Subscriber<T>() {

            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscriber.onSubscribe(new javax.ws.rs.Flow.Subscription() {

                    @Override
                    public void request(final long n) {
                        subscription.request(n);
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                    }
                });
            }

            @Override
            public void onNext(final T item) {
                subscriber.onNext(item);
            }

            @Override
            public void onError(final Throwable throwable) {
                subscriber.onError(throwable);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
    }

    /**
     * Publishes the given item to each current subscriber by asynchronously invoking its onNext method.
     * <p>
     * Blocks uninterruptibly while resources for any subscriber are unavailable.
     *
     * @param data published data
     * @return the estimated maximum lag among subscribers
     * @throws IllegalStateException if closed
     * @throws NullPointerException if data is null
     * @throws java.util.concurrent.RejectedExecutionException if thrown by Executor
     */
    public int submit(final T data) {
        return submissionPublisher.submit(data);
    }

    /**
     * Processes all published items using the given Consumer function. Returns a CompletableFuture that is completed
     * normally when this publisher signals {@code onComplete()}, or completed exceptionally upon any error, or an
     * exception is thrown by the Consumer, or the returned CompletableFuture is cancelled, in which case no further
     * items are processed.
     *
     * @param consumer function to process all published data
     * @return a {@link CompletableFuture} that is completed normally when the publisher signals onComplete,
     * and exceptionally upon any error or cancellation
     * @throws NullPointerException if consumer is null
     */
    public CompletableFuture<Void> consume(final Consumer<? super T> consumer) {
        return submissionPublisher.consume(consumer);
    }

    /**
     * Unless already closed, issues {@code onComplete()} signals to current subscribers, and disallows subsequent
     * attempts to publish. Upon return, this method does <em>NOT</em> guarantee that all subscribers have yet
     * completed.
     */
    public void close() {
        submissionPublisher.close();
    }

    /**
     * Issues onError signals to current subscribers with the given error, and disallows subsequent attempts to publish.
     *
     * @param error the {@code onError} argument sent to subscribers
     * @throws NullPointerException if error is null
     */
    public void closeExceptionally(final Throwable error) {
        submissionPublisher.closeExceptionally(error);
    }

    /**
     * Returns an estimate of the maximum number of items produced but not yet consumed among all current subscribers.
     *
     * @return estimated maximum lag
     */
    public int estimateMaximumLag() {
        return submissionPublisher.estimateMaximumLag();
    }

    /**
     * Returns an estimate of the minimum number of items requested but not yet produced, among all current subscribers.
     *
     * @return estimated minimum demand
     */
    public long estimateMinimumDemand() {
        return submissionPublisher.estimateMinimumDemand();
    }

    /**
     * Returns the exception associated with {@link #closeExceptionally}, or null if not closed or if closed normally.
     *
     * @return exception thrown on closing or {@code null}
     */
    public Throwable getClosedException() {
        return submissionPublisher.getClosedException();
    }

    /**
     * Returns the maximum per-subscriber buffer capacity.
     *
     * @return the maximum per-subscriber buffer capacity
     */
    public int getMaxBufferCapacity() {
        return submissionPublisher.getMaxBufferCapacity();
    }
}
