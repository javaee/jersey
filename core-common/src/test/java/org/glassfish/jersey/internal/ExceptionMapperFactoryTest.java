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

package org.glassfish.jersey.internal;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test of {@link ExceptionMapperFactory}.
 */
public class ExceptionMapperFactoryTest {

    private static class ExtendedExceptionMappers extends AbstractBinder {

        @Override
        protected void configure() {
            bind(IllegalArgumentExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
            bind(IllegalStateExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
        }

    }

    private static class AllMappers extends AbstractBinder {

        @Override
        protected void configure() {
            bind(IllegalArgumentExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
            bind(IllegalStateExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
            bind(RuntimeExceptionMapper.class).to(ExceptionMapper.class).in(Singleton.class);
        }

    }

    /**
     * Test spec:
     * <p/>
     * setup:<br/>
     * - have two extended exception mappers, order matters<br/>
     * - both using the same generic type (RuntimeException)<br/>
     * - first mapper return isMappable true only to IllegalArgumentException<br/>
     * - second mapper return isMappable true only to IllegalStateException<br/>
     * <br/>
     * when:<br/>
     * - {@link ExceptionMapperFactory#findMapping(Throwable)} with IllegalArgumentException instance<br/>
     * <br/>
     * then:<br/>
     * - exception mapper factory returns IllegalArgumentExceptionMapper<br/>
     * <p/>
     * why:<br/>
     * - IllegalArgumentException has the same distance (1) for both exception mappers generic type (RuntimeException),
     * but IllegalArgumentException's isMappable return true, so it is the winner
     *
     * @throws Exception unexpected - if anything goes wrong, the test fails
     */
    @Test
    public void testFindMappingExtendedExceptions() throws Exception {
        final ServiceLocator serviceLocator = Injections.createLocator(new ExtendedExceptionMappers());
        final ExceptionMapperFactory mapperFactory = new ExceptionMapperFactory(serviceLocator);

        final ExceptionMapper mapper = mapperFactory.findMapping(new IllegalArgumentException());

        Assert.assertTrue("IllegalArgumentExceptionMapper should be returned",
                mapper instanceof IllegalArgumentExceptionMapper);
    }

    /**
     * Test spec:
     * <p/>
     * setup:<br/>
     * - have 3 exception mappers, order matters<br/>
     * - first is *not* extended mapper typed to RuntimeException
     * - second and third are extended mappers type to RuntimeException
     * <br/>
     * when:<br/>
     * - {@link ExceptionMapperFactory#findMapping(Throwable)} invoked with RuntimeException instance<br/>
     * then: <br/>
     * - exception mapper factory returns RuntimeExceptionMapper<br/>
     * <p/>
     * why:<br/>
     * - RuntimeException mapper has distance 0 for RuntimeException, it is not extended mapper, so it will be chosen
     * immediately, cause there is no better option possible
     *
     * @throws Exception unexpected - if anything goes wrong, the test fails
     */
    @Test
    public void testFindMapping() throws Exception {
        final ServiceLocator serviceLocator = Injections.createLocator(new AllMappers());
        final ExceptionMapperFactory mapperFactory = new ExceptionMapperFactory(serviceLocator);

        final ExceptionMapper<RuntimeException> mapper = mapperFactory.findMapping(new RuntimeException());

        Assert.assertTrue("RuntimeExceptionMapper should be returned", mapper instanceof RuntimeExceptionMapper);
    }

    /**
     * Test spec: <br/>
     * <p/>
     * setup:<br/>
     * - have 2 extended mappers, order matters<br/>
     * - first mapper return isMappable true only to IllegalArgumentException<br/>
     * - second mapper return isMappable true only to IllegalStateException<br/>
     * <br/>
     * when:<br/>
     * - {@link ExceptionMapperFactory#find(Class)} invoked with IllegalArgumentException.class<br/>
     * then:<br/>
     * - exception mapper factory returns IllegalArgumentExceptionMapper<br/>
     * <p/>
     * why:<br/>
     * - both exception mappers have distance 1 to IllegalArgumentException, we don't have instance of the
     * IllegalArgumentException, so the isMappable check is not used and both are accepted, the later accepted is
     * the winner
     *
     * @throws Exception unexpected - if anything goes wrong, the test fails
     */
    @Test
    public void testFindExtendedExceptions() throws Exception {
        final ServiceLocator serviceLocator = Injections.createLocator(new ExtendedExceptionMappers());
        final ExceptionMapperFactory mapperFactory = new ExceptionMapperFactory(serviceLocator);

        final ExceptionMapper mapper = mapperFactory.find(IllegalArgumentException.class);

        Assert.assertTrue("IllegalStateExceptionMapper should be returned",
                mapper instanceof IllegalStateExceptionMapper);
    }

    /**
     * Extended Exception Mapper which has RuntimeException as generic type and isMappable returns true if the
     * exception is instance of IllegalArgumentException.
     */
    private static class IllegalArgumentExceptionMapper implements ExtendedExceptionMapper<RuntimeException> {

        @Override
        public boolean isMappable(final RuntimeException exception) {
            return exception instanceof IllegalArgumentException;
        }

        @Override
        public Response toResponse(final RuntimeException exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        }

    }

    /**
     * Extended Exception Mapper which has RuntimeException as generic type and isMappable returns true if the
     * exception is instance of IllegalStateException.
     */
    private static class IllegalStateExceptionMapper implements ExtendedExceptionMapper<RuntimeException> {

        @Override
        public boolean isMappable(final RuntimeException exception) {
            return exception instanceof IllegalStateException;
        }

        @Override
        public Response toResponse(final RuntimeException exception) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .build();
        }

    }

    /**
     * Exception Mapper which has RuntimeException as generic type.
     */
    private static class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

        @Override
        public Response toResponse(final RuntimeException exception) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }

    }
}
