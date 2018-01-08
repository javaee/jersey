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

package org.glassfish.jersey.tests.e2e.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import javax.inject.Inject;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.DisposableSupplier;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * E2E Request Scope Tests.
 *
 * @author Michal Gajdos
 */
@Ignore("Test Supplier Injection -> this test require dispose() method from Factory")
public class RequestScopeTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(RemoveResource.class)
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bindFactory(CloseMeFactory.class, RequestScoped.class).to(CloseMe.class).in(RequestScoped.class);
                    }
                });
    }

    public interface CloseMe {

        String eval();

        void close();
    }

    public static class CloseMeFactory implements DisposableSupplier<CloseMe> {

        private static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @Override
        public CloseMe get() {
            return new CloseMe() {
                @Override
                public String eval() {
                    return "foo";
                }

                @Override
                public void close() {
                    CLOSED_LATCH.countDown();
                }
            };
        }

        @Override
        public void dispose(final CloseMe instance) {
            instance.close();
        }
    }

    @Path("remove")
    public static class RemoveResource {

        private CloseMe closeMe;

        @Inject
        public RemoveResource(final CloseMe closeMe) {
            this.closeMe = closeMe;
        }

        @GET
        public String get() {
            return closeMe.eval();
        }
    }

    /**
     * Test that Factory.dispose method is called during release of Request Scope.
     */
    @Test
    public void testRemove() throws Exception {
        final Response response = target().path("remove").request().get();

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is("foo"));

        assertTrue(CloseMeFactory.CLOSED_LATCH.await(3, TimeUnit.SECONDS));
    }
}
