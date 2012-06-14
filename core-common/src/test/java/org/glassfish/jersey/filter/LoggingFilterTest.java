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
package org.glassfish.jersey.filter;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.glassfish.jersey._remove.Helper;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.glassfish.jersey._remove.FilterContext;
import org.glassfish.jersey._remove.RequestFilter;
import org.glassfish.jersey._remove.ResponseFilter;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.FilteringStage;
import org.glassfish.jersey.process.internal.ProcessingTestModule;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stage;
import org.glassfish.jersey.process.internal.Stages;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class LoggingFilterTest {

    private RequestInvoker<Request, Response> invoker;
    private RequestScope requestScope;
    private CustomLoggingFilter loggingFilter;

    private class CustomLoggingFilter extends LoggingFilter {

        public boolean preMatchRequestLogged = false;
        public boolean requestLogged = false;
        public boolean responseLogged = false;

        @Override
        public void preMatchFilter(FilterContext context) throws IOException {
            preMatchRequestLogged = true;
            super.preMatchFilter(context);
        }

        @Override
        public void preFilter(FilterContext context) throws IOException {
            requestLogged = true;
            super.preFilter(context);
        }

        @Override
        public void postFilter(FilterContext context) throws IOException {
            responseLogged = true;
            assertTrue(context.getRequest() != null);
            super.postFilter(context);
        }
    }

    @Before
    public void setUp() {
        this.loggingFilter = new CustomLoggingFilter();
        RequestFilterModule requestFilterModule = new RequestFilterModule(Lists.<RequestFilter>newArrayList(loggingFilter));
        ResponseFilterModule responseFilterModule = new ResponseFilterModule(Lists.<ResponseFilter>newArrayList(loggingFilter));

        final Services services = HK2.get().create(null,
                new ProcessingTestModule(),
                requestFilterModule,
                responseFilterModule);

        ProcessingTestModule.initProviders(services);

        FilteringStage filteringAcceptor = services.forContract(FilteringStage.class).get();
        final Stage<Request> rootStage = Stages.chain(filteringAcceptor).build(Stages.asStage(
                new Inflector<Request, Response>() {

                    @Override
                    public Response apply(Request data) {
                        return Responses.from(200, data).entity(Helper.unwrap(data).readEntity(String.class)).build();
                    }
                }));

        invoker = services.forContract(RequestInvoker.Builder.class).get().build(rootStage);
        requestScope = services.forContract(RequestScope.class).get();

    }

    @After
    public void tearDown() {
    }

    @Test
    public void testLoggingFilter() throws Exception {
        final CustomLoggingFilter logFilter = loggingFilter;
        requestScope.runInScope(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                ListenableFuture<Response> responseFuture = invoker.apply(
                        Requests.from("http://examples.jersey.java.net/", "GET").entity("TEST\nEntity").build());

                assertNotNull(responseFuture.get());

                assertFalse(logFilter.preMatchRequestLogged);
                assertTrue(logFilter.requestLogged);
                assertTrue(logFilter.responseLogged);
                return null;

            }
        });


    }
}
