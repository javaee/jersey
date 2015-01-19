/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.jersey2689;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * Tests for JERSEY-2689: Problem with validation errors on primitive type arrays.
 * <p/> There is a bug when a validation fails for a primitive data array. Eg a NotNull failed validation on a byte[] causes the code to throw a ClassCastException. The problem is caused by ValidationHelper.getViolationInvalidValue(Object invalidValue) It tries to cast any array to a Object[] A byte[] parameter would generate a ClassCastException.*
 * @author Oscar Guindzberg (oscar.guindzberg at gmail.com)
 */
public class Jersey2689ITCase extends JerseyTest {


    @Override
    protected ResourceConfig configure() {
        return new Jersey2689();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    /**
     * Checks if a thread gets stuck when an {@code IOException} is thrown from the {@code
     * MessageBodyWriter#writeTo}.
     */
    @Test
    public void testByteArray() throws Exception {
        // Executor.
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        final Future<Response> responseFuture = executor.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                SampleBean bean = new SampleBean();
                bean.setArray(new byte[]{});

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
                provider.setMapper(mapper);
                client().register(provider);

                return target().path("post-bean").request().post(Entity.entity(bean, MediaType.APPLICATION_JSON));
            }

        });

        executor.shutdown();
        final boolean inTime = executor.awaitTermination(5000, TimeUnit.MILLISECONDS);

        // Asserts.
        assertTrue(inTime);

        // Response.
        final Response response = responseFuture.get();

        //Make sure we get a 400 error and not a 500 error
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatusInfo().getStatusCode());

    }


}
