/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.AbstractModule;
import org.glassfish.jersey.internal.util.collection.Pair;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.internal.MappableException;
import static org.glassfish.jersey.process.internal.Stages.acceptingTree;
import static org.glassfish.jersey.process.internal.StringAppender.append;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.base.Optional;
import javax.ws.rs.ext.RuntimeDelegate;
import org.glassfish.jersey.internal.TestRuntimeDelegate;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class HierarchicalRequestProcessorTest {

    public static class Module extends AbstractModule {

        @Override
        protected void configure() {
            final TreeAcceptor inflectingStage = Stages.asTreeAcceptor(new Inflector<Request, Response>() {

                @Override
                public Response apply(Request data) {
                    try {
                        return Responses.from(200, data).entity(Integer.valueOf(data.readEntity(String.class))).build();
                    } catch (NumberFormatException ex) {
                        throw new MappableException(ex);
                    }
                }
            });

            /**
             * Tree:
             * 1 - 2 - 3
             *  \
             *   - 4 - 5 - Inflector
             *      \
             *       - 6
             */
            TreeAcceptor root =
                    acceptingTree(append("1")).child(acceptingTree(append("2")).child(acceptingTree(append("3")).build())
                                                             .build())
                                     .child(acceptingTree(append("4")).child(acceptingTree(append("5")).child(inflectingStage)
                                                                                     .build())
                                                             .child(acceptingTree(append("6")).build())
                                                             .build())
                                     .build();

            bind(TreeAcceptor.class).annotatedWith(Stage.Root.class).toInstance(root);

            bind(RequestProcessor.class).to(HierarchicalRequestProcessor.class);
        }
    }
    private RequestProcessor processor;
    private RequestScope requestScope;

    public HierarchicalRequestProcessorTest() {
        RuntimeDelegate.setInstance(new TestRuntimeDelegate());
    }

    @Before
    public void setUp() {
        Services services = HK2.get().create(null,
                new ProcessingTestModule(),
                new Module());

        processor = services.forContract(RequestProcessor.class).get();
        requestScope = services.forContract(RequestScope.class).get();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInflect() throws Exception {
        try {
            requestScope.enter();

            final Pair<Request, Optional<Inflector<Request, Response>>> continuation = processor.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("").build());
            assertEquals(145, continuation.right().get().apply(continuation.left()).readEntity(Integer.class).intValue());
        } finally {
            requestScope.exit();
        }
    }

    @Test
    public void testFailure() {
        try {
            requestScope.enter();

            final Pair<Request, Optional<Inflector<Request, Response>>> continuation = processor.apply(
                    Requests.from("http://examples.jersey.java.net/", "GET").entity("text").build());
            continuation.right().get().apply(continuation.left());
        } catch (MappableException ex) {
            // success
            return;
        } finally {
            requestScope.exit();
        }
        fail();
    }
}
