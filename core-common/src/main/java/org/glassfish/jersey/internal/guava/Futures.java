/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.jersey.internal.guava;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to the {@link Future} interface.
 * <p>
 * <p>Many of these methods use the {@link ListenableFuture} API; consult the
 * Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/ListenableFutureExplained">
 * {@code ListenableFuture}</a>.
 *
 * @author Kevin Bourrillion
 * @author Nishant Thakkar
 * @author Sven Mawson
 * @since 1.0
 */
public final class Futures {
    private Futures() {
    }

    /**
     * Creates a {@code ListenableFuture} which has its value set immediately upon
     * construction. The getters just return the value. This {@code Future} can't
     * be canceled or timed out and its {@code isDone()} method always returns
     * {@code true}.
     */
    public static <V> ListenableFuture<V> immediateFuture(V value) {
        return new ImmediateSuccessfulFuture<V>(value);
    }

    /**
     * Returns a {@code ListenableFuture} which has an exception set immediately
     * upon construction.
     * <p>
     * <p>The returned {@code Future} can't be cancelled, and its {@code isDone()}
     * method always returns {@code true}. Calling {@code get()} will immediately
     * throw the provided {@code Throwable} wrapped in an {@code
     * ExecutionException}.
     */
    public static <V> ListenableFuture<V> immediateFailedFuture(
            Throwable throwable) {
        checkNotNull(throwable);
        return new ImmediateFailedFuture<V>(throwable);
    }

