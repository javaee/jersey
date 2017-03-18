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
package org.glassfish.jersey.client;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executor for client async processing and background task scheduling.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @since 2.26
 */
public interface ClientExecutor {
    /**
     * Submits a value-returning task for execution and returns a {@link Future} representing the pending results of the task.
     * The Future's {@code get()} method will return the task's result upon successful completion.
     *
     * @param task task to submit
     * @param <T>  task's return type
     * @return a {@code Future} representing pending completion of the task
     * @throws {@link java.util.concurrent.RejectedExecutionException} if the task cannot be scheduled for execution
     * @throws {@link NullPointerException} if the task is null
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a {@link Runnable} task for execution and returns a {@link Future} representing that task. The Future's {@code
     * get()} method will return the given result upon successful completion.
     *
     * @param task the task to submit
     * @return a  {@code Future} representing pending completion of the task
     * @throws {@link java.util.concurrent.RejectedExecutionException} if the task cannot be scheduled for execution
     * @throws {@link NullPointerException} if the task is null
     */
    Future<?> submit(Runnable task);

    /**
     * Submits a {@link Runnable} task for execution and returns a {@link Future} representing that task. The Future's {@code
     * get()} method will return the given result upon successful completion.
     *
     * @param task   the task to submit
     * @param result the result to return
     * @param <T>    result type
     * @return a {@code Future} representing pending completion of the task
     * @throws {@link java.util.concurrent.RejectedExecutionException} if the task cannot be scheduled for execution
     * @throws {@link NullPointerException} if the task is null
     */
    <T> Future<T> submit(Runnable task, T result);

    /**
     * Creates and executes a {@link ScheduledFuture} that becomes enabled after the given delay.
     *
     * @param callable the function to execute
     * @param delay    the time from now to delay execution
     * @param unit     the time unit of the delay parameter
     * @param <T>      return type of the function
     * @return a {@code ScheduledFuture} that can be used to extract result or cancel
     * @throws {@link java.util.concurrent.RejectedExecutionException} if the task cannot be scheduled for execution
     * @throws {@link NullPointerException} if callable is null
     */
    <T> ScheduledFuture<T> schedule(Callable<T> callable, long delay, TimeUnit unit);

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     *
     * @param command the task to execute
     * @param delay   the time from now to delay execution
     * @param unit    the time unit of the daly parameter
     * @return a scheduledFuture representing pending completion of the task and whose {@code get()} method will return {@code
     * null} upon completion
     */
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);


}
