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
package org.glassfish.jersey.server;

import org.glassfish.jersey.server.model.Resource;
import junit.framework.Assert;
import org.glassfish.jersey.filter.PreMatchRequestFilterModule;
import org.glassfish.jersey.filter.RequestFilterModule;
import org.glassfish.jersey.filter.ResponseFilterModule;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.ws.rs.BindingPriority;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.RequestFilter;
import javax.ws.rs.ext.ResponseFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for JAX-RS filters.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ApplicationFilterTest {

    @Test
    public void testSinglePreMatchRequestFilter() throws Exception {

        final AtomicInteger called = new AtomicInteger(0);

        List<PreMatchRequestFilter> preMatchRequestFilters = new ArrayList<PreMatchRequestFilter>();
        preMatchRequestFilters.add(new PreMatchRequestFilter() {

            @Override
            public void preMatchFilter(FilterContext context) throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .addModules(new PreMatchRequestFilterModule(preMatchRequestFilters));

        Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        resourceConfig.addResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(Requests.from("/test", "GET").build()).get().getStatus());

        // should be "1"; current value is "2" because of HK2 issue
        Assert.assertTrue(called.intValue() >= 1);
    }

    @Test
    public void testSingleRequestFilter() throws Exception {

        final AtomicInteger called = new AtomicInteger(0);

        List<RequestFilter> requestFilterList = new ArrayList<RequestFilter>();
        requestFilterList.add(new RequestFilter() {

            @Override
            public void preFilter(FilterContext context) throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .addModules(new RequestFilterModule(requestFilterList));

        Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        resourceConfig.addResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(Requests.from("/test", "GET").build()).get().getStatus());

        // should be "1"; current value is "2" because of HK2 issue
        Assert.assertTrue(called.intValue() >= 1);
    }

    @Test
    public void testSingleResponseFilter() throws Exception {

        final AtomicInteger called = new AtomicInteger(0);

        List<ResponseFilter> responseFilterList = new ArrayList<ResponseFilter>();
        responseFilterList.add(new ResponseFilter() {

            @Override
            public void postFilter(FilterContext context) throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .addModules(new ResponseFilterModule(responseFilterList));

        Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        resourceConfig.addResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(Requests.from("/test", "GET").build()).get().getStatus());

        // should be "1"; current value is "2" because of HK2 issue
        Assert.assertTrue(called.intValue() >= 1);
    }

    public abstract class CommonFilter implements RequestFilter {

        public boolean called = false;

        @Override
        public void preFilter(FilterContext context) throws IOException {
            verify();
            called = true;
        }

        protected abstract void verify();
    }

    @BindingPriority(1)
    public class Filter1 extends CommonFilter {

        private Filter10 filter10;
        private Filter100 filter100;

        public void setFilters(Filter10 filter10, Filter100 filter100) {
            this.filter10 = filter10;
            this.filter100 = filter100;
        }

        @Override
        protected void verify() {
            assertTrue(filter10.called == false);
            assertTrue(filter100.called == false);
        }
    }

    @BindingPriority(10)
    public class Filter10 extends CommonFilter {

        private Filter1 filter1;
        private Filter100 filter100;

        public void setFilters(Filter1 filter1, Filter100 filter100) {
            this.filter1 = filter1;
            this.filter100 = filter100;
        }

        @Override
        protected void verify() {
            assertTrue(filter1.called == true);
            assertTrue(filter100.called == false);
        }
    }

    @BindingPriority(100)
    public class Filter100 extends CommonFilter {

        private Filter1 filter1;
        private Filter10 filter10;

        public void setFilters(Filter1 filter1, Filter10 filter10) {
            this.filter1 = filter1;
            this.filter10 = filter10;
        }

        @Override
        protected void verify() {
            assertTrue(filter1.called == true);
            assertTrue(filter10.called == true);
        }
    }

    @Test
    public void testMultipleFiltersWithBindingPriority() throws Exception {

        final Filter1 filter1 = new Filter1();
        final Filter10 filter10 = new Filter10();
        final Filter100 filter100 = new Filter100();

        filter1.setFilters(filter10, filter100);
        filter10.setFilters(filter1, filter100);
        filter100.setFilters(filter1, filter10);

        List<RequestFilter> requestFilterList = new ArrayList<RequestFilter>();
        requestFilterList.add(filter100);
        requestFilterList.add(filter1);
        requestFilterList.add(filter10);

        final ResourceConfig resourceConfig = new ResourceConfig()
                .addModules(new RequestFilterModule(requestFilterList));

        Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        resourceConfig.addResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(Requests.from("/test", "GET").build()).get().getStatus());
    }

    public class ExceptionFilter implements RequestFilter {

        @Override
        public void preFilter(FilterContext context) throws IOException {
            throw new IOException("test");
        }
    }

    @Test
    public void testFilterExceptionHandling() throws Exception {

        List<RequestFilter> requestFilterList = new ArrayList<RequestFilter>();
        requestFilterList.add(new ExceptionFilter());

        final ResourceConfig resourceConfig = new ResourceConfig()
                .addModules(new RequestFilterModule(requestFilterList));

        Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        resourceConfig.addResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(500, application.apply(Requests.from("/test", "GET").build()).get().getStatus());
    }
}
