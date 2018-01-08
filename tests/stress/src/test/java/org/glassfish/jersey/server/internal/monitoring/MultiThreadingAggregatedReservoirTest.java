/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Multi Threading concurrency test of Jersey monitoring internals.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class MultiThreadingAggregatedReservoirTest {

    private static final Logger LOGGER = Logger.getLogger(MultiThreadingAggregatedReservoirTest.class.getName());

    private static final int PRODUCER_COUNT = 5;
    private static final int CONSUMER_COUNT = 5;

    /**
     * Note that more than 5 seconds may require more than 1G heap memory.
     */
    private static final int TEST_DURATION_MILLIS = 10_000;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 120;
    private static final double DELTA = 0.0001;

    private final AtomicInteger incrementer = new AtomicInteger(0);

    private final ExecutorService producerExecutorService = Executors
            .newFixedThreadPool(PRODUCER_COUNT, new ThreadFactoryBuilder().setDaemon(true).build());

    private final ExecutorService consumerExecutorService = Executors
            .newFixedThreadPool(CONSUMER_COUNT, new ThreadFactoryBuilder().setDaemon(true).build());

    private final long startTime = System.nanoTime();
    private final TimeUnit startUnitTime = TimeUnit.NANOSECONDS;

    private final AggregatingTrimmer trimmer =
            new AggregatingTrimmer(startTime(), startUnitTime, 10, TimeUnit.MICROSECONDS);
    private final SlidingWindowTimeReservoir time10usReservoir =
            new SlidingWindowTimeReservoir(10, TimeUnit.MICROSECONDS,
                                           startTime(), startUnitTime, trimmer);
    private final AggregatedSlidingWindowTimeReservoir time1DayAggregatedReservoir =
            new AggregatedSlidingWindowTimeReservoir(1,
                                                     TimeUnit.DAYS,
                                                     startTime(), startUnitTime, trimmer);
    private final AggregatedSlidingWindowTimeReservoir time10DaysAggregatedReservoir =
            new AggregatedSlidingWindowTimeReservoir(
                    10, TimeUnit.DAYS,
                    startTime(), startUnitTime, trimmer);
    private final List<AggregatedSlidingWindowTimeReservoir> aggregatedTimeReservoirs =
            new CopyOnWriteArrayList<>(
                    Arrays.asList(
                            new AggregatedSlidingWindowTimeReservoir(1, TimeUnit.SECONDS, startTime(),
                                                                     startUnitTime, trimmer),
                            time1DayAggregatedReservoir,
                            time10DaysAggregatedReservoir
                    ));

    /**
     * Determines the start time of the test.
     *
     * @return The start time of the test. Must be a constant value.
     */
    protected long startTime() {
        return startTime;
    }

    private volatile boolean doShutdown = false;

    /**
     * Runs {@link #PRODUCER_COUNT} producers that update {@link #time10usReservoir} 10 microseconds sliding window reservoir with
     * sequentially increasing values generated by {@link @incrementer}. This sliding window updates 1 day aggregated sliding
     * window and also 10 days aggregated sliding window ({@link #time1DayAggregatedReservoir} and {@link
     * #time10DaysAggregatedReservoir} respectively). In the meantime, {@link #CONSUMER_COUNT} consumers retrieve snapshots from
     * the aggregated window in order to increase the level of concurrency.
     *
     * @throws InterruptedException If any of the thread was interrupted and the test result won't be reliable
     */
    @Test
    public void parallelProducersAndConsumersTestingAggregatedSlidingWindows() throws InterruptedException {

        executeInParallel(consumerExecutorService, CONSUMER_COUNT, new Runnable() {
            @Override
            public void run() {
                try {
                    LOGGER.info("Consumer starting.");
                    while (!doShutdown && !Thread.currentThread().isInterrupted()) {

                        aggregatedTimeReservoirs.get(ThreadLocalRandom.current().nextInt(aggregatedTimeReservoirs.size()))
                                                .getSnapshot(System.nanoTime(), TimeUnit.NANOSECONDS);
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    LOGGER.info("Consumer terminating.");
                }
            }
        });

        executeInParallel(producerExecutorService, PRODUCER_COUNT, new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Producer starting.");
                while (!doShutdown) {
                    final int value = incrementer.incrementAndGet();
                    time10usReservoir.update((long) value, System.nanoTime(), TimeUnit.NANOSECONDS);
                }
                LOGGER.info("Producer terminating.");
            }
        });

        Thread.sleep(TEST_DURATION_MILLIS);
        LOGGER.info("Shutting down...");

        doShutdown = true;
        producerExecutorService.shutdown();
        consumerExecutorService.shutdown();
        Assert.assertTrue("Consumer tasks didn't terminated peacefully, aborting this test.",
                          consumerExecutorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertTrue("Producer tasks didn't terminated peacefully, aborting this test.",
                          producerExecutorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        final long snapshotTime = System.nanoTime();
        final long sum = (long) incrementer.get() * (incrementer.get() + 1) / 2;

        LOGGER.info("Integer reached: " + incrementer.get());

        checkInNanos(time1DayAggregatedReservoir, snapshotTime, incrementer.get(), 1, incrementer.get(),
                     (double) sum / incrementer.get(), snapshotTime - startTime());
        checkInNanos(time10DaysAggregatedReservoir, snapshotTime, incrementer.get(), 1, incrementer.get(),
                     (double) sum / incrementer.get(), snapshotTime - startTime());
    }

    private void executeInParallel(final Executor consumerExecutorService, final int count, final Runnable runnable) {
        for (int i = 0; i < count; ++i) {
            consumerExecutorService.execute(runnable);
        }
    }

    /**
     * Shutdown the producer executor service.
     */
    @After
    public void shutdownProducers() {
        producerExecutorService.shutdownNow();
    }

    /**
     * Shutdown the consumer executor service.
     */
    @After
    public void shutdownConsumers() {
        consumerExecutorService.shutdownNow();
    }

    /**
     * Checks whether the snapshot of given reservoir exhibits with expected measurements.
     *
     * @param reservoir        The reservoir to assert.
     * @param snapshotTime     The time for which to get the snapshot
     * @param expectedSize     Expected size of the snapshot
     * @param expectedMin      Expected minimum
     * @param expectedMax      Expected maximum
     * @param expectedMean     Expected mean
     * @param expectedInterval Expected interval
     */
    private static void checkInNanos(final TimeReservoir reservoir,
                                     final long snapshotTime,
                                     final long expectedSize,
                                     final long expectedMin,
                                     final long expectedMax,
                                     final double expectedMean, final long expectedInterval) {
        final UniformTimeSnapshot snapshot = reservoir.getSnapshot(snapshotTime, TimeUnit.NANOSECONDS);

        assertEquals("Total count does not match!", expectedSize, snapshot.size());
        assertEquals("Min exec time does not match!", expectedMin, snapshot.getMin());
        assertEquals("Max exec time does not match!", expectedMax, snapshot.getMax());
        assertEquals("Average exec time does not match!", expectedMean, snapshot.getMean(), DELTA);
        assertEquals("Expected interval does not match!", expectedInterval, snapshot.getTimeInterval(TimeUnit.NANOSECONDS));
    }
}
