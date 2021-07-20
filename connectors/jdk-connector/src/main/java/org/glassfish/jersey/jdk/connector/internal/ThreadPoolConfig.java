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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.jdk.connector.internal;

import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Client thread pool configuration, which might be used to customize client thread pool.
 * <p/>
 * One can get a default <tt>ThreadPoolConfig</tt> using {@link ThreadPoolConfig#defaultConfig()}
 * and customize it according to the application specific requirements.
 * <p/>
 * A <tt>ThreadPoolConfig</tt> object might be customized in a "Builder"-like fashion:
 * <pre>
 *      ThreadPoolConfig.defaultConfig()
 *               .setPoolName("App1Pool")
 *               .setCorePoolSize(5)
 *               .setMaxPoolSize(10);
 * </pre>
 *
 * @author Oleksiy Stashok
 * @author gustav trede
 */
public final class ThreadPoolConfig {

    private static final int DEFAULT_CORE_POOL_SIZE = 1;
    private static final int DEFAULT_MAX_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 50);
    private static final int DEFAULT_MAX_QUEUE_SIZE = -1;
    private static final int DEFAULT_IDLE_THREAD_KEEP_ALIVE_TIMEOUT = 10;

    private static final ThreadPoolConfig DEFAULT = new ThreadPoolConfig(
            "jdk-connector", DEFAULT_CORE_POOL_SIZE,
            DEFAULT_MAX_POOL_SIZE,
            null, DEFAULT_MAX_QUEUE_SIZE,
            DEFAULT_IDLE_THREAD_KEEP_ALIVE_TIMEOUT,
            TimeUnit.SECONDS,
            null, Thread.NORM_PRIORITY, true, null);

    /**
     * Create new client thread pool configuration instance. The returned <tt>ThreadPoolConfig</tt> instance will be
     * pre-configured with a default values.
     *
     * @return client thread pool configuration instance.
     */
    public static ThreadPoolConfig defaultConfig() {
        return DEFAULT.copy();
    }

    private String poolName;
    private int corePoolSize;
    private int maxPoolSize;
    private Queue<Runnable> queue;
    private int queueLimit = -1;
    private long keepAliveTimeMillis;
    private ThreadFactory threadFactory;
    private int priority = Thread.MAX_PRIORITY;
    private boolean isDaemon;
    private ClassLoader initialClassLoader;

    private ThreadPoolConfig(String poolName,
                             int corePoolSize,
                             int maxPoolSize,
                             Queue<Runnable> queue,
                             int queueLimit,
                             long keepAliveTime,
                             TimeUnit timeUnit,
                             ThreadFactory threadFactory,
                             int priority,
                             boolean isDaemon,
                             ClassLoader initialClassLoader) {
        this.poolName = poolName;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queue = queue;
        this.queueLimit = queueLimit;
        if (keepAliveTime > 0) {
            this.keepAliveTimeMillis =
                    TimeUnit.MILLISECONDS.convert(keepAliveTime, timeUnit);
        } else {
            keepAliveTimeMillis = keepAliveTime;
        }

        this.threadFactory = threadFactory;
        this.priority = priority;
        this.isDaemon = isDaemon;
        this.initialClassLoader = initialClassLoader;
    }

    private ThreadPoolConfig(ThreadPoolConfig cfg) {
        this.queue = cfg.queue;
        this.threadFactory = cfg.threadFactory;
        this.poolName = cfg.poolName;
        this.priority = cfg.priority;
        this.isDaemon = cfg.isDaemon;
        this.maxPoolSize = cfg.maxPoolSize;
        this.queueLimit = cfg.queueLimit;
        this.corePoolSize = cfg.corePoolSize;
        this.keepAliveTimeMillis = cfg.keepAliveTimeMillis;
        this.initialClassLoader = cfg.initialClassLoader;
    }

    /**
     * Return a copy of this thread pool config.
     *
     * @return a copy of this thread pool config.
     */
    public ThreadPoolConfig copy() {
        return new ThreadPoolConfig(this);
    }

    /**
     * Return a queue that will be used to temporarily store tasks when all threads in the thread pool are busy.
     *
     * @return queue that will be used to temporarily store tasks when all threads in the thread pool are busy.
     */
    public Queue<Runnable> getQueue() {
        return queue;
    }

    /**
     * Set a queue implementation that will be used to temporarily store tasks when all threads in the thread pool are busy.
     *
     * @param queue queue implementation that will be used to temporarily store tasks when all threads in the thread pool are
     *              busy.
     * @return the {@link ThreadPoolConfig} with the new {@link Queue} implementation.
     */
    public ThreadPoolConfig setQueue(Queue<Runnable> queue) {
        this.queue = queue;
        return this;
    }

    /**
     * Return {@link ThreadFactory} that will be used to create thread pool threads.
     * <p/>
     * If {@link ThreadFactory} is set, then {@link #priority}, {@link #isDaemon},
     * {@link #poolName} settings will not be considered when creating new threads.
     *
     * @return {@link ThreadFactory} that will be used to create thread pool threads.
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Set {@link ThreadFactory} that will be used to create thread pool threads.
     *
     * @param threadFactory custom {@link ThreadFactory}
     *                      If {@link ThreadFactory} is set, then {@link #priority}, {@link #isDaemon},
     *                      {@link #poolName} settings will not be considered when creating new threads.
     * @return the {@link ThreadPoolConfig} with the new {@link ThreadFactory}
     */
    public ThreadPoolConfig setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    /**
     * Return thread pool name. The default is "Tyrus-client".
     *
     * @return the thread pool name.
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Set thread pool name. The default is "Tyrus-client".
     *
     * @param poolName the thread pool name.
     * @return the {@link ThreadPoolConfig} with the new thread pool name.
     */
    public ThreadPoolConfig setPoolName(String poolName) {
        this.poolName = poolName;
        return this;
    }

    /**
     * Get priority of the threads in thread pool. The default is {@link Thread#NORM_PRIORITY}.
     *
     * @return priority of the threads in thread pool.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set priority of the threads in thread pool. The default is {@link Thread#NORM_PRIORITY}.
     *
     * @param priority of the threads in thread pool.
     * @return the {@link ThreadPoolConfig} with the new thread priority.
     */
    public ThreadPoolConfig setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Return {@code true} if thread pool threads are daemons. The default is {@code true}.
     *
     * @return {@code true} if thread pool threads are daemons.
     */
    public boolean isDaemon() {
        return isDaemon;
    }

    /**
     * Set {@code true} if thread pool threads are daemons. The default is {@code true}.
     *
     * @param isDaemon {@code true} if thread pool threads are daemons.
     * @return the {@link ThreadPoolConfig} with the daemon property set.
     */
    public ThreadPoolConfig setDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
        return this;
    }

    /**
     * Get max thread pool size. The default is {@code Math.max(Runtime.getRuntime().availableProcessors(), 20)}
     *
     * @return max thread pool size.
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Set max thread pool size. The default is The default is {@code Math.max(Runtime.getRuntime().availableProcessors(), 20)}.
     * <p/>
     * Cannot be smaller than 3.
     *
     * @param maxPoolSize the max thread pool size.
     * @return the {@link ThreadPoolConfig} with the new max pool size set.
     */
    public ThreadPoolConfig setMaxPoolSize(int maxPoolSize) {
        if (maxPoolSize < 3) {
            throw new IllegalArgumentException(LocalizationMessages.THREAD_POOL_MAX_SIZE_TOO_SMALL());
        }

        this.maxPoolSize = maxPoolSize;
        return this;
    }

    /**
     * Get the core thread pool size - the size of the thread pool will never bee smaller than this.
     * <p/>
     * The default is 1.
     *
     * @return the core thread pool size - the size of the thread pool will never bee smaller than this.
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Set the core thread pool size - the size of the thread pool will never bee smaller than this.
     * <p/>
     * The default is 1.
     *
     * @param corePoolSize the core thread pool size - the size of the thread pool will never bee smaller than this.
     * @return the {@link ThreadPoolConfig} with the new core pool size set.
     */
    public ThreadPoolConfig setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException(LocalizationMessages.THREAD_POOL_CORE_SIZE_TOO_SMALL());
        }

        this.corePoolSize = corePoolSize;
        return this;
    }

    /**
     * Get the limit of the queue, where tasks are temporarily stored when all threads are busy.
     * <p/>
     * Value less than 0 means unlimited queue. The default is -1.
     *
     * @return the thread-pool queue limit. The queue limit
     */
    public int getQueueLimit() {
        return queueLimit;
    }

    /**
     * Set the limit of the queue, where tasks are temporarily stored when all threads are busy.
     * <p/>
     * Value less than 0 means unlimited queue. The default is -1.
     *
     * @param queueLimit the thread pool queue limit. The <tt>queueLimit</tt> value less than 0 means unlimited queue.
     * @return the {@link ThreadPoolConfig} with the new queue limit.
     */
    public ThreadPoolConfig setQueueLimit(int queueLimit) {
        if (queueLimit < 0) {
            this.queueLimit = -1;
        } else {
            this.queueLimit = queueLimit;
        }
        return this;
    }

    /**
     * The max period of time a thread will wait for a new task to process.
     * <p/>
     * If the timeout expires and the thread is not a core one (see {@link #setCorePoolSize(int)}, {@link #setMaxPoolSize(int)})
     * - then the thread will be terminated and removed from the thread pool.
     * <p/>
     * The default is 10s.
     *
     * @param time max keep alive timeout. The value less than 0 means no timeout.
     * @param unit time unit.
     * @return the {@link ThreadPoolConfig} with the new keep alive time.
     */
    public ThreadPoolConfig setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0) {
            keepAliveTimeMillis = -1;
        } else {
            keepAliveTimeMillis = TimeUnit.MILLISECONDS.convert(time, unit);
        }
        return this;
    }

    /**
     * Get the max period of time a thread will wait for a new task to process.
     * <p/>
     * If the timeout expires and the thread is not a core one (see {@link #setCorePoolSize(int)}, {@link #setMaxPoolSize(int)})
     * - then the thread will be terminated and removed from the thread pool.
     * <p/>
     * The default is 10s.
     *
     * @return the keep-alive timeout, the value less than 0 means no timeout.
     */
    public long getKeepAliveTime(TimeUnit timeUnit) {
        if (keepAliveTimeMillis == -1) {
            return -1;
        }

        return timeUnit.convert(keepAliveTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Get the class loader (if any) to be initially exposed by threads from this pool.
     * <p/>
     * If not specified, the class loader of the parent thread that initialized the pool will be used.
     *
     * @return the class loader (if any) to be initially exposed by threads from this pool.
     */
    public ClassLoader getInitialClassLoader() {
        return initialClassLoader;
    }

    /**
     * Specifies the context class loader that will be used by threads in this pool.
     * <p/>
     * If not specified, the class loader of the parent thread that initialized the pool will be used.
     *
     * @param initialClassLoader the class loader to be exposed by threads of this pool.
     * @return the {@link ThreadPoolConfig} with the class loader set.
     * @see Thread#getContextClassLoader()
     */
    public ThreadPoolConfig setInitialClassLoader(final ClassLoader initialClassLoader) {
        this.initialClassLoader = initialClassLoader;
        return this;
    }

    @Override
    public String toString() {
        return ThreadPoolConfig.class.getSimpleName() + " :\r\n"
                + "  poolName: " + poolName + "\r\n"
                + "  corePoolSize: " + corePoolSize + "\r\n"
                + "  maxPoolSize: " + maxPoolSize + "\r\n"
                + "  queue: " + (queue != null ? queue.getClass() : "undefined") + "\r\n"
                + "  queueLimit: " + queueLimit + "\r\n"
                + "  keepAliveTime (millis): " + keepAliveTimeMillis + "\r\n"
                + "  threadFactory: " + threadFactory + "\r\n"
                + "  priority: " + priority + "\r\n"
                + "  isDaemon: " + isDaemon + "\r\n"
                + "  initialClassLoader: " + initialClassLoader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ThreadPoolConfig that = (ThreadPoolConfig) o;

        if (corePoolSize != that.corePoolSize) {
            return false;
        }
        if (isDaemon != that.isDaemon) {
            return false;
        }
        if (keepAliveTimeMillis != that.keepAliveTimeMillis) {
            return false;
        }
        if (maxPoolSize != that.maxPoolSize) {
            return false;
        }
        if (priority != that.priority) {
            return false;
        }
        if (queueLimit != that.queueLimit) {
            return false;
        }
        if (initialClassLoader != null ? !initialClassLoader.equals(that.initialClassLoader) : that.initialClassLoader != null) {
            return false;
        }
        if (poolName != null ? !poolName.equals(that.poolName) : that.poolName != null) {
            return false;
        }
        if (queue != null ? !queue.equals(that.queue) : that.queue != null) {
            return false;
        }
        if (threadFactory != null ? !threadFactory.equals(that.threadFactory) : that.threadFactory != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = poolName != null ? poolName.hashCode() : 0;
        result = 31 * result + corePoolSize;
        result = 31 * result + maxPoolSize;
        result = 31 * result + (queue != null ? queue.hashCode() : 0);
        result = 31 * result + queueLimit;
        result = 31 * result + (int) (keepAliveTimeMillis ^ (keepAliveTimeMillis >>> 32));
        result = 31 * result + (threadFactory != null ? threadFactory.hashCode() : 0);
        result = 31 * result + priority;
        result = 31 * result + (isDaemon ? 1 : 0);
        result = 31 * result + (initialClassLoader != null ? initialClassLoader.hashCode() : 0);
        return result;
    }
}
