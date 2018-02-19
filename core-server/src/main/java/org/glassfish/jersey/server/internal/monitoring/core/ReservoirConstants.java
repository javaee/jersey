/*
 * Copyright 2015-2018 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring.core;

/**
 * The constants that determine the behaviour of sliding windows and their trimmers.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public final class ReservoirConstants {

    /**
     * Allow for 2^that many duplicate ticks before throwing away measurements.
     */
    public static final int COLLISION_BUFFER_POWER = 8;

    /**
     * The size of the collision buffer derived from the collision buffer power.
     */
    public static final int COLLISION_BUFFER = 1 << COLLISION_BUFFER_POWER; // 256

    /**
     * Only trim on updating once every N.
     */
    public static final int TRIM_THRESHOLD = 256;

    private ReservoirConstants() {
        throw new AssertionError("Instantiation not allowed.");
    }
}
