/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.util.collection;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.util.JdkVersion;

/**
 * Utility class, which tries to pickup the best collection implementation depending
 * on running environment.
 *
 * @author Gustav Trede
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.3
 */
public final class DataStructures {

    private static final Class<?> LTQ_CLASS;

    static {
        String className = null;

        Class<?> c;
        try {
            final JdkVersion jdkVersion = JdkVersion.getJdkVersion();
            final JdkVersion minimumVersion = JdkVersion.parseVersion("1.7.0");

            className = (minimumVersion.compareTo(jdkVersion) <= 0)
                    ? "java.util.concurrent.LinkedTransferQueue"
                    : "org.glassfish.jersey.internal.util.collection.LinkedTransferQueue";

            c = getAndVerify(className);
            Logger.getLogger(DataStructures.class.getName()).log(Level.FINE, "USING LTQ class:{0}", c);
        } catch (final Throwable t) {
            Logger.getLogger(DataStructures.class.getName()).log(Level.FINE,
                    "failed loading data structure class:" + className
                            + " fallback to embedded one", t);

            c = LinkedBlockingQueue.class; // fallback to LinkedBlockingQueue
        }

        LTQ_CLASS = c;
    }

    /**
     * Default concurrency level calculated based on the number of available CPUs.
     */
    public static final int DEFAULT_CONCURENCY_LEVEL = ceilingNextPowerOfTwo(Runtime.getRuntime().availableProcessors());

    private static int ceilingNextPowerOfTwo(final int x) {
        // Hacker's Delight, Chapter 3, Harry S. Warren Jr.
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(x - 1));
    }

    private static Class<?> getAndVerify(final String cn) throws Throwable {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws Exception {
                    return DataStructures.class.getClassLoader().loadClass(cn).newInstance().getClass();
                }
            });
        } catch (final PrivilegedActionException ex) {
            throw ex.getCause();
        }
    }

    /**
     * Create an instance of a {@link BlockingQueue} that is based on
     * {@code LinkedTransferQueue} implementation from JDK 7.
     * <p>
     * When running on JDK 7 or higher, JDK {@code LinkedTransferQueue} implementation is used,
     * on JDK 6 an internal Jersey implementation class is used.
     * </p>
     *
     * @param <E> the type of elements held in the queue.
     * @return new instance of a {@link BlockingQueue} that is based on {@code LinkedTransferQueue}
     *         implementation from JDK 7.
     */
    @SuppressWarnings("unchecked")
    public static <E> BlockingQueue<E> createLinkedTransferQueue() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<BlockingQueue<E>>() {
                @Override
                public BlockingQueue<E> run() throws Exception {
                    return (BlockingQueue<E>) LTQ_CLASS.newInstance();
                }
            });
        } catch (final PrivilegedActionException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
    }

    /**
     * Creates a new, empty map with a default initial capacity (16),
     * load factor (0.75) and concurrencyLevel (16).
     * <p>
     * On Oracle JDK, the factory method will return an instance of
     * <a href="http://gee.cs.oswego.edu/dl/jsr166/dist/jsr166edocs/jsr166e/ConcurrentHashMapV8.html">
     * {@code ConcurrentHashMapV8}</a>
     * that is supposed to be available in JDK 8 and provides better performance and memory characteristics than
     * {@link ConcurrentHashMap} implementation from JDK 7 or earlier. On non-Oracle JDK,
     * the factory instantiates the standard {@code ConcurrentHashMap} from JDK.
     * </p>
     *
     * @return the map.
     */
    public static <K, V> ConcurrentMap<K, V> createConcurrentMap() {
        return JdkVersion.getJdkVersion().isUnsafeSupported()
                ? new ConcurrentHashMapV8<K, V>()
                : new ConcurrentHashMap<K, V>();
    }

    /**
     * Creates a new map with the same mappings as the given map.
     * <p>
     * On Oracle JDK, the factory method will return an instance of
     * <a href="http://gee.cs.oswego.edu/dl/jsr166/dist/jsr166edocs/jsr166e/ConcurrentHashMapV8.html">
     * {@code ConcurrentHashMapV8}</a>
     * that is supposed to be available in JDK 8 and provides better performance and memory characteristics than
     * {@link ConcurrentHashMap} implementation from JDK 7 or earlier. On non-Oracle JDK,
     * the factory instantiates the standard {@code ConcurrentHashMap} from JDK.
     * </p>
     *
     * @param map the map.
     */
    public static <K, V> ConcurrentMap<K, V> createConcurrentMap(
            final Map<? extends K, ? extends V> map) {
        return JdkVersion.getJdkVersion().isUnsafeSupported()
                ? new ConcurrentHashMapV8<K, V>(map)
                : new ConcurrentHashMap<K, V>(map);
    }

    /**
     * Creates a new, empty map with an initial table size  accommodating the specified
     * number of elements without the need to dynamically resize.
     * <p>
     * On Oracle JDK, the factory method will return an instance of
     * <a href="http://gee.cs.oswego.edu/dl/jsr166/dist/jsr166edocs/jsr166e/ConcurrentHashMapV8.html">
     * {@code ConcurrentHashMapV8}</a>
     * that is supposed to be available in JDK 8 and provides better performance and memory characteristics than
     * {@link ConcurrentHashMap} implementation from JDK 7 or earlier. On non-Oracle JDK,
     * the factory instantiates the standard {@code ConcurrentHashMap} from JDK.
     * </p>
     *
     * @param initialCapacity The implementation performs internal
     *                        sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative.
     */
    public static <K, V> ConcurrentMap<K, V> createConcurrentMap(
            final int initialCapacity) {
        return JdkVersion.getJdkVersion().isUnsafeSupported()
                ? new ConcurrentHashMapV8<K, V>(initialCapacity)
                : new ConcurrentHashMap<K, V>(initialCapacity);
    }

    /**
     * Creates a new, empty map with an initial table size based on the given number of elements
     * ({@code initialCapacity}), table density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     * <p>
     * On Oracle JDK, the factory method will return an instance of
     * <a href="http://gee.cs.oswego.edu/dl/jsr166/dist/jsr166edocs/jsr166e/ConcurrentHashMapV8.html">
     * {@code ConcurrentHashMapV8}</a>
     * that is supposed to be available in JDK 8 and provides better performance and memory characteristics than
     * {@link ConcurrentHashMap} implementation from JDK 7 or earlier. On non-Oracle JDK,
     * the factory instantiates the standard {@code ConcurrentHashMap} from JDK.
     * </p>
     *
     * @param initialCapacity  the initial capacity. The implementation
     *                         performs internal sizing to accommodate this many elements,
     *                         given the specified load factor.
     * @param loadFactor       the load factor (table density) for
     *                         establishing the initial table size.
     * @param concurrencyLevel the estimated number of concurrently
     *                         updating threads. The implementation may use this value as
     *                         a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     *                                  negative or the load factor or concurrencyLevel are
     *                                  not positive.
     */
    public static <K, V> ConcurrentMap<K, V> createConcurrentMap(
            final int initialCapacity, final float loadFactor,
            final int concurrencyLevel) {
        return JdkVersion.getJdkVersion().isUnsafeSupported()
                ? new ConcurrentHashMapV8<K, V>(initialCapacity, loadFactor, concurrencyLevel)
                : new ConcurrentHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel);
    }
}