    /**
     * Returns a new {@code ListenableFuture} whose result is the product of
     * applying the given {@code Function} to the result of the given {@code
     * Future}. Example:
     * <p>
     * <pre>   {@code
     *   ListenableFuture<QueryResult> queryFuture = ...;
     *   Function<QueryResult, List<Row>> rowsFunction =
     *       new Function<QueryResult, List<Row>>() {
     *         public List<Row> apply(QueryResult queryResult) {
     *           return queryResult.getRows();
     *         }
     *       };
     *   ListenableFuture<List<Row>> rowsFuture =
     *       transform(queryFuture, rowsFunction);}</pre>
     * <p>
     * <p>Note: If the transformation is slow or heavyweight, consider {@linkplain
     * #transform(ListenableFuture, Function, Executor) supplying an executor}.
     * If you do not supply an executor, {@code transform} will use an inline
     * executor, which carries some caveats for heavier operations.  For example,
     * the call to {@code function.apply} may run on an unpredictable or
     * undesirable thread:
     * <p>
     * <ul>
     * <li>If the input {@code Future} is done at the time {@code transform} is
     * called, {@code transform} will call {@code function.apply} inline.
     * <li>If the input {@code Future} is not yet done, {@code transform} will
     * schedule {@code function.apply} to be run by the thread that completes the
     * input {@code Future}, which may be an internal system thread such as an
     * RPC network thread.
     * </ul>
     * <p>
     * <p>Also note that, regardless of which thread executes the {@code
     * function.apply}, all other registered but unexecuted listeners are
     * prevented from running during its execution, even if those listeners are
     * to run in other executors.
     * <p>
     * <p>The returned {@code Future} attempts to keep its cancellation state in
     * sync with that of the input future. That is, if the returned {@code Future}
     * is cancelled, it will attempt to cancel the input, and if the input is
     * cancelled, the returned {@code Future} will receive a callback in which it
     * will attempt to cancel itself.
     * <p>
     * <p>An example use of this method is to convert a serializable object
     * returned from an RPC into a POJO.
     *
     * @param input    The future to transform
     * @param function A Function to transform the results of the provided future
     *                 to the results of the returned future.  This will be run in the thread
     *                 that notifies input it is complete.
     * @return A future that holds result of the transformation.
     * @since 9.0 (in 1.0 as {@code compose})
     */
    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
                                                       final Function<? super I, ? extends O> function) {
        checkNotNull(function);
        ChainingListenableFuture<I, O> output =
                new ChainingListenableFuture<I, O>(asAsyncFunction(function), input);
        input.addListener(output, MoreExecutors.directExecutor());
        return output;
    }

    /**
     * Wraps the given function as an AsyncFunction.
     */
    private static <I, O> AsyncFunction<I, O> asAsyncFunction(
            final Function<? super I, ? extends O> function) {
        return new AsyncFunction<I, O>() {
            @Override
            public ListenableFuture<O> apply(I input) {
                O output = function.apply(input);
                return immediateFuture(output);
            }
        };
    }

    private abstract static class ImmediateFuture<V>
            implements ListenableFuture<V> {

        private static final Logger log =
                Logger.getLogger(ImmediateFuture.class.getName());

        @Override
        public void addListener(Runnable listener, Executor executor) {
            checkNotNull(listener, "Runnable was null.");
            checkNotNull(executor, "Executor was null.");
            try {
                executor.execute(listener);
            } catch (RuntimeException e) {
                // ListenableFuture's contract is that it will not throw unchecked
                // exceptions, so log the bad runnable and/or executor and swallow it.
                log.log(Level.SEVERE, "RuntimeException while executing runnable "
                        + listener + " with executor " + executor, e);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public abstract V get() throws ExecutionException;

        @Override
        public V get(long timeout, TimeUnit unit) throws ExecutionException {
            checkNotNull(unit);
            return get();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }

    private static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {

        private final V value;

        ImmediateSuccessfulFuture(V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return value;
        }
    }

    private static class ImmediateFailedFuture<V> extends ImmediateFuture<V> {

        private final Throwable thrown;

        ImmediateFailedFuture(Throwable thrown) {
            this.thrown = thrown;
        }

        @Override
        public V get() throws ExecutionException {
            throw new ExecutionException(thrown);
        }
    }

    /**
     * An implementation of {@code ListenableFuture} that also implements
     * {@code Runnable} so that it can be used to nest ListenableFutures.
     * Once the passed-in {@code ListenableFuture} is complete, it calls the
     * passed-in {@code Function} to generate the result.
     * <p>
     * <p>For historical reasons, this class has a special case in its exception
     * handling: If the given {@code AsyncFunction} throws an {@code
     * UndeclaredThrowableException}, {@code ChainingListenableFuture} unwraps it
     * and uses its <i>cause</i> as the output future's exception, rather than
     * using the {@code UndeclaredThrowableException} itself as it would for other
     * exception types. The reason for this is that {@code Futures.transform} used
     * to require a {@code Function}, whose {@code apply} method is not allowed to
     * throw checked exceptions. Nowadays, {@code Futures.transform} has an
     * overload that accepts an {@code AsyncFunction}, whose {@code apply} method
     * <i>is</i> allowed to throw checked exception. Users who wish to throw
     * checked exceptions should use that overload instead, and <a
     * href="http://code.google.com/p/guava-libraries/issues/detail?id=1548">we
     * should remove the {@code UndeclaredThrowableException} special case</a>.
     */
    private static class ChainingListenableFuture<I, O>
            extends AbstractFuture<O> implements Runnable {

        private AsyncFunction<? super I, ? extends O> function;
        private ListenableFuture<? extends I> inputFuture;
        private volatile ListenableFuture<? extends O> outputFuture;

        private ChainingListenableFuture(
                AsyncFunction<? super I, ? extends O> function,
                ListenableFuture<? extends I> inputFuture) {
            this.function = checkNotNull(function);
            this.inputFuture = checkNotNull(inputFuture);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
      /*
       * Our additional cancellation work needs to occur even if
       * !mayInterruptIfRunning, so we can't move it into interruptTask().
       */
            if (super.cancel(mayInterruptIfRunning)) {
                // This should never block since only one thread is allowed to cancel
                // this Future.
                cancel(inputFuture, mayInterruptIfRunning);
                cancel(outputFuture, mayInterruptIfRunning);
                return true;
            }
            return false;
        }

        private void cancel(Future<?> future,
                            boolean mayInterruptIfRunning) {
            if (future != null) {
                future.cancel(mayInterruptIfRunning);
            }
        }

        @Override
        public void run() {
            try {
                I sourceResult;
                try {
                    sourceResult = Uninterruptibles.getUninterruptibly(inputFuture);
                } catch (CancellationException e) {
                    // Cancel this future and return.
                    // At this point, inputFuture is cancelled and outputFuture doesn't
                    // exist, so the value of mayInterruptIfRunning is irrelevant.
                    cancel(false);
                    return;
                } catch (ExecutionException e) {
                    // Set the cause of the exception as this future's exception
                    setException(e.getCause());
                    return;
                }

                final ListenableFuture<? extends O> outputFuture = this.outputFuture =
                        Preconditions.checkNotNull(
                                function.apply(sourceResult),
                                "AsyncFunction may not return null.");
                if (isCancelled()) {
                    outputFuture.cancel(wasInterrupted());
                    this.outputFuture = null;
                    return;
                }
                outputFuture.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            set(Uninterruptibles.getUninterruptibly(outputFuture));
                        } catch (CancellationException e) {
                            // Cancel this future and return.
                            // At this point, inputFuture and outputFuture are done, so the
                            // value of mayInterruptIfRunning is irrelevant.
                            cancel(false);
                        } catch (ExecutionException e) {
                            // Set the cause of the exception as this future's exception
                            setException(e.getCause());
                        } finally {
                            // Don't pin inputs beyond completion
                            Futures.ChainingListenableFuture.this.outputFuture = null;
                        }
                    }
                }, MoreExecutors.directExecutor());
            } catch (UndeclaredThrowableException e) {
                // Set the cause of the exception as this future's exception
                setException(e.getCause());
            } catch (Throwable t) {
                // This exception is irrelevant in this thread, but useful for the
                // client
                setException(t);
            } finally {
                // Don't pin inputs beyond completion
                function = null;
                inputFuture = null;
            }
        }
    }

}
