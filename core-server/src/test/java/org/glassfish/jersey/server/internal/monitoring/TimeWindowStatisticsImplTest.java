/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Miroslav Fuksa
 */
public class TimeWindowStatisticsImplTest {

    private static final double DELTA = 0.0001;

    @Test
    public void test() {
        final long now = System.currentTimeMillis();
        final TimeWindowStatisticsImpl.Builder builder = new TimeWindowStatisticsImpl.Builder(1000, TimeUnit.MILLISECONDS, now);
        builder.addRequest(now, 30);
        builder.addRequest(now + 300, 100);
        builder.addRequest(now + 600, 150);
        builder.addRequest(now + 800, 15);
        builder.addRequest(now + 999, 60);
        builder.addRequest(now + 1000, 95);

        check(builder, now + 1000, 6, 15, 150, 75, 6);
        builder.addRequest(now + 1001, 999);
        check(builder, now + 1001, 7, 15, 999, 207, 7);
    }

    @Test
    public void test10() {
        final long now = 0;
        final TimeWindowStatisticsImpl.Builder builder
                = new TimeWindowStatisticsImpl.Builder(10000, TimeUnit.MILLISECONDS, now);
        builder.addRequest(now, 30);
        builder.addRequest(now + 300, 100);
        builder.addRequest(now + 600, 150);
        builder.addRequest(now + 800, 15);
        builder.addRequest(now + 999, 60);
        builder.addRequest(now + 1000, 95);
        builder.addRequest(now + 8001, 600);

        // 30 + 100 + 150 + 15 + 60 + 95 = 450 duration in first second
        // 30 + 100 + 150 + 15 + 60 + 95 + 600 = 1050 total duration
        // 0.9 ratio
        // 450 * 0.9 = 405
        // 1050 - 405 = 645 total duration
        // avg = 645 / 2 = 322.5

        check(builder, now + 8001, 7, 15, 600, 150, 0.8748906);

        check(builder, now + 10900, 2, 15, 600, 322, 0.2);

    }

    @Test
    public void test3s() {
        final long now = 0;
        final TimeWindowStatisticsImpl.Builder builder
                = new TimeWindowStatisticsImpl.Builder(3000, TimeUnit.MILLISECONDS, now);
        builder.addRequest(now, 99);
        builder.addRequest(now + 300, 98);
        builder.addRequest(now + 600, 1);
        builder.addRequest(now + 1000, 96);
        builder.addRequest(now + 1500, 95);
        builder.addRequest(now + 2500, 3);
        // ... above should be ignored

        builder.addRequest(now + 3500, 90);
        builder.addRequest(now + 3900, 4);
        builder.addRequest(now + 3900, 80);
        builder.addRequest(now + 4200, 92);
        builder.addRequest(now + 4900, 15);
        builder.addRequest(now + 5300, 8);
        builder.addRequest(now + 5600, 50);

        check(builder, now + 6001, 7, 4, 92, 48, 2.333333);
    }

    @Test
    public void testLongPause() {
        final long now = 0;
        final TimeWindowStatisticsImpl.Builder builder = new TimeWindowStatisticsImpl.Builder(60, TimeUnit.SECONDS, now);
        builder.addRequest(now, 99);
        final long time = now + 1000 * 60 * 60 * 23;
        builder.addRequest(time, 95);
        builder.addRequest(time + 5, 5);
        check(builder, time + 20000, 2, 5, 95, 50, 0.03333);
    }

    private void check(final TimeWindowStatisticsImpl.Builder builder,
                       final long buildTime,
                       final int totalCount,
                       final int minimumExecTime,
                       final int maximumExecTime,
                       final long average,
                       final double requestsPerSecond) {
        final TimeWindowStatisticsImpl stat = builder.build(buildTime);

        assertEquals("Total count does not match!", totalCount, stat.getRequestCount());
        assertEquals("Min exec time does not match!", minimumExecTime, stat.getMinimumDuration());
        assertEquals("Max exec time does not match!", maximumExecTime, stat.getMaximumDuration());
        assertEquals("Average exec time does not match!", average, stat.getAverageDuration());
        assertEquals("Requests per seconds does not match!", requestsPerSecond, stat.getRequestsPerSecond(), DELTA);
    }

    @Test
    public void testGeneric() {
        final TimeWindowStatisticsImpl.Builder builder = new TimeWindowStatisticsImpl.Builder(10, TimeUnit.SECONDS, 0);
        for (int i = 0; i < 100; i++) {
            final int requestTime = i * 10000;
            builder.addRequest(requestTime + 1, i);
            for (int j = 11; j < 100; j++) {
                try {
                    final TimeWindowStatisticsImpl stat = builder.build(requestTime + j * 100);
                    assertEquals(1, stat.getRequestCount());
                    assertEquals(i, stat.getMinimumDuration());
                    assertEquals(i, stat.getMaximumDuration());
                } catch (final AssertionError e) {
                    System.out.println(i + " / " + j);
                    throw e;
                }
            }
        }
    }

    @Test
    public void testUnlimited() {
        final TimeWindowStatisticsImpl.Builder builder = new TimeWindowStatisticsImpl.Builder(0, TimeUnit.MILLISECONDS, 0);

        check(builder, 0, 0, 0, 0, 0, 0);
        check(builder, 10000, 0, 0, 0, 0, 0);

        builder.addRequest(0, 10);

        check(builder, 50, 1, 10, 10, 10, 20.0);

        builder.addRequest(100 + 300, 20);
        builder.addRequest(1000 + 600, 30);
        builder.addRequest(1587 + 800, 40);
        builder.addRequest(5544 + 999, 60);
        builder.addRequest(9998 + 1000, 50);

        check(builder, 10000, 6, 10, 60, 35, 0.6);
    }

}



