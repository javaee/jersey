/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.integration.tracing;

import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Configurable;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 */
public final class Utils {

    public static final String HEADER_TRACING_PREFIX = "X-Jersey-Tracing-";

    public static final String APPLICATION_X_JERSEY_TEST = "application/x-jersey-test";
    public static final String FORMAT_PREFIX = "-=#[";
    public static final String FORMAT_SUFFIX = "]#=-";
    public static final String HEADER_TEST_ACTION = "test-action";

    private Utils() {
    }

    public static void configure(ResourceConfig configurable) {
        configurable.packages(Utils.class.getPackage().getName());
//        OR:
//        configure((Configurable)configurable);
//        configurable.register(PreMatchingContainerRequestFilter23.class);
//        configurable.register(PreMatchingContainerRequestFilter42.class);
//        configurable.register(ContainerRequestFilter68.class);
//        configurable.register(ContainerRequestFilterNoPriority.class);
//        configurable.register(ContainerResponseFilter5001.class);
//        configurable.register(ContainerResponseFilterNoPriority.class);
//        configurable.register(TestExceptionMapper.class);
//        configurable.register(TestExtendedExceptionMapperGeneric.class);
//        configurable.register(TestExtendedExceptionMapperRuntime.class);
//        configurable.register(Resource.class);
//        configurable.register(SubResource.class);
        configurable.register(new LoggingFeature(Logger.getAnonymousLogger(), LoggingFeature.Verbosity.PAYLOAD_ANY));
    }

    public static void configure(ClientConfig configurable) {
        configure((Configurable) configurable);
    }

    public static void configure(Configurable configurable) {
        configurable.register(ReaderInterceptor14.class);
        configurable.register(ReaderInterceptor18.class);
        configurable.register(WriterInterceptor39.class);
        configurable.register(WriterInterceptor45.class);
        configurable.register(new MessageBodyReaderTestFormat(false));
        configurable.register(MessageBodyReaderGeneric.class);
        configurable.register(new MessageBodyWriterTestFormat(false));
        configurable.register(MessageBodyWriterGeneric.class);
    }

    public static void throwException(final ContainerRequestContext requestContext,
                                      final Object fromContext,
                                      final TestAction throwWebApplicationException,
                                      final TestAction throwProcessingException,
                                      final TestAction throwAnyException) {
        final Utils.TestAction testAction = Utils.getTestAction(requestContext);
        throwExceptionImpl(testAction, fromContext, throwWebApplicationException, throwProcessingException, throwAnyException);
    }

    public static void throwException(final String testActionName,
                                      final Object fromContext,
                                      final TestAction throwWebApplicationException,
                                      final TestAction throwProcessingException,
                                      final TestAction throwAnyException) {
        Utils.TestAction testAction;
        try {
            testAction = TestAction.valueOf(testActionName);
        } catch (IllegalArgumentException ex) {
            try {
                testAction = TestAction.valueOf(new StringBuffer(testActionName).reverse().toString());
            } catch (IllegalArgumentException ex2) {
                testAction = null;
            }
        }
        throwExceptionImpl(testAction, fromContext, throwWebApplicationException, throwProcessingException, throwAnyException);
    }

    private static void throwExceptionImpl(final Utils.TestAction testAction,
                                           final Object fromContext,
                                           final TestAction throwWebApplicationException,
                                           final TestAction throwProcessingException,
                                           final TestAction throwAnyException) {
        final String message = "Test Exception from " + fromContext.getClass().getName();
        if (testAction == null) {
            // do nothing
        } else if (testAction == throwWebApplicationException) {
            throw new WebApplicationException(message);
        } else if (testAction == throwProcessingException) {
            throw new ProcessingException(message);
        } else if (testAction == throwAnyException) {
            throw new RuntimeException(message);
        }
    }

    public static TestAction getTestAction(ContainerRequestContext requestContext) {
        String testActionHeader = requestContext.getHeaderString(HEADER_TEST_ACTION);
        TestAction testAction = null;
        if (testActionHeader != null) {
            testAction = TestAction.valueOf(testActionHeader);
        }
        return testAction;
    }

    public static enum TestAction {
        PRE_MATCHING_REQUEST_FILTER_THROW_WEB_APPLICATION,
        PRE_MATCHING_REQUEST_FILTER_THROW_PROCESSING,
        PRE_MATCHING_REQUEST_FILTER_THROW_ANY,
        MESSAGE_BODY_READER_THROW_WEB_APPLICATION,
        MESSAGE_BODY_READER_THROW_PROCESSING,
        MESSAGE_BODY_READER_THROW_ANY,
        MESSAGE_BODY_WRITER_THROW_WEB_APPLICATION,
        MESSAGE_BODY_WRITER_THROW_PROCESSING,
        MESSAGE_BODY_WRITER_THROW_ANY;
        //TODO add other *_THROW_* actions to throw exception from other stages
    }
}
