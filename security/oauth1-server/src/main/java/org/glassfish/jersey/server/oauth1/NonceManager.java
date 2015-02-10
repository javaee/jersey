/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.oauth1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks the nonces for a given consumer key and/or token. Automagically
 * ensures timestamp is monotonically increasing and tracks all nonces
 * for a given timestamp.
 *
 * @author Paul C. Bryan
 * @author Martin Matula
 * @author Thomas Meire
 */
final class NonceManager {
    /**
     * The maximum valid age of a nonce timestamp, in milliseconds.
     */
    private final long maxAge;

    /**
     * Verifications to perform on average before performing garbage collection.
     */
    private final int gcPeriod;

    /**
     * Counts number of verification requests performed to schedule garbage collection.
     */
    private int gcCounter = 0;

    private final TimeUnit timestampUnit;

    private final long maximumMapSize;

    /**
     * Maps timestamps to key-nonce pairs.
     */
    private final SortedMap<Long, Map<String, Set<String>>> tsToKeyNoncePairs = new TreeMap<>();

    private volatile long mapSize = 0;

    /**
     * Create a new nonce manager configured with maximum age, old nonce cleaning period and a time
     * unit of timestamps.
     *
     * @param maxAge   the maximum valid age of a nonce timestamp, in milliseconds.
     * @param gcPeriod number of verifications to be performed on average before performing garbage collection
     *                 of old nonces.
     * @param timestampUnit unit in which timestamps are passed to {@link #verify(String, String, String)} method.
     * @param maximumCacheSize maximum size of the cache that keeps nonces. If the cache exceeds the method
     *                         {@link #verify(String, String, String)} will return {@code false}.
     */
    public NonceManager(final long maxAge, final int gcPeriod, final TimeUnit timestampUnit, final long maximumCacheSize) {
        if (maxAge <= 0 || gcPeriod <= 0) {
            throw new IllegalArgumentException();
        }

        this.maxAge = maxAge;
        this.gcPeriod = gcPeriod;
        this.timestampUnit = timestampUnit;
        this.maximumMapSize = maximumCacheSize;
    }


    /**
     * Evaluates the timestamp/nonce combination for validity, storing and/or
     * clearing nonces as required.
     * <p>
     * The method is package private in order to be used in unit tests only.
     * </p>
     *
     * @param key       the oauth_consumer_key value for a given consumer request
     * @param timestamp the oauth_timestamp value for a given consumer request (in milliseconds).
     * @param nonce     the oauth_nonce value for a given consumer request.
     * @param now       current time in milliseconds
     * @return true if the timestamp/nonce are valid.
     */
    synchronized boolean verify(final String key, final String timestamp, final String nonce, final long now) {
        // convert timestamp to milliseconds since epoch to deal with uniformly
        final long stamp = timestampUnit.toMillis(longValue(timestamp));

        if (mapSize + 1 > maximumMapSize) {
            gc(now);
            if (mapSize + 1 > maximumMapSize) {
                // cannot keep another nonce (prevents exhausting memory)
                return false;
            }
        }

        // invalid timestamp supplied; automatically invalid
        if (stamp + maxAge < now || stamp - maxAge > now) {
            return false;
        }

        Map<String, Set<String>> keyToNonces = tsToKeyNoncePairs.get(stamp);
        if (keyToNonces == null) {
            keyToNonces = new HashMap<>();
            tsToKeyNoncePairs.put(stamp, keyToNonces);
        }

        Set<String> nonces = keyToNonces.get(key);
        if (nonces == null) {
            nonces = new HashSet<>();
            keyToNonces.put(key, nonces);
        }

        final boolean result = nonces.add(nonce);
        if (result) {
            mapSize++;
        }

        // perform garbage collection if counter is up to established number of passes
        if (++gcCounter >= gcPeriod) {
            gc(now);
        }

        // returns false if nonce already encountered for given timestamp
        return result;
    }

    /**
     * Evaluates the timestamp/nonce combination for validity, storing and/or
     * clearing nonces as required.
     *
     * @param key       the oauth_consumer_key value for a given consumer request
     * @param timestamp the oauth_timestamp value for a given consumer request (in milliseconds).
     * @param nonce     the oauth_nonce value for a given consumer request.
     * @return true if the timestamp/nonce are valid.
     */
    public synchronized boolean verify(final String key, final String timestamp, final String nonce) {
        return verify(key, timestamp, nonce, System.currentTimeMillis());
    }

    /**
     * Deletes all nonces older than maxAge.
     * This method is package private (instead of private) for testability purposes.
     *
     * @param now milliseconds since epoch representing "now"
     */
    void gc(final long now) {
        gcCounter = 0;
        final SortedMap<Long, Map<String, Set<String>>> headMap = tsToKeyNoncePairs.headMap(now - maxAge);
        for (final Map.Entry<Long, Map<String, Set<String>>> entry : headMap.entrySet()) {
            for (final Map.Entry<String, Set<String>> timeEntry : entry.getValue().entrySet()) {
                mapSize -= timeEntry.getValue().size();
            }
        }

        headMap.clear();
    }

    /**
     * Returns number of currently tracked timestamp-key-nonce tuples. The method should be used by tests only.
     * @return number of currently tracked timestamp-key-nonce tuples.
     */
    long checkAndGetSize() {
        long size = 0;
        for (final Map<String, Set<String>> keyToNonces : tsToKeyNoncePairs.values()) {
            size += keyToNonces.values().size();
        }
        assert mapSize == size;
        return mapSize;
    }

    private static long longValue(final String value) {
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException nfe) {
            return -1;
        }
    }
}

