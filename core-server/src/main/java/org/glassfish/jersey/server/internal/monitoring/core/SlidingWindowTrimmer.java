/*
 * Copyright (c) 2015-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2010-2013 Coda Hale and Yammer, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.jersey.server.internal.monitoring.core;

import java.util.concurrent.ConcurrentNavigableMap;

/**
 * The trimmer of an associated sliding window.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public interface SlidingWindowTrimmer<V> {

    /**
     * Trim the measurements provided as the map from the head up to the key (not inclusive).
     *
     * @param map The map to trim.
     * @param key The key to which trim the map.
     */
    void trim(ConcurrentNavigableMap<Long, V> map, final long key);

    /**
     * @param reservoir The reservoir that uses this trimmer.
     */
    void setTimeReservoir(TimeReservoir<V> reservoir);
}
