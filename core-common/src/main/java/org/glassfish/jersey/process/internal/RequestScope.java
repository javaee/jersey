/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.process.internal;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.BootstrapBag;
import org.glassfish.jersey.internal.BootstrapConfigurator;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.Producer;

import static org.glassfish.jersey.internal.guava.Preconditions.checkState;

/**
 * Scopes a single request/response processing execution on a single thread.
 * <p>
 * To execute a code inside of the request scope use one of the {@code runInScope(...)}
 * methods and supply the task encapsulating the code that should be executed in the scope.
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>
 * &#064;Inject
 * RequestScope requestScope;
 *
 * ...
 *
 * requestScope.runInScope(new Runnable() {
 *     &#064;Override
 *     public void run() {
 *          System.out.println("This is executed in the request scope...");
 *     }
 * });
 * </pre>
 * <p>
 * An instance of the request scope can be suspended and retrieved via a call to
 * {@link RequestScope#suspendCurrent} method. This instance can be later
 * used to resume the same request scope and run another task in the same scope:
 * </p>
 * <pre>
 *  RequestContext requestScopeContext =
 *      requestScope.runInScope(new Callable&lt;Instance&gt;() {
 *          &#064;Override
 *          public RequestContext call() {
 *              // This is executed in the new request scope.
 *
 *              // The following call will cause that the
 *              // RequestContext will not be released
 *              // automatically and we will have to release
 *              // it explicitly at the end.
 *              return requestScope.suspendCurrent();
 *          }
 *      });
 *
 *  requestScope.runInScope(requestScopeContext, new Runnable() {
 *
 *      &#064;Override
 *      public void run() {
 *          // This is executed in the same request scope as code above.
 *      }
 *  });
 *
 *  // The scope context must be explicitly released.
 *  requestScopeContext.release();
 * </pre>
 * <p>
 * In the previous example the {@link RequestContext request scope context}
 * was suspended and retrieved which also informs {@code requestScope} that it
 * should not automatically release the instance once the running task is finished.
 * The {@code requestScopeContext} is then used to initialize the next
 * request-scoped execution. The second task will run in the same request scope as the
 * first task. At the end the suspended {@code requestScopeContext} must be
 * manually {@link RequestContext#release released}. Not releasing the instance
 * could cause memory leaks. Please note that calling {@link RequestScope#suspendCurrent}
 * does not retrieve an immutable snapshot of the current request scope but
 * a live reference to the internal {@link RequestContext request scope context}
 * which may change it's state during each request-scoped task execution for
 * which this scope context is used.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
public abstract class RequestScope {

    private static final ExtendedLogger logger = new ExtendedLogger(Logger.getLogger(RequestScope.class.getName()), Level.FINEST);

    /**
     * A thread local copy of the current scope context.
     */
    private final ThreadLocal<RequestContext> currentRequestContext = new ThreadLocal<>();
    private volatile boolean isActive = true;

    public boolean isActive() {
        return isActive;
    }

    public void shutdown() {
        isActive = false;
    }

    /**
     * Get a new reference for to currently running request scope context. This call
     * prevents automatic {@link RequestContext#release() release} of the scope
     * context once the task that runs in the scope has finished.
     * <p>
     * The returned scope context may be used to run additional task(s) in the
     * same request scope using one of the {@code #runInScope(RequestContext, ...)} methods.
     * </p>
     * <p>
     * Note that the returned context must be {@link RequestContext#release()
     * released} manually once not needed anymore to prevent memory leaks.
     * </p>
     *
     * @return currently active {@link RequestContext request scope context}.
     * @throws IllegalStateException in case there is no active request scope associated
     *                               with the current thread or if the request scope has
     *                               been already shut down.
     * @see #suspendCurrent()
     */
    public RequestContext referenceCurrent() throws IllegalStateException {
        return current().getReference();
    }

    /**
     * Returns the current {@link RequestContext} which has to be active on the given thread.
     *
     * @return current active request context.
     */
    public RequestContext current() {
        checkState(isActive, "Request scope has been already shut down.");

        final RequestContext scopeInstance = currentRequestContext.get();
        checkState(scopeInstance != null, "Not inside a request scope.");

        return scopeInstance;
    }

    private RequestContext retrieveCurrent() {
        checkState(isActive, "Request scope has been already shut down.");
        return currentRequestContext.get();
    }

    /**
     * Get the current {@link RequestContext request scope context}
     * and mark it as suspended. This call prevents automatic
     * {@link RequestContext#release() release} of the scope context
     * once the task that runs in the scope has finished.
     * <p>
     * The returned scope context may be used to run additional task(s) in the
     * same request scope using one of the {@code #runInScope(RequestContext, ...)}
     * methods.
     * </p>
     * <p>
     * Note that the returned context must be {@link RequestContext#release()
     * released} manually once not needed anymore to prevent memory leaks.
     * </p>
     *
     * @return currently active {@link RequestContext request scope context}
     * that was suspended or {@code null} if the thread is not currently running
     * in an active request scope.
     * @see #referenceCurrent()
     */
    public RequestContext suspendCurrent() {
        final RequestContext context = retrieveCurrent();
        if (context == null) {
            return null;
        }
        try {
            RequestContext referencedContext = context.getReference();
            suspend(referencedContext);
            return referencedContext;
        } finally {
            logger.debugLog("Returned a new reference of the request scope context {0}", context);
        }
    }

    /**
     * Executes the action when the request scope comes into suspended state. For example, implementation can call deactivation
     * of the underlying request scope storage.
     *
     * @param context current request context to be suspended.
     */
    protected void suspend(RequestContext context) {
    }

    /**
     * Creates a new instance of the {@link RequestContext request scope context}.
     * This instance can be then used to run task in the request scope. Returned context
     * is suspended by default and must therefore be closed explicitly as it is shown in
     * the following example:
     * <pre>
     * RequestContext context = requestScope.createContext();
     * requestScope.runInScope(context, someRunnableTask);
     * context.release();
     * </pre>
     *
     * @return New suspended request scope context.
     */
    public abstract RequestContext createContext();

    /**
     * Stores the provided {@link RequestContext} to thread-local variable belonging to current request scope.
     *
     * @param context storage with request scoped objects.
     */
    protected void activate(RequestContext context, RequestContext oldContext) {
        checkState(isActive, "Request scope has been already shut down.");
        currentRequestContext.set(context);
    }

    /**
     * Resumes the provided {@link RequestContext} to thread-local variable belonging to current request scope.
     *
     * @param context storage with request scoped objects.
     */
    protected void resume(RequestContext context) {
        currentRequestContext.set(context);
    }

    /**
     * Releases the provided {@link RequestContext} to thread-local variable belonging to current request scope.
     *
     * @param context storage with request scoped objects.
     */
    protected void release(RequestContext context) {
        context.release();
    }

    /**
     * Runs the {@link Runnable task} in the request scope initialized from the
     * {@link RequestContext scope context}. The {@link RequestContext
     * scope context} is NOT released by the method (this must be done explicitly). The
     * current thread might be already in any request scope and in that case the scope
     * will be changed to the scope defined by the {@link RequestContext scope
     * instance}. At the end of the method the request scope is returned to its original
     * state.
     *
     * @param context The request scope context from which the request scope will be initialized.
     * @param task    Task to be executed.
     */
    public void runInScope(RequestContext context, Runnable task) {
        final RequestContext oldContext = retrieveCurrent();
        try {
            activate(context.getReference(), oldContext);
            Errors.process(task);
        } finally {
            release(context);
            resume(oldContext);
        }
    }

    /**
     * Runs the {@link Runnable task} in the new request scope. The current thread might
     * be already in any request scope and in that case the scope will be changed to the
     * scope defined by the {@link RequestContext scope context}. At the end of
     * the method the request scope is returned to its original state. The newly created
     * {@link RequestContext scope context} will be implicitly released at the end
     * of the method call except the task will call
     * {@link RequestScope#suspendCurrent}.
     *
     * @param task Task to be executed.
     */
    public void runInScope(Runnable task) {
        final RequestContext oldContext = retrieveCurrent();
        final RequestContext context = createContext();
        try {
            activate(context, oldContext);
            Errors.process(task);
        } finally {
            release(context);
            resume(oldContext);
        }
    }

    /**
     * Runs the {@link Callable task} in the request scope initialized from the
     * {@link RequestContext scope context}. The {@link RequestContext
     * scope context} is NOT released by the method (this must be done explicitly). The
     * current thread might be already in any request scope and in that case the scope
     * will be changed to the scope defined by the {@link RequestContext scope
     * instance}. At the end of the method the request scope is returned to its original
     * state.
     *
     * @param context The request scope context from which the request scope will be initialized.
     * @param task    Task to be executed.
     * @param <T>     {@code task} result type.
     * @return result returned by the {@code task}.
     * @throws Exception Exception thrown by the {@code task}.
     */
    public <T> T runInScope(RequestContext context, Callable<T> task) throws Exception {
        final RequestContext oldContext = retrieveCurrent();
        try {
            activate(context.getReference(), oldContext);
            return Errors.process(task);
        } finally {
            release(context);
            resume(oldContext);
        }
    }

    /**
     * Runs the {@link Callable task} in the new request scope. The current thread might
     * be already in any request scope and in that case the scope will be changed to the
     * scope defined by the {@link RequestContext scope context}. At the end of
     * the method the request scope is returned to its original state. The newly created
     * {@link RequestContext scope context} will be implicitly released at the end
     * of the method call except the task will call
     * {@link RequestScope#suspendCurrent}.
     *
     * @param task Task to be executed.
     * @param <T>  {@code task} result type.
     * @return result returned by the {@code task}.
     * @throws Exception Exception thrown by the {@code task}.
     */
    public <T> T runInScope(Callable<T> task) throws Exception {
        final RequestContext oldContext = retrieveCurrent();
        final RequestContext context = createContext();
        try {
            activate(context, oldContext);
            return Errors.process(task);
        } finally {
            release(context);
            resume(oldContext);
        }
    }

    /**
     * Runs the {@link org.glassfish.jersey.internal.util.Producer task} in the request scope initialized
     * from the {@link RequestContext scope context}.
     * The {@link RequestContext scope context} is NOT released by the method (this
     * must be done explicitly). The current thread might be already in any request scope
     * and in that case the scope will be changed to the scope defined by the
     * {@link RequestContext scope context}. At the end of the method the request
     * scope is returned to its original state.
     *
     * @param context The request scope context from which the request scope will be initialized.
     * @param task    Task to be executed.
     * @param <T>     {@code task} result type.
     * @return result returned by the {@code task}
     */
    public <T> T runInScope(RequestContext context, Producer<T> task) {
        final RequestContext oldContext = retrieveCurrent();
        try {
            activate(context.getReference(), oldContext);
            return Errors.process(task);
        } finally {
            release(context);
            resume(oldContext);
        }
    }

    /**
     * Runs the {@link org.glassfish.jersey.internal.util.Producer task} in the new request scope. The
     * current thread might be already in any request scope and in that case the scope
     * will be changed to the scope defined by the {@link RequestContext scope
     * instance}. At the end of the method the request scope is returned to its original
     * state. The newly created {@link RequestContext scope context} will be
     * implicitly released at the end of the method call except the task will call
     * {@link RequestScope#suspendCurrent}.
     *
     * @param task Task to be executed.
     * @param <T>  {@code task} result type.
     * @return result returned by the {@code task}.
     */
    public <T> T runInScope(Producer<T> task) {
        final RequestContext oldContext = retrieveCurrent();
        final RequestContext context = createContext();
        try {
            activate(context, oldContext);
            return Errors.process(task);
        } finally {
            release(context);
            resume(oldContext);
        }
    }

    /**
     * Configurator which initializes and register {@link RequestScope} instance int {@link InjectionManager} and
     * {@link BootstrapBag}.
     *
     * @author Petr Bouda (petr.bouda at oracle.com)
     */
    public static class RequestScopeConfigurator implements BootstrapConfigurator {

        @Override
        public void init(InjectionManager injectionManagerFactory, BootstrapBag bootstrapBag) {
        }

        @Override
        public void postInit(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
            RequestScope requestScope = injectionManager.getInstance(RequestScope.class);
            bootstrapBag.setRequestScope(requestScope);
        }
    }
}
