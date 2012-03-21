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

import static junit.framework.Assert.assertEquals;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import javax.annotation.Nullable;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;

import org.junit.Test;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ApplicationBuilderHttpMethodsTest {

    @Test
    public void testGet() throws Exception {
        final JerseyApplication.Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("test").method("GET").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        JerseyApplication application = appBuilder.build();

        checkReturnedStatus(Requests.from("", "test", "GET").build(), application);
    }

    @Test
    public void testHead() throws Exception {
        final JerseyApplication.Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("test").method("HEAD").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        JerseyApplication application = appBuilder.build();

        checkReturnedStatus(Requests.from("", "test", "HEAD").build(), application);
    }

    @Test
    public void testOptions() throws Exception {
        final JerseyApplication.Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("test").method("OPTIONS").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        JerseyApplication application = appBuilder.build();

        checkReturnedStatus(Requests.from("", "test", "OPTIONS").build(), application);
    }

    @Test
    public void testMultiple() throws Exception {
        final JerseyApplication.Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("test").method("GET", "OPTIONS", "HEAD").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(200).build();
            }
        });
        JerseyApplication application = appBuilder.build();

        checkReturnedStatus(Requests.from("", "test", "GET").build(), application);
        checkReturnedStatus(Requests.from("", "test", "HEAD").build(), application);
        checkReturnedStatus(Requests.from("", "test", "OPTIONS").build(), application);
    }

    @Test
    public void testTwoBindersSamePath() throws Exception {
        final JerseyApplication.Builder appBuilder = JerseyApplication.builder();
        appBuilder.bind("test1").method("GET").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(201).build();
            }
        });
        appBuilder.bind("test2").method("GET", "HEAD").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(202).build();
            }
        });
        appBuilder.bind("test1").method("OPTIONS", "HEAD").to(new Inflector<Request, Response>() {

            @Override
            public Response apply(@Nullable Request request) {
                return Responses.empty().status(203).build();
            }
        });
        JerseyApplication application = appBuilder.build();

        checkReturnedStatusEquals(201, Requests.from("", "test1", "GET").build(), application);
//        checkReturnedStatusEquals(203, Requests.from("", "test1", "HEAD").build(), application);
//        checkReturnedStatusEquals(203, Requests.from("", "test1", "OPTIONS").build(), application);

//        checkReturnedStatusEquals(202, Requests.from("", "test2", "GET").build(), application);
//        checkReturnedStatusEquals(202, Requests.from("", "test2", "HEAD").build(), application);
//        checkReturnedStatusEquals(202, Requests.from("", "test2", "OPTIONS").build(), application);
    }

    private void checkReturnedStatus(Request req, JerseyApplication app) throws Exception {
        checkReturnedStatusEquals(200, req, app);
    }

    private void checkReturnedStatusEquals(int expectedStatus, Request req, JerseyApplication app) throws Exception {
        final int responseStatus = app.apply(req).get().getStatus();
        assertEquals(responseStatus, expectedStatus);
    }
}
