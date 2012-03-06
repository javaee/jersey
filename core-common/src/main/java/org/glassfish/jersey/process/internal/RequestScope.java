/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.util.LazyUid;

import org.glassfish.hk2.Provider;
import org.glassfish.hk2.Scope;
import org.glassfish.hk2.ScopeInstance;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkState;

/**
 * Scopes a single request/response processing execution.
 *<p />
 * This implementation is derived from Guice Wiki article on
 * <a href="http://code.google.com/p/google-guice/wiki/CustomScopes">Custom Scopes</a>:
 * <p />
 * Apply this scope with a <code>try&hellip;finally</code> block:
 *
 * <pre><code>
 *   scope.enter();
 *   try {
 *     // explicitly seed some seed objects...
 *     scope.seed(Key.value(SomeObject.class), someObject);
 *     // create and access scoped objects
 *   } finally {
 *     scope.exit();
 *   }
 * </code></pre>
 *
 * The scope can be initialized with one or more seed instances by calling
 * <code>seed(key, instance)</code> before the injector will be called upon to
 * provide for this key. A typical use is for a Servlet filter to enter/exit the
 * scope, representing a Request Scope, and seed HttpServletRequest and
 * HttpServletResponse.  For each key inserted with seed(), it's good practice
 * (since you have to provide <i>some</i> binding anyhow) to include a
 * corresponding binding that will throw an exception if Guice is asked to
 * provide for that key if it was not yet seeded:
 *
 * <pre><code>
 *   bind(key)
 *       .toProvider(RequestScope.<KeyClass>unseededKeyProvider())
 *       .in(ScopeAnnotation.class);
 * </code></pre>
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class RequestScope implements Scope {

    private static final Logger LOGGER = Logger.getLogger(RequestScope.class.getName());

    public static class Module extends AbstractModule {

        @Override
        protected void configure() {
            final RequestScope requestScope = new RequestScope();
            bind(RequestScope.class).toInstance(requestScope);
        }
    }
    //
    /**
     * A thread local copy of the current scope instance.
     */
    private final ThreadLocal<Instance> currentScopeInstance = new ThreadLocal<Instance>();

    @Override
    public ScopeInstance current() {
        return getCurrentScopeInstance();
    }

    private Instance getCurrentScopeInstance() throws IllegalStateException {
        Instance scopeInstance = currentScopeInstance.get();
        checkState(scopeInstance != null, "Not inside a request scope.");
        return scopeInstance;
    }

    /**
     * Implementation of the request scope instance.
     */
    private static final class Instance implements ScopeInstance {
        /*
         * Scope instance UUID.
         *
         * For performance reasons, it's only generated if toString() method is invoked,
         * e.g. as part of some low-level logging.
         */

        private final LazyUid id = new LazyUid();
        /**
         * A map of injectable instances in this scope.
         */
        private final Map<Provider<?>, Object> store;
        /**
         * Holds the number of snapshots of this scope.
         */
        private final AtomicInteger referenceCounter;

        private Instance() {
            this.store = new HashMap<Provider<?>, Object>();
            this.referenceCounter = new AtomicInteger(1);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Provider<T> inhabitant) {
            return (T) store.get(inhabitant);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T put(Provider<T> inhabitant, T value) {
            checkState(!store.containsKey(inhabitant), "An instance for the provider %s was "
                    + "already seeded in this scope. Old instance: %s New instance: %s",
                    inhabitant, store.get(inhabitant), value);

            return (T) store.put(inhabitant, value);
        }

        @Override
        public <T> boolean contains(Provider<T> provider) {
            return store.containsKey(provider);
        }

        public Instance snapshot() {
            referenceCounter.incrementAndGet();
            return this;
        }

        @Override
        public void release() {
            if (referenceCounter.decrementAndGet() < 1) {
                store.clear();
            }
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("id", id.value())
                    .add("referenceCounter", referenceCounter.get())
                    .add("store size", store.size())
                    .toString();
        }
    }

    /**
     * An opaque {@link RequestScope} state holder.
     * <p />
     * Instances of this class can be used to transfer an active {@code RequestScope}
     * scope block from one thread to another.
     */
    public static final class Snapshot {

        private final Instance scopeInstance;

        private Snapshot(Instance instance) {
            this.scopeInstance = instance.snapshot();
        }
    }

    /**
     * Takes snapshot of the state of the active {@link RequestScope} scope block
     * in the current thread.
     *
     * @return currently active {@code RequestScope} scope block state snapshot
     * @throws IllegalStateException in case there is no active {@code RequestScope}
     *     scope block in the current thread.
     */
    public Snapshot takeSnapshot() throws IllegalStateException {
        return new Snapshot(getCurrentScopeInstance());
    }

    /**
     * Provides information whether the current code is executed in a context of
     * an active request scope.
     *
     * @return {@code true} if the current code runs in a context of an active
     *     request scope, {@code false} otherwise.
     */
    public boolean isActive() {
        return currentScopeInstance.get() != null;
    }

    /**
     * Enters the new {@link RequestScope} scope block in the current thread.
     * <p />
     * NOTE: This method must not be called from within an active {@code RequestScope}
     * scope block in the same thread.
     *
     * @throws IllegalStateException in case the method is called from an already
     *     active {@code RequestScope} scope block in the same thread.
     */
    public void enter() throws IllegalStateException {
        checkState(currentScopeInstance.get() == null, "A scoped block is already in progress");
        final Instance scopeInstance = new Instance();
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER,
                    "Entering scope %s on thread %s",
                    new Object[]{scopeInstance.toString(), Thread.currentThread().getName()});
        }
        currentScopeInstance.set(scopeInstance);
    }

    /**
     * Resumes/continues an existing {@link RequestScope} scope block in the
     * current thread. All scope data are initialized from the provided
     * scope {@link Snapshot snapshot}.
     * <p />
     * NOTE: This method must not be called from within an active {@code RequestScope}
     * scope block in the same thread otherwise an exception will be thrown.
     *
     * @param snapshot snapshot of the scope block that should be resumed
     *     in the current thread
     * @throws IllegalStateException in case the method is called from an already
     *     active {@code RequestScope} scope block in the same thread.
     */
    public void enter(Snapshot snapshot) throws IllegalStateException {
        checkState(currentScopeInstance.get() == null, "A scoped block is already in progress");
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER,
                    "Resuming scope %s on thread %s",
                    new Object[]{snapshot.scopeInstance.toString(), Thread.currentThread().getName()});
        }
        currentScopeInstance.set(snapshot.scopeInstance);
    }

    /**
     * Exits the active scope block in the current thread. All scoped instances are discarded.
     * <p />
     * NOTE: This method must be called only from within an active {@code RequestScope}
     * scope block in the current thread.
     *
     * @throws IllegalStateException in case there is no active {@code RequestScope} scope
     *     block to exit in the current thread.
     */
    public void exit() throws IllegalStateException {
        final Instance scopeInstance = currentScopeInstance.get();
        checkState(scopeInstance != null, "No scoped block in progress");
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER,
                    "Exiting scope %s on thread %s",
                    new Object[]{scopeInstance.toString(), Thread.currentThread().getName()});
        }
        currentScopeInstance.remove();
        scopeInstance.release();
    }
}
