/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.spi;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

/**
 * Default implementation of the Jersey {@link org.glassfish.jersey.spi.ExecutorServiceProvider executor service provider SPI}.
 * <p>
 * This provider creates and provisions a shared {@link java.util.concurrent.ThreadPoolExecutor} instance
 * using the customizable values for :
 * <ul>
 * <li>{@link #getCorePoolSize() number of core pool threads}</li>
 * <li>{@link #getMaximumPoolSize() maximum number of threads in the pool}</li>
 * <li>{@link #getKeepAliveTime() thread keep-alive time (in seconds)}</li>
 * <li>{@link #getWorkQueue() backing thread pool work queue}</li>
 * <li>{@link #getBackingThreadFactory() backing thread factory}</li>
 * <li>{@link #getRejectedExecutionHandler() rejected task handler}</li>
 * </ul>
 * Subclasses may override the respective methods to customize the parameters of the provisioned thread pool executor.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.18
 */
public class ThreadPoolExecutorProvider extends AbstractThreadPoolProvider<ThreadPoolExecutor>
        implements ExecutorServiceProvider {

    private static final long CACHED_POOL_KEEP_ALIVE_DEFAULT_TIMEOUT = 60L;

    /**
     * Create a new instance of the thread pool executor provider.
     *
     * @param name provider name. The name will be used to name the threads created & used by the
     *             provisioned thread pool executor.
     */
    public ThreadPoolExecutorProvider(final String name) {
        super(name);
    }

    @Override
    public ExecutorService getExecutorService() {
        return super.getExecutor();
    }

    @Override
    protected final ThreadPoolExecutor createExecutor(
            final int corePoolSize, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
        return createExecutor(
                corePoolSize,
                getMaximumPoolSize(),
                getKeepAliveTime(),
                getWorkQueue(),
                threadFactory,
                handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial parameters.
     *
     * @param corePoolSize    the number of threads to keep in the thread pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the thread pool.
     * @param keepAliveTime   when the number of threads is greater than the core, this is the maximum time (in seconds)
     *                        that excess idle threads will wait for new tasks before terminating.
     * @param workQueue       the queue to use for holding tasks before they are executed.  This queue will hold only the
     *                        {@code Runnable} tasks submitted by the {@code execute} method.
     * @param threadFactory   the factory to use when the executor creates a new thread.
     * @param handler         the handler to use when execution is blocked because the thread bounds and queue capacities
     *                        are reached.
     * @return new configured thread pool instance.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *                                  {@code corePoolSize < 0}<br>
     *                                  {@code keepAliveTime < 0}<br>
     *                                  {@code maximumPoolSize <= 0}<br>
     *                                  {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException     if {@code workQueue} or {@code threadFactory} or {@code handler} is {@code null}.
     */
    protected ThreadPoolExecutor createExecutor(
            final int corePoolSize,
            final int maximumPoolSize,
            final long keepAliveTime,
            final BlockingQueue<Runnable> workQueue,
            final ThreadFactory threadFactory,
            final RejectedExecutionHandler handler) {

        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                handler);
    }

    /**
     * Get the maximum number of threads to allow in the thread pool.
     * <p>
     * The value from this method is passed as one of the input parameters in a call to the
     * {@link #createExecutor(int, int, long, java.util.concurrent.BlockingQueue, java.util.concurrent.ThreadFactory,
     * java.util.concurrent.RejectedExecutionHandler)} method.
     * </p>
     * <p>
     * The method can be overridden to customize the maximum number of threads allowed in the provisioned thread pool executor.
     * If not customized, the method defaults to {@link java.lang.Integer#MAX_VALUE}.
     * </p>
     *
     * @return maximum number of threads allowed in the thread pool.
     */
    protected int getMaximumPoolSize() {
        return Integer.MAX_VALUE;
    }

    /**
     * Get the thread keep-alive time (in seconds).
     * <p>
     * When the number of threads in the provisioned thread pool is greater than the core, this is the maximum time (in seconds)
     * that excess idle threads will wait for new tasks before terminating.
     * </p>
     * <p>
     * The value from this method is passed as one of the input parameters in a call to the
     * {@link #createExecutor(int, int, long, java.util.concurrent.BlockingQueue, java.util.concurrent.ThreadFactory,
     * java.util.concurrent.RejectedExecutionHandler)} method.
     * </p>
     * <p>
     * The method can be overridden to customize the thread keep-alive time in the provisioned thread pool executor.
     * If not customized, the method defaults to:
     * <ul>
     * <li>{@value #CACHED_POOL_KEEP_ALIVE_DEFAULT_TIMEOUT} in case the {@link #getMaximumPoolSize() maximum pool size}
     * is equal to {@link java.lang.Integer#MAX_VALUE}</li>
     * <li>{@code 0L} in case the maximum pool size is lower than
     * {@code java.lang.Integer#MAX_VALUE}</li>
     * </ul>
     * The default value computation closely corresponds to the thread pool executor configurations used in
     * {@link java.util.concurrent.Executors#newCachedThreadPool()} and
     * {@link java.util.concurrent.Executors#newFixedThreadPool(int)} methods.
     * </p>
     *
     * @return thread keep-alive time (in seconds) for the provisioned thread pool executor.
     */
    protected long getKeepAliveTime() {
        return CACHED_POOL_KEEP_ALIVE_DEFAULT_TIMEOUT;
    }

    /**
     * Get the work queue for the provisioned thread pool executor.
     * <p>
     * The work queue is used to hold the tasks before they are executed by the provisioned thread pool executor.
     * The queue will hold only the {@link Runnable} tasks submitted by the {@link ThreadPoolExecutor#execute} method.
     * </p>
     * <p>
     * The value from this method is passed as one of the input parameters in a call to the
     * {@link #createExecutor(int, int, long, java.util.concurrent.BlockingQueue, java.util.concurrent.ThreadFactory,
     * java.util.concurrent.RejectedExecutionHandler)} method.
     * </p>
     * <p>
     * The method can be overridden to customize the work queue used by the provisioned thread pool executor.
     * If not customized, the method defaults to:
     * <ul>
     * <li>{@link java.util.concurrent.SynchronousQueue} in case the {@link #getMaximumPoolSize() maximum pool size}
     * is equal to {@link java.lang.Integer#MAX_VALUE}</li>
     * <li>{@link java.util.concurrent.LinkedBlockingQueue} in case the maximum pool size is lower than
     * {@code java.lang.Integer#MAX_VALUE}</li>
     * </ul>
     * The default value computation closely corresponds to the thread pool executor configurations used in
     * {@link java.util.concurrent.Executors#newCachedThreadPool()} and
     * {@link java.util.concurrent.Executors#newFixedThreadPool(int)} methods.
     * </p>
     *
     * @return work queue for the provisioned thread pool executor.
     */
    protected BlockingQueue<Runnable> getWorkQueue() {
        return (getMaximumPoolSize() == Integer.MAX_VALUE)
                ? new SynchronousQueue<Runnable>() : new LinkedBlockingQueue<Runnable>();
    }

    @Override
    public void dispose(final ExecutorService executorService) {
        // NO-OP.
    }

    /**
     * Container pre-destroy handler method.
     * <p>
     * Invoking the method {@link #close() closes} this provider.
     * </p>
     */
    @PreDestroy
    public void preDestroy() {
        close();
    }
}
