/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.helloworld;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.util.runner.ConcurrentParameterizedRunner;

import org.jboss.weld.environment.se.Weld;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test request scoped resource. Number of various requests will be made in parallel
 * against a single Grizzly instance. This is to ensure server side external request scope
 * binding does not mix different request data.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(ConcurrentParameterizedRunner.class)
public class RequestScopedResourceTest extends JerseyTest {

    // Total number of requests to make
    static final int REQUEST_COUNT = 1000;
    // basis for test data sequence
    static final AtomicInteger dataFeed = new AtomicInteger();

    // to help us randomily select resource method to test
    static final Random RANDOMIZER = new Random();

    // our Weld container instance
    static Weld weld;

    /**
     * Take test data sequence from here
     *
     * @return iterable test input data
     */
    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return new Iterable<Object[]>() {

            @Override
            public Iterator<Object[]> iterator() {
                return new Iterator<Object[]>() {

                    @Override
                    public boolean hasNext() {
                        return dataFeed.get() < REQUEST_COUNT;
                    }

                    @Override
                    public Object[] next() {
                        Object[] result = new Object[1];
                        int nextValue = dataFeed.getAndIncrement();
                        result[0] = String.format("%02d", nextValue);
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @BeforeClass
    public static void before() throws Exception {
        weld = new Weld();
        weld.initialize();
    }

    @AfterClass
    public static void after() throws Exception {
        weld.shutdown();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        System.out.printf("SYNC: %d, ASYNC: %d, STRAIGHT: %d%n",
                parameterizedCounter.intValue(), parameterizedAsyncCounter.intValue(), straightCounter.intValue());
    }

    @Override
    protected ResourceConfig configure() {
        //        enable(TestProperties.LOG_TRAFFIC);
        return App.createJaxRsApp();
    }

    // we want to keep some statistics
    final AtomicInteger parameterizedCounter = new AtomicInteger(0);
    final AtomicInteger parameterizedAsyncCounter = new AtomicInteger(0);
    final AtomicInteger straightCounter = new AtomicInteger(0);

    @Test
    public void testRequestScopedResource(final String param) {

        String path;
        String expected = param;

        // select one of the three resource methods available
        switch (RANDOMIZER.nextInt(3)) {
            case 0:
                path = "req/parameterized";
                parameterizedCounter.incrementAndGet();
                break;
            case 1:
                path = "req/parameterized-async";
                parameterizedAsyncCounter.incrementAndGet();
                break;
            default:
                path = "req/straight";
                expected = String.format("straight: %s", param);
                straightCounter.incrementAndGet();
                break;
        }

        final Response response = target().path(path).queryParam("q", param).request("text/plain").get();

        assertNotNull(String.format("Request failed for %s", path), response);
        assertEquals(200, response.getStatus());
        assertEquals(expected, response.readEntity(String.class));
    }
}
