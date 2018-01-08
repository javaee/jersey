/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import javax.annotation.Priority;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class InterceptorNameAndDynamicBindingTest extends JerseyTest {

    static final String ENTITY = "ENTITY";

    @Override
    protected void configureClient(ClientConfig config) {
        super.configureClient(config);
    }

    abstract static class PrefixAddingReaderInterceptor implements ReaderInterceptor {

        public PrefixAddingReaderInterceptor() {
        }

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            context.setInputStream(
                    new SequenceInputStream(new ByteArrayInputStream(getPrefix().getBytes()), context.getInputStream()));
            return context.proceed();
        }

        abstract String getPrefix();
    }

    abstract static class PrefixAddingWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getOutputStream().write(getPrefix().getBytes());
            context.proceed();
        }

        abstract String getPrefix();
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    static @interface NameBoundReader {
    }

    @NameBoundReader
    @Priority(40)
    static class NameBoundReaderInterceptor extends PrefixAddingReaderInterceptor {

        @Override
        String getPrefix() {
            return "nameBoundReader";
        }
    }

    @Priority(60)
    static class DynamicallyBoundReaderInterceptor extends PrefixAddingReaderInterceptor {

        @Override
        String getPrefix() {
            return "dynamicallyBoundReader";
        }
    }

    @NameBinding
    @Priority(40)
    @Retention(RetentionPolicy.RUNTIME)
    static @interface NameBoundWriter {
    }

    @NameBoundWriter
    public static class NameBoundWriterInterceptor extends PrefixAddingWriterInterceptor {

        @Override
        String getPrefix() {
            return "nameBoundWriter";
        }
    }

    @Priority(20)
    public static class DynamicallyBoundWriterInterceptor extends PrefixAddingWriterInterceptor {

        @Override
        String getPrefix() {
            return "dynamicallyBoundWriter";
        }
    }

    @Path("method")
    public static class MethodBindingResource {

        @Path("dynamicallyBoundWriter")
        @GET
        public String getDynamicallyBoundWriter() {
            return ENTITY;
        }

        @Path("nameBoundWriter")
        @GET
        @NameBoundWriter
        public String getNameBoundWriter() {
            return ENTITY;
        }

        @Path("dynamicallyBoundReader")
        @POST
        public String postDynamicallyBoundReader(String input) {
            return input;
        }

        @Path("nameBoundReader")
        @POST
        @NameBoundReader
        public String postNameBoundReader(String input) {
            return input;
        }
    }

    @Path("class")
    @NameBoundWriter
    public static class ClassBindingResource {

        @Path("nameBoundWriter")
        @GET
        public String getNameBoundWriter() {
            return ENTITY;
        }

        @Path("nameBoundReader")
        @POST
        public String postNameBoundReader(String input) {
            return input;
        }
    }

    @Path("mixed")
    @NameBoundWriter
    public static class MixedBindingResource {

        @Path("nameBoundWriterDynamicReader")
        @POST
        public String postNameBoundWrDynamicallyBoundReader(String input) {
            return input;
        }

        @Path("nameBoundWriterDynamicWriterNameBoundReader")
        @POST
        @NameBoundReader
        public String postNameBoundReWrDynamicallyBoundWriter(String input) {
            return input;
        }
    }

    static final Pattern ReaderMETHOD = Pattern.compile(".*Dynamically.*Reader");
    static final Pattern WriterMETHOD = Pattern.compile(".*Dynamically.*Writer");

    @Override
    protected Application configure() {
        return new ResourceConfig(MethodBindingResource.class, ClassBindingResource.class,
                MixedBindingResource.class, NameBoundReaderInterceptor.class, NameBoundWriterInterceptor.class).registerInstances(
                new DynamicFeature() {

                    @Override
                    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
                        if (ReaderMETHOD.matcher(resourceInfo.getResourceMethod().getName()).matches()) {
                            context.register(DynamicallyBoundReaderInterceptor.class);
                        }
                    }
                },
                new DynamicFeature() {

                    @Override
                    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
                        if (WriterMETHOD.matcher(resourceInfo.getResourceMethod().getName()).matches()) {
                            context.register(DynamicallyBoundWriterInterceptor.class);
                        }
                    }
                }
        );
    }

    @Test
    public void testNameBoundReaderOnMethod() {
        _testReader("method", "nameBoundReader");
    }

    @Test
    public void testNameBoundWriterOnMethod() {
        _testWriter("method", "nameBoundWriter");
    }

    @Test
    public void testNameBoundReaderOnClass() {
        _testReader("class", "nameBoundReader", "nameBoundWriterENTITY");
    }

    @Test
    public void testNameBoundWriterOnClass() {
        _testWriter("class", "nameBoundWriter");
    }

    @Test
    public void testDynamicallyBoundReaderOnMethod() {
        _testReader("method", "dynamicallyBoundReader");
    }

    @Test
    public void testDynamicallyBoundWriterOnMethod() {
        _testWriter("method", "dynamicallyBoundWriter");
    }

    @Test
    public void testDynamicReaderOnMethodNamedWriterOnClass() {
        _testReader("mixed", "nameBoundWriterDynamicReader", "nameBoundWriterdynamicallyBoundReaderENTITY");
    }

    @Test
    public void testNameBoundWriterDynamicWriterNameBoundReader() {
        _testReader("mixed", "nameBoundWriterDynamicWriterNameBoundReader",
                "dynamicallyBoundWriternameBoundWriternameBoundReaderENTITY");
    }

    private void _testReader(String root, String id) {
        _testReader(root, id, id + ENTITY);
    }

    private void _testReader(String root, String id, String expected) {
        Response r = target(root + "/" + id).request().post(Entity.entity(ENTITY, MediaType.TEXT_PLAIN));
        assertEquals(200, r.getStatus());
        assertEquals(expected, r.readEntity(String.class));
    }

    private void _testWriter(String root, String id) {
        _testWriter(root, id, id + ENTITY);
    }

    private void _testWriter(String root, String id, String expected) {
        Response r = target(root + "/" + id).request().get();
        assertEquals(200, r.getStatus());
        assertEquals(expected, r.readEntity(String.class));
    }
}
