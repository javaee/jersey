/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

/**
 * Reproducer for JERSEY-2786.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ShutdownHookLeakTest {

    private static final int ITERATIONS = 4000;
    private static final int THRESHOLD = ITERATIONS * 2 / 3;

    @SuppressWarnings("unchecked")
    @Test
    public void testShutdownHookDoesNotLeak() throws Exception {
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target("http://example.com");

        final Collection shutdownHooks = getShutdownHooks(client);

        for (int i = 0; i < ITERATIONS; i++) {
            // Create/Initialize client runtime.
            target.property("Washington", "Irving")
                    .request()
                    .property("how", "now")
                    .buildGet()
                    .property("Irving", "Washington");
        }

        System.gc();

        int notEnqueued = 0;
        int notNull = 0;
        for (final Object o : shutdownHooks) {
            if (((WeakReference<JerseyClient.ShutdownHook>) o).get() != null) {
                notNull++;
            }
            if (!((WeakReference<JerseyClient.ShutdownHook>) o).isEnqueued()) {
                notEnqueued++;
            }
        }

        assertThat(
                "Non-null shutdown hook references count should not copy number of property invocation",
                // 66 % seems like a reasonable threshold for this test to keep it stable
                notNull, is(lessThan(THRESHOLD)));

        assertThat(
                "Shutdown hook references count not enqueued in the ReferenceQueue should not copy number of property invocation",
                // 66 % seems like a reasonable threshold for this test to keep it stable
                notEnqueued, is(lessThan(THRESHOLD)));
    }

    private Collection getShutdownHooks(final Client client) throws NoSuchFieldException, IllegalAccessException {
        final JerseyClient jerseyClient = (JerseyClient) client;
        final Field shutdownHooksField = JerseyClient.class.getDeclaredField("shutdownHooks");
        shutdownHooksField.setAccessible(true);
        return (Collection) shutdownHooksField.get(jerseyClient);
    }
}
